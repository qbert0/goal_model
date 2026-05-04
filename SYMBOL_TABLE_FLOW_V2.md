# Symbol Table & Semantic Flow V2

Tài liệu này mô tả **call tree hiện trạng** của semantic pipeline trong codebase hiện tại, gồm:

- **V1 checks**: `S1..S10` (core GOAL, không phụ thuộc OCL type semantics)
- **V2 checks**: `E1..E7` (mở rộng OCL)

Entry points chính:

- `goal/src/main/java/org/vnu/sme/goal/parser/semantic/pipeline/GoalSemanticPipelineSkeleton.java`
- `goal/src/main/java/org/vnu/sme/goal/parser/semantic/symbols/GoalSymbolTableBuilder.java`
- `goal/src/main/java/org/vnu/sme/goal/parser/semantic/GoalSemanticAnalyzer.java`
- `goal/src/main/java/org/vnu/sme/goal/parser/semantic/OclSemanticAnalyzer.java`

---

## 1) Call tree tổng quát (top-down)

```text
GOALCompiler.compileSpecification(...)
└─ GoalSemanticPipelineSkeleton.run(ast, useModel, err)
   ├─ builder = new GoalSymbolTableBuilder()
   ├─ table = createEmptySymbolTable(ast)
   │  └─ new GoalSymbolTable(modelName)
   ├─ declarationPass(ast, table, err)                     [Pass 1]
   │  └─ builder.runDeclarationPass(ast, table)
   │     ├─ registerActor(...) for each ActorDeclCS
   │     │  ├─ create ActorSymbol
   │     │  └─ registerElement(...) for each IntentionalElementCS
   │     │     ├─ create ElementSymbol
   │     │     ├─ collect OutgoingLink -> RelationEntry(raw target)
   │     │     └─ attachOclContracts(...)                 [V2 data chuẩn bị]
   │     │        ├─ GoalCS -> GoalContract
   │     │        └─ TaskCS -> TaskContract
   │     └─ registerDependency(...) for each DependencyCS
   │        └─ create DependencySymbol(raw refs + dependum symbol)
   ├─ resolutionPass(ast, table, err)                      [Pass 2]
   │  └─ builder.runResolutionPass(ast, table)
   │     ├─ resolveRelationTargets(table)                 [S1 nếu fail]
   │     ├─ resolveDependencyReferences(table)            [S1 nếu fail]
   │     └─ recomputeLeafFlags(table)
   │        └─ refinement target (&>, |>) => leaf=false
   ├─ computeDerivedFlags(table, err)
   │  └─ hiện chỉ log (leaf đã xử lý ở resolutionPass)
   ├─ validateSemanticRules(ast, table, useModel, err)
   │  ├─ merged = builder.getIssues()                     [S1, S5]
   │  ├─ run V1 analyzer (GoalSemanticAnalyzer)
   │  │  ├─ traverseActorReferenceTree(ast, table)        [S1, S4]
   │  │  ├─ relationCtx = traverseElementRelationTree(...) [S2, S6, S7, S8]
   │  │  ├─ traverseRefinementTargetMap(relationCtx)      [S9, S10]
   │  │  └─ traverseDependencyTree(table)                 [S3]
   │  └─ run V2 analyzer (OclSemanticAnalyzer)
   │     ├─ traverseGoalContractCardinality(table)        [E5]
   │     ├─ traverseAchieveContracts(table)               [E1, E2]
   │     ├─ traverseContractBodies(table)                 [E4]
   │     └─ traverseOclExpressions(table)                 [E3, E6, E7]
   └─ printSemanticIssues(merged, err)
```

---

## 2) Trạng thái dữ liệu qua từng giai đoạn

### 2.1 Sau `createEmptySymbolTable`

- `actorsByName = {}`
- `elementsByQualifiedName = {}`
- `dependenciesByName = {}`

