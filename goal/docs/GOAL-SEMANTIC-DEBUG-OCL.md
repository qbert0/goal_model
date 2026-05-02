# GOAL Semantic, Debug va OCL Spec

## 1. Pham vi

Tai lieu nay gom 3 phan:

- mo ta package `parser/semantic`;
- mo ta package `parser/debug`;
- dac ta phan OCL hien duoc ngon ngu GOAL ho tro.

No duoc viet theo code hien tai trong package `org.vnu.sme.goal.parser`.

## 2. Package `parser/semantic`

### 2.1 Vai tro tong quan

Tang `semantic` hien tai chua sua vao `mm`. No lam viec tren `GoalModelCS` va symbol table trung gian.

Muc tieu cua tang nay:

- thu thap declaration;
- resolve reference;
- tinh toan thong tin suy dien nhu `leaf`;
- kiem tra cac rang buoc nguyen nghia truoc khi AST duoc doi thanh runtime model.

### 2.2 Luong xu ly

```text
GoalModelCS
  -> GoalSymbolTableBuilder.runDeclarationPass()
  -> GoalSymbolTableBuilder.runResolutionPass()
  -> GoalSemanticAnalyzer.traverse*()
  -> List<SemanticIssue>
```

### 2.3 Cac file va ham chinh

#### `GoalSemanticPipelineSkeleton.java`

Day la entry-point muc cao cho semantic pass.

Ham chinh:

- `run(GoalModelCS ast, MModel model, PrintWriter err)`
  - tao symbol table goc;
  - chay declaration pass;
  - chay resolution pass;
  - chay semantic validation;
  - in issue neu co.
- `createEmptySymbolTable(...)`
  - tao root `GoalSymbolTable`.
- `declarationPass(...)`
  - goi builder de dang ky actor, element, dependency.
- `resolutionPass(...)`
  - goi builder de resolve relation target va dependency ref.
- `computeDerivedFlags(...)`
  - ghi nhan diem mo rong, hien logic leaf duoc tinh ngay trong builder.
- `validateSemanticRules(...)`
  - tron issue cua builder va analyzer.
- `printSemanticIssues(...)`
  - in issue theo format `[SEM][code] line:column message`.

#### `GoalSemanticAnalyzer.java`

Day la noi giu cac luat semantic cap AST/symbol table.

Ham chinh:

- `analyze(...)`
  - mode standalone, tu build symbol table va chay tat ca check.
- `traverseActorReferenceTree(...)`
  - check S1, S4 cho quan he actor-level.
- `traverseElementRelationTree(...)`
  - check S2, S6, S7, S8;
  - dong thoi thu thap refinement graph va incoming refinement metadata.
- `traverseRefinementTargetMap(...)`
  - check S9, S10 sau khi co refinement graph.
- `traverseDependencyTree(...)`
  - check S3 cho dependency leaf constraint.
- `validateActorRefConstraint(...)`
  - kiem tra tung tham chieu actor.
- `validateMixedRefinementType(...)`
  - cam tron `&>` va `|>` vao cung mot target.
- `validateCircularRefinement(...)`
  - DFS de tim refinement cycle.
- `createOperatorMatrix()`
  - dinh nghia ma tran operator hop le theo `ElementKind`.

`RelationTraversalContext` giu 3 tap du lieu trung gian:

- `issues`;
- `refinementGraph`;
- `incomingRefinementsByTarget`.

#### `ActorKind.java`

Enum phan biet:

- `ACTOR`
- `AGENT`
- `ROLE`

#### `ElementKind.java`

Enum phan biet:

- `GOAL`
- `TASK`
- `QUALITY`
- `RESOURCE`

## 3. Package `parser/semantic/symbols`

### 3.1 Vai tro

Package nay tao cau noi giua AST va semantic checks.

No khong phai runtime model va khong phai parse tree. No la symbol graph:

- giam phu thuoc vao class AST cu the;
- giu raw ref va resolved ref can thiet cho validation;
- cho phep semantic rule chay nhanh tren ten va loai phan tu.

