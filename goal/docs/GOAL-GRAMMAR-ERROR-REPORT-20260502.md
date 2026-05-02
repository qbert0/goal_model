## GOAL grammar error report

File checked: `goal/src/main/resources/grammars/GOAL.g4`

Backup created:
- `goal/src/main/resources/grammars/GOAL.g4.bak-20260502`

Problems found in the old grammar:

1. `->` was declared twice:
- `CONTRIB_HURT : '->'`
- `ARROW : '->'`
- In ANTLR lexer mode this makes `ARROW` unreachable, so collection navigation like `self.orders->forAll(...)` is not tokenized the way the parser comments expect.

2. `<>` was declared twice:
- `NEEDED_BY : '<>'`
- `NEQ : '<>'`
- This makes `NEQ` unreachable, so OCL inequality and GOAL relation syntax were colliding.

3. `achieve for unique (...) in <expr> : <expr>` produced two anonymous `expression` children:
- The Java visitors were calling `ctx.expression()` as if there were only one expression.
- Generated code returned a list, which caused compilation failure.

4. The Java OCL builder no longer matched the grammar shape:
- `andExpr` in grammar used `notExpr`, but builder read `equalityExpr`.
- `NOT` belonged to `notExpr`, but builder expected `NOT` inside `unaryExpr`.
- `iteratorCall` used `iteratorOp`, but builder expected `iterator.IDENT()`.
- `aggregateCall` existed in grammar but was not built correctly in Java.

5. The validator mostly recognized only dotted paths such as `self.orders` or `o.status`:
- It did not properly support the implicit-root form `orders->exists(...)`.
- For the intended USE environment, `self` should map to the loaded `SystemState`, and `orders` should be accepted as an implicit root property equivalent to `self.orders` when such a property exists in the USE model.

What was corrected:

- Removed the duplicate lexer aliases for `->` and `<>`.
- Reworked expression layering so logic operators and arithmetic operators are separated cleanly.
- Labeled `achieve` expressions as `source` and `body`.
- Updated Java builder/visitors to match the new parse tree.
- Updated the validator to understand `self` and implicit root properties such as `orders`.

Target behavior after the fix:

- Basic expressions:
  - `a = b`
  - `x <> null`
  - `a + b * c`
  - `p implies q`

- Lambda / iterator expressions:
  - `self.orders->forAll(o | o.paymentConfirmed = true)`
  - `orders->exists(o | o.status = OrderStatus::DELIVERED)`
  - `self.doors->collect(d | d.openingTime)->max()`