### 2.2 Sau Pass 1 (`runDeclarationPass`)

- Actor/Element/Dependency đã được khai báo vào table.
- `RelationEntry` mới ở dạng raw token, chưa resolve target.
- `ElementSymbol` đã có thể mang contract:
  - `goalContract` (nếu element là goal có clause)
  - `taskContract` (nếu element là task có pre/post)
- `builder.issues` đã có thể chứa `S5` (duplicate).

### 2.3 Sau Pass 2 (`runResolutionPass`)

- `RelationEntry.resolvedTarget` được gán nếu resolve thành công.
- `DependencySymbol.depender/dependee` được gán nếu resolve thành công.
- `leaf` được recompute từ refinement graph.
- `builder.issues` có thể thêm `S1` (unresolved refs).

### 2.4 Sau validation

- `merged` chứa toàn bộ issue theo thứ tự:
  - Builder issues (`S1`,`S5`)
  - V1 semantic issues (`S1..S4`,`S6..S10`)
  - V2 semantic issues (`E1..E7`)

### 2.5 Snapshot trạng thái Symbol Table (quick view)


| Trường / Node                               | Sau `createEmptySymbolTable` | Sau Pass 1               | Sau Pass 2                 | Sau validation          |
| ------------------------------------------- | ---------------------------- | ------------------------ | -------------------------- | ----------------------- |
| `GoalSymbolTable.actorsByName`              | rỗng                         | đã có                    | giữ nguyên                 | giữ nguyên              |
| `GoalSymbolTable.elementsByQualifiedName`   | rỗng                         | đã có                    | giữ nguyên                 | giữ nguyên              |
| `GoalSymbolTable.dependenciesByName`        | rỗng                         | đã có                    | đã resolve refs nếu hợp lệ | giữ nguyên              |
| `ElementSymbol.relations[*].targetRef`      | chưa có                      | đã có (raw token)        | giữ nguyên                 | giữ nguyên              |
| `ElementSymbol.relations[*].resolvedTarget` | null                         | null                     | đã gán nếu resolve được    | giữ nguyên              |
| `ElementSymbol.isLeaf`                      | mặc định                     | tạm thời                 | đã recompute từ refinement | giữ nguyên              |
| `ElementSymbol.goalContract`                | null                         | có nếu goal có clause    | giữ nguyên                 | giữ nguyên              |
| `ElementSymbol.taskContract`                | null                         | có nếu task có pre/post  | giữ nguyên                 | giữ nguyên              |
| `ElementSymbol.goalContractAssignmentCount` | 0                            | tăng khi attach contract | giữ nguyên                 | dùng để check `E5`      |
| `DependencySymbol.depender/dependee`        | null                         | null                     | đã gán nếu resolve được    | giữ nguyên              |
| `builder.issues`                            | rỗng                         | có thể có `S5`           | có thể thêm `S1`           | được merge vào `merged` |
| `merged issues`                             | chưa có                      | chưa có                  | chưa có                    | đầy đủ `S*` + `E*`      |


### 2.6 Snapshot kiểu `key -> value` (minh hoạ dữ liệu map)

Ví dụ dưới đây là biểu diễn minh hoạ để nhìn đúng dạng key-value trong symbol layer.

#### Sau `createEmptySymbolTable`

```text
GoalSymbolTable {
  actorsByName = {}
  elementsByQualifiedName = {}
  dependenciesByName = {}
}
```

#### Sau Pass 1 (`runDeclarationPass`)