### 3.2 Cac file va ham chinh

#### `GoalSymbolTableBuilder.java`

Builder hai pass.

Ham chinh:

- `build(...)`
  - goi ca declaration pass va resolution pass.
- `runDeclarationPass(...)`
  - dang ky actor va dependency.
- `runResolutionPass(...)`
  - resolve relation target;
  - resolve dependency ref;
  - recompute leaf flags.
- `registerActor(...)`
  - map `ActorDeclCS` thanh `ActorSymbol`.
- `registerElement(...)`
  - map `IntentionalElementCS` thanh `ElementSymbol`;
  - doi relation AST thanh `RelationEntry`.
- `registerDependency(...)`
  - map `DependencyCS` thanh `DependencySymbol`.
- `resolveRelationTargets(...)`
  - resolve `OutgoingLink.target`.
- `resolveDependencyReferences(...)`
  - resolve depender/dependee cua dependency.
- `recomputeLeafFlags(...)`
  - mac dinh moi element la leaf;
  - neu co refinement incoming thi danh dau khong phai leaf.
- `reportDuplicateDeclaration(...)`
  - tao `SemanticIssue S5`.
- `reportUndeclaredReference(...)`
  - tao `SemanticIssue S1`.

#### `GoalSymbolTable.java`

Root symbol scope cua model.

Ham chinh:

- `getActorsByName()`
- `getElementsByQualifiedName()`
- `getDependenciesByName()`
- `resolveActor(...)`
- `resolveElement(...)`

#### `ActorSymbol.java`

Giu:

- `name`
- `kind`
- `declarationToken`
- `elementTable`

#### `ElementSymbol.java`

Giu:

- `name`
- `kind`
- `ownerActor`
- `declarationToken`
- `relations`
- `leaf`

Ham quan trong:

- `getQualifiedName()`
  - tra ve `ActorName.ElementName`.

#### `DependencySymbol.java`

Giu:

- `name`
- `declarationToken`
- `dependerRawRef`
- `dependeeRawRef`
- `depender`
- `dependee`
- `dependum`

#### `RelationEntry.java`

Symbol cho mot outgoing relation AST.

Giu:

- `operator`
- `targetRef`
- `resolvedTarget`

#### `SemanticIssue.java`

Record nho de bao loi semantic:

- `code`
- `message`
- `line`
- `column`

## 4. Semantic rules hien tai

Tang semantic hien tai da co cac rule sau:

- `S1`: undeclared actor/element reference.
- `S2`: invalid operator matrix giua source-kind, operator, target-kind.
- `S3`: dependency depender/dependee phai la leaf.
- `S4`: invalid actor relationship (`is-a`, `participates-in`).
- `S5`: duplicate declaration.
- `S6`: self reference relation.
- `S7`: `QUALIFY` bat buoc source la `QUALITY`.
- `S8`: `NEEDED_BY` bat buoc source la `RESOURCE`.
- `S9`: circular refinement.
- `S10`: mixed refinement type tren cung target.

## 5. Package `parser/debug`

### 5.1 Vai tro

Package nay dung de in cau truc trung gian khi can debug parser va semantic pipeline.

No khong thay doi model. No chi serialise AST hoac symbol table thanh text.

### 5.2 Cac file va ham chinh

#### `GoalAstPrinter.java`

Ham chinh:

- `dump(GoalModelCS ast)`
  - in root model;
  - in actor declaration;
  - in intentional element;
  - in outgoing relation;
  - in dependency declaration.
- `dumpActor(...)`
  - in tung actor va cac quan he actor-level.
- `dumpDependency(...)`
  - in dependency raw structure.
- `actorKind(...)`
  - phan loai actor AST.
- `elementKind(...)`
  - phan loai intentional element AST.
- `pos(...)`
  - format line:column.

#### `GoalSymbolTablePrinter.java`

Ham chinh:

- `dump(String title, GoalSymbolTable table)`
  - in actor table;
  - in element table;
  - in dependency table;
  - in relation target raw/resolved;
  - in co `leaf`.