```text
actorsByName = {
  "DeliverySystem" -> ActorSymbol(name="DeliverySystem", kind=AGENT, elementTable={...}),
  "WarehouseRole"  -> ActorSymbol(name="WarehouseRole",  kind=ROLE,  elementTable={...})
}

elementsByQualifiedName = {
  "DeliverySystem.CapacityLimit" -> ElementSymbol(
      kind=GOAL,
      ownerActor="DeliverySystem",
      relations=[
        RelationEntry(operator=REFINE_AND, targetRef="DispatchOrder", resolvedTarget=null)
      ],
      goalContract=GoalContract(type=MAINTAIN, bodyExpr=<Expression>, ...),
      taskContract=null,
      goalContractAssignmentCount=1
  ),
  "DeliverySystem.DispatchOrder" -> ElementSymbol(
      kind=TASK,
      relations=[],
      goalContract=null,
      taskContract=TaskContract(pre=<Expression>, post=<Expression>),
      goalContractAssignmentCount=0
  )
}

dependenciesByName = {
  "Dep01" -> DependencySymbol(
      dependerRawRef="DeliverySystem.DispatchOrder",
      dependeeRawRef="WarehouseRole.StockReady",
      depender=null,
      dependee=null
  )
}
```

#### Sau Pass 2 (`runResolutionPass`)

```text
elementsByQualifiedName["DeliverySystem.CapacityLimit"].relations = [
  RelationEntry(
    operator=REFINE_AND,
    targetRef="DispatchOrder",
    resolvedTarget=ElementSymbol("DeliverySystem.DispatchOrder")
  )
]

dependenciesByName["Dep01"] = DependencySymbol(
  dependerRawRef="DeliverySystem.DispatchOrder",
  dependeeRawRef="WarehouseRole.StockReady",
  depender=ElementSymbol("DeliverySystem.DispatchOrder"),
  dependee=ElementSymbol("WarehouseRole.StockReady")
)

builder.issues = [
  SemanticIssue(code="S1", message="Undeclared reference: ..."),   // nếu có
  SemanticIssue(code="S5", message="Duplicate declaration: ...")    // nếu có
]
```

#### Sau validation (`validateSemanticRules`)

```text
merged = builder.issues
       + analyzerV1Issues   // S1..S10
       + analyzerV2Issues   // E1..E7

// ví dụ shape:
merged = [
  SemanticIssue(code="S2", ...),
  SemanticIssue(code="S8", ...),
  SemanticIssue(code="E3", ...),
  SemanticIssue(code="E6", ...)
]
```

---

## 3) V1 semantic (S-series) hiện chạy như nào

### `GoalSemanticAnalyzer.traverseActorReferenceTree`

- Duyệt actor refs (`is-a`, `participates-in`).
- Resolve fail -> `S1`.
- Kind pair sai -> `S4`.

### `GoalSemanticAnalyzer.traverseElementRelationTree`

- Duyệt relation của từng element.
- Check:
  - `S6`: self-reference
  - `S7`: `=>` source không phải QUALITY
  - `S8`: `<>` source không phải RESOURCE
  - `S2`: matrix source/operator/target không hợp lệ
- Đồng thời collect refinement metadata cho bước sau.

### `GoalSemanticAnalyzer.traverseRefinementTargetMap`

- `S9`: cycle refinement (DFS).
- `S10`: cùng target bị trộn AND-refine và OR-refine.

### `GoalSemanticAnalyzer.traverseDependencyTree`

- `S3`: depender/dependee phải là leaf.

---

## 4) V2 semantic (E-series) hiện chạy như nào

### `OclSemanticAnalyzer.traverseGoalContractCardinality` -> `E5`

- Defensive check: một goal bị assign contract > 1 lần.
- Dựa trên `ElementSymbol.goalContractAssignmentCount`.

### `OclSemanticAnalyzer.traverseAchieveContracts` -> `E1`, `E2`

- Chỉ áp dụng cho `GoalContractType.ACHIEVE_UNIQUE`.
- `E1`: thiếu `iterVars`.
- `E2`: thiếu `sourceExpr` phần `in <expr>`.

### `OclSemanticAnalyzer.traverseContractBodies` -> `E4`

- Goal contract có keyword nhưng body null.
- Task contract tồn tại nhưng cả pre và post đều null.
- Lưu ý: chỉ pre hoặc chỉ post vẫn hợp lệ.

### `OclSemanticAnalyzer.traverseOclExpressions` -> `E3`, `E6`, `E7`

- Duyệt recursive cây `Expression`.
- `E3`: cấm chain `->` sau node trả scalar/boolean:
  - aggregate: `size`, `max`, `min`, `isEmpty`, `notEmpty`, `includes`, `excludes`, `count`
  - iterator boolean: `forAll`, `exists`, `isUnique`
- `E6`: navigation property không tồn tại trên USE class tương ứng.
- `E7`: kiểu iterator var không resolve được trong `MModel`.

Ví dụ nhanh:

```text
// E6: InvalidSelfNavigation
maintain: self.unknownField->notEmpty()
// nếu root USE class không có property/role "unknownField" -> E6

// E7: UnknownIterVarType
achieve for unique (s: UnknownType) in self.students:
  s.name <> ""
// "UnknownType" không resolve được trong MModel -> E7
```


---

## 6) Mapping mã lỗi theo nơi phát sinh


| Nơi phát sinh                                         | Mã lỗi                 |
| ----------------------------------------------------- | ---------------------- |
| `GoalSymbolTableBuilder` declaration                  | `S5`                   |
| `GoalSymbolTableBuilder` resolution                   | `S1`                   |
| `GoalSemanticAnalyzer.traverseActorReferenceTree`     | `S1`, `S4`             |
| `GoalSemanticAnalyzer.traverseElementRelationTree`    | `S2`, `S6`, `S7`, `S8` |
| `GoalSemanticAnalyzer.traverseRefinementTargetMap`    | `S9`, `S10`            |
| `GoalSemanticAnalyzer.traverseDependencyTree`         | `S3`                   |
| `OclSemanticAnalyzer.traverseGoalContractCardinality` | `E5`                   |
| `OclSemanticAnalyzer.traverseAchieveContracts`        | `E1`, `E2`             |
| `OclSemanticAnalyzer.traverseContractBodies`          | `E4`                   |
| `OclSemanticAnalyzer.traverseOclExpressions`          | `E3`, `E6`, `E7`       |


---

## 7) Debug flags hữu ích


| JVM flag                          | Ý nghĩa                        |
| --------------------------------- | ------------------------------ |
| `-Dgoal.dump.parsetree=true`      | Dump parse tree ANTLR          |
| `-Dgoal.dump.ast=true`            | Dump AST (`GoalModelCS`)       |
| `-Dgoal.dump.semantic.steps=true` | Log bước pipeline semantic     |
| `-Dgoal.dump.symbols=true`        | Dump symbol table sau Pass 1/2 |


---

## 8) Cơ chế check semantic theo từng nhóm lỗi

Mục này mô tả ngắn gọn theo format: **duyệt đâu -> lấy gì -> đối chiếu gì -> phát lỗi nào**.

### 8.1 Nhóm Builder issues (`S5`, `S1`) — từ AST sang Symbol

**Hàm chung:** `GoalSymbolTableBuilder.runDeclarationPass`, `runResolutionPass`

- **Duyệt:** cây AST `GoalModelCS`:
  - `actorDecls`
  - `intentionalElements` trong từng actor
  - `dependencyDecls`
- **Lấy ra:** tên actor/element/dependency, relation target token, dependency raw refs.
- **Đối chiếu:**
  - khi declare: check trùng key trong scope map (`actorsByName`, `elementTable`, `dependenciesByName`) -> `S5`.
  - khi resolve: map raw ref sang symbol thật (`resolveElement`) -> fail thì `S1`.

### 8.2 Nhóm Actor reference (`S1`, `S4`)

**Hàm chung:** `GoalSemanticAnalyzer.traverseActorReferenceTree`