- `pos(...)`
  - format line:column.

## 6. Dac ta OCL hien duoc GOAL ho tro

### 6.1 Vi tri xuat hien trong ngon ngu

Hien tai plugin ho tro OCL trong 5 loai clause:

- `goal achieve <expression>`
- `goal maintain <expression>`
- `goal avoid <expression>`
- `task pre <expression>`
- `task post <expression>`

Trong AST:

- `GoalCS.goalType + GoalCS.oclExpression`
- `TaskCS.preExpression`
- `TaskCS.postExpression`

Trong runtime model:

- `GoalClause` (`Achieve`, `Maintain`, `Avoid`)
- `Pre`
- `Post`

### 6.2 Tang bieu dien

Parser khong giu OCL nhu chuoi thuan.

No doi thanh `mm.ocl.Expression`, bao gom:

```text
Expression
|- BinaryExp
|- UnaryExp
|- VariableExp
|  `- SelfExp
|- AtPreExp
|- IteratorExp
|- FeatureCallExp
|  |- PropertyCallExp
|  `- OperationCallExp
|- LiteralExp
|  |- StringLiteralExp
|  |- IntegerLiteralExp
|  |- RealLiteralExp
|  |- BooleanLiteralExp
|  |- NullLiteralExp
|  `- EnumLiteralExp
`- OpaqueExpression
```

### 6.3 Nhom cu phap duoc `OclExpressionBuilder` ho tro

#### Toan tu logic

- `implies`
- `or`
- `and`
- `not`

#### Toan tu so sanh

- `=`
- `<>` trong OCL-level khong thay trong builder hien tai; phan equality dang map `=` va truong hop con lai thanh `NOT_EQUALS`
- `<`
- `<=`
- `>`
- `>=`

#### Toan tu so hoc

- `+`
- `-`
- `*`
- `/`
- unary `-`

#### Literal

- `String`
- `Integer`
- `Real`
- `Boolean`
- `null`
- enum literal

#### Bien va duong dan

- `self`
- variable name
- property call
- operation call
- `@pre`

#### Iterator

- `exists`
- `forAll`
- `collect`
- `select`
- `reject`
- `any`
- `one`
- `isUnique`
- `sortedBy`
- `closure`

### 6.4 Dac ta sematic muc ngon ngu

OCL trong GOAL duoc hieu theo 2 tang:

1. Tang parse va AST:
   - cu phap hop le;
   - co cay `Expression`.

2. Tang doi chieu voi USE model:
   - `OclUseValidator` trong `view` so khop text/cau truc OCL voi `MModel` dang mo.

Nghia la:

- `semantic` package hien tai chua suy luan kieu OCL;
- viec OCL "co hop le voi model USE hay khong" hien duoc de sang `view/OclUseValidator`.

### 6.5 Cach dac ta goal bang OCL trong plugin

De "dac ta duoc cac goal", hinh dang hien tai la:

- goal de thuoc tinh dat duoc:
  - `achieve <predicate>`
- goal de rang buoc luon dung:
  - `maintain <predicate>`
- goal de rang buoc phai tranh:
  - `avoid <predicate>`

Vi du muc ngon ngu:

```text
goal PaidOrdersEventuallyDelivered
  achieve self.orders->forAll(o | o.paid implies o.delivered)

goal NoUnpaidDeliveredOrder
  avoid self.orders->exists(o | (not o.paid) and o.delivered)

task AssignDriver
  pre self.availableDrivers->exists(d | d.active)
  post self.assignedDriver <> null
```

## 7. Ket luan

Trang thai hien tai co the tom tat nhu sau:

- `semantic` da kiem tra duoc phan lon rang buoc co ban cua AST;
- `debug` da du de in AST va symbol table khi mo rong grammar;
- OCL da duoc parse thanh expression model, nhung semantic type-check OCL van chua nam trong `parser/semantic`;
- neu muon tang do chat cho OCL, buoc tiep theo hop ly la dua mot OCL semantic pass vao truoc `GoalModelFactory`, thay vi chi validate luc mo view.