- **Duyệt:** AST actor declarations (`is-a`, `participates-in` refs), đồng thời tra symbol table actor map.
- **Lấy ra:** `sourceActorKind`, token tên actor được tham chiếu.
- **Đối chiếu:**
  - không tìm thấy target actor trong `actorsByName` -> `S1`.
  - tìm thấy nhưng cặp kind không hợp lệ theo bảng luật (`isAAllowed` / `participatesInAllowed`) -> `S4`.

### 8.3 Nhóm Relation graph (`S2`, `S6`, `S7`, `S8`, `S9`, `S10`)

**Hàm chung:** `traverseElementRelationTree` + `traverseRefinementTargetMap`

- **Duyệt:** symbol graph `actor -> element -> relations`.
- **Lấy ra:** `source.kind`, `operator`, `resolvedTarget.kind`, token lỗi, metadata refinement (incoming edges, DFS graph).
- **Đối chiếu trong `traverseElementRelationTree`:**
  - `source == target` -> `S6`
  - `operator == QUALIFY` nhưng source không phải `QUALITY` -> `S7`
  - `operator == NEEDED_BY` nhưng source không phải `RESOURCE` -> `S8`
  - cặp `(sourceKind, operator, targetKind)` không nằm trong operator matrix -> `S2`
- **Đối chiếu tiếp trong `traverseRefinementTargetMap`:**
  - DFS thấy chu trình refinement -> `S9`
  - một target nhận lẫn `&>` và `|>` -> `S10`

### 8.4 Nhóm Dependency leaf (`S3`)

**Hàm:** `GoalSemanticAnalyzer.traverseDependencyTree`

- **Duyệt:** `dependenciesByName`.
- **Lấy ra:** `depender`, `dependee` đã resolve + cờ `isLeaf`.
- **Đối chiếu:** depender hoặc dependee không phải leaf -> `S3`.

### 8.5 Nhóm Contract cardinality/body (`E5`, `E1`, `E2`, `E4`)

**Các hàm:** `OclSemanticAnalyzer.traverseGoalContractCardinality`, `traverseAchieveContracts`, `traverseContractBodies`

- **Duyệt:** `elementsByQualifiedName`.
- **Lấy ra:** `goalContract`, `taskContract`, `goalContractAssignmentCount`, `goalContract.type`, `iterVars`, `sourceExpr`, `bodyExpr`, `pre/post`.
- **Đối chiếu:**
  - goal bị assign contract > 1 lần -> `E5`
  - `ACHIEVE_UNIQUE` nhưng `iterVars` rỗng/null -> `E1`
  - `ACHIEVE_UNIQUE` nhưng `sourceExpr` null -> `E2`
  - contract tồn tại nhưng body rỗng (goal body null, hoặc task pre/post đều null) -> `E4`

### 8.6 Nhóm OCL expression traversal (`E3`, `E6`, `E7`)

**Hàm chung:** `OclSemanticAnalyzer.traverseOclExpressions`

- **Duyệt:** cây `Expression` của từng contract (goal body, task pre, task post), theo kiểu recursive DFS.
- **Lấy ra:** node type (`OperationCallExp`, `IteratorExp`, `PropertyCallExp`, ...), chain trả về trung gian, biến iterator và type name, root/use class context.
- **Đối chiếu:**
  - gặp `->` sau node đã suy luận là scalar/boolean (`size`, `max`, `min`, `isEmpty`, `notEmpty`, `includes`, `excludes`, `count`, hoặc iterator boolean) -> `E3`
  - path bắt đầu `self.` có segment không resolve được trong USE class hiện tại -> `E6`
  - iterator var có `typeName` không resolve được trong `MModel` / type registry -> `E7`

### 8.7 Ý tưởng tổng quát để đọc flow

- **AST layer** trả lời câu hỏi: "khai báo gì, token nào, vị trí nào".
- **Symbol layer** trả lời câu hỏi: "đã resolve sang thực thể nào, quan hệ nào".
- **Semantic layer** so sánh các cặp/triples/ràng buộc trên symbol graph và expression tree để phát issue.

---

## 9) Các sửa đổi ngoài `parser` (core GOAL) dùng để làm gì

Mục này giải thích các thay đổi **không nằm trong package `org.vnu.sme.goal.parser`**, nhưng bắt buộc để semantic v2 chạy được end-to-end.

### 9.1 `goal/src/main/java/org/vnu/sme/goal/ast/GoalCS.java`

- **Ý nghĩa:** mở rộng AST của `goal` để giữ đủ dữ liệu cho `achieve for unique`.
- **Đã thêm:** `ACHIEVE_UNIQUE`, `iterVars`, `sourceExpression`, `clauseToken`.
- **Vì sao cần:** không có các field này thì builder/symbol không thể phát hiện `E1`/`E2`/`E7` đúng ngữ nghĩa.

### 9.2 `goal/src/main/java/org/vnu/sme/goal/ast/TaskCS.java`

- **Ý nghĩa:** giữ token của `pre`/`post` clause.
- **Đã thêm:** `preToken`, `postToken`.
- **Vì sao cần:** khi phát `E4` (empty task contract), semantic có thể báo vị trí lỗi ổn định hơn, thay vì chỉ fallback vào token khai báo task.

### 9.3 `goal/src/main/java/org/vnu/sme/goal/mm/ocl/AggregateCallExp.java`

- **Ý nghĩa:** bổ sung node OCL chuyên biệt cho aggregate calls (`size`, `max`, `count`, `includes`, ...).
- **Vì sao cần:** `E3` cần phân biệt node nào trả scalar/boolean để cấm chain `->` tiếp theo. Nếu chỉ dùng `OperationCallExp` chung thì semantic phải suy đoán bằng string rời rạc, dễ sai.

### 9.4 `goal/src/main/java/org/vnu/sme/goal/parser/GoalModelFactory.java` (package parser nhưng thuộc core model mapping)

- **Ý nghĩa:** đồng bộ factory mapping giữa AST mới và meta-model runtime.
- **Đã chỉnh:** xử lý `GoalCS.GoalType.ACHIEVE_UNIQUE` tương thích với luồng materialize hiện tại (map về `Achieve` clause).
- **Vì sao cần:** tránh break đường compileSpecification -> create GoalModel khi AST đã có enum mới.

### 9.5 `goal/src/main/java/org/vnu/sme/goal/parser/debug/GoalSymbolTablePrinter.java`

- **Ý nghĩa:** nâng cấp tooling debug để nhìn thấy contract v2 trong dump.
- **Đã thêm:** in `goalContract`, `taskContract`, iterVars/source/body.
- **Vì sao cần:** khi kiểm tra pass1/pass2 hoặc debug lỗi `E*`, có thể quan sát trực tiếp dữ liệu semantic đã attach vào symbol.

### 9.6 `goal/src/main/java/org/vnu/sme/goal/parser/semantic/symbols/*` (nhóm core semantic data model)

- **Các file mới/chỉnh:**
  - `GoalContract.java`
  - `TaskContract.java`
  - `GoalContractType.java`
  - `ElementSymbol.java` (thêm contract slots + assignment count)
- **Ý nghĩa:** tạo data model semantic trung gian (không phụ thuộc trực tiếp parse tree).
- **Vì sao cần:** giữ đúng kiến trúc v1: analyzer đọc từ symbol table; parser/AST chỉ là input stage.

### 9.7 Tóm tắt ngắn

Các sửa đổi ngoài `parser` không phải “đổi logic business tuỳ tiện”, mà là:

1. **Mở rộng domain model** để biểu diễn khái niệm mới của v2 (`achieve for unique`, aggregate semantics),
2. **Giữ tương thích runtime model** khi materialize `GoalModel`,
3. **Tăng khả năng quan sát/debug** cho pipeline semantic.

