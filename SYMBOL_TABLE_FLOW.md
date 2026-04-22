# Symbol Table Build Flow (Only)

Phạm vi tài liệu này chỉ mô tả phần **xây dựng Symbol Table** từ AST đã có sẵn.

Entry point trong code:

- `goal/src/main/java/org/vnu/sme/goal/parser/semantic/pipeline/GoalSemanticPipelineSkeleton.java`
- `goal/src/main/java/org/vnu/sme/goal/parser/semantic/symbols/GoalSymbolTableBuilder.java`

---

## 1) Call tree chỉ cho Symbol Table

```text
GoalSemanticPipelineSkeleton.run(ast, model, err)
├─ builder = new GoalSymbolTableBuilder()
│  └─ Khởi tạo builder giữ state issue list trong quá trình build.
├─ table = createEmptySymbolTable(ast)
│  ├─ Tạo root symbol table theo tên model trong AST.
│  └─ new GoalSymbolTable(modelName)
├─ declarationPass(ast, table, err)
│  └─ builder.runDeclarationPass(ast, table)
│     ├─ for each ActorDeclCS: registerActor(...)
│     │  └─ Duyệt từng actor/agent/role trong AST để đăng ký symbol actor.
│     │  ├─ create ActorSymbol (ActorKind)
│     │  │  └─ Xác định kind từ class AST: ActorCS/AgentCS/RoleCS.
│     │  ├─ put vào table.actorsByName
│     │  │  └─ Tạo global namespace cho actor; duplicate => S5.
│     │  └─ for each IntentionalElementCS: registerElement(...)
│     │     └─ Duyệt phần tử bên trong actor hiện tại.
│     │     ├─ create ElementSymbol (ElementKind)
│     │     │  └─ Xác định kind: GOAL/TASK/QUALITY/RESOURCE.
│     │     ├─ put vào actor.elementTable (local scope)
│     │     │  └─ Bảng local theo actor để resolve target không-qualified.
│     │     ├─ put vào table.elementsByQualifiedName (Actor.Element)
│     │     │  └─ Bảng global qualified cho ref dạng A.B.
│     │     └─ OutgoingLink -> RelationEntry(raw target token)
│     │        └─ Lưu cạnh relation ở dạng thô, chưa resolve target.
│     └─ for each DependencyCS: registerDependency(...)
│        └─ Duyệt các dependency declaration ở cấp model.
│        ├─ create DependencySymbol(raw depender/dependee refs)
│        │  └─ Lưu string ref ban đầu để resolve ở Pass 2.
│        ├─ create dependum ElementSymbol inline (owner ảo)
│        │  └─ Gắn dependum vào dependency như một symbol độc lập.
│        └─ put vào table.dependenciesByName
│           └─ Bảng global dependency; duplicate => S5.
├─ resolutionPass(ast, table, err)
│  └─ builder.runResolutionPass(ast, table)
│     ├─ resolveRelationTargets(table)
│     │  └─ Nối target raw của từng relation tới ElementSymbol thực.
│     │  └─ set RelationEntry.resolvedTarget
│     │     └─ Nếu không resolve được thì ghi lỗi S1.
│     ├─ resolveDependencyReferences(table)
│     │  └─ Resolve depender/dependee raw refs sang symbol thực.
│     │  └─ set dep.depender / dep.dependee
│     │     └─ Không tìm thấy endpoint => S1.
│     └─ recomputeLeafFlags(table)
│        └─ Tính lại leaf từ đầu dựa trên refinement relations đã resolve.
│        ├─ reset all element.leaf = true
│        └─ refinement target => leaf = false
└─ validateSemanticRules(table, model, err)
   └─ Hợp nhất lỗi build + check semantic cơ bản hiện tại.
   ├─ merged builder.getIssues()
   │  └─ Bao gồm S1 (undeclared), S5 (duplicate) từ builder.
   └─ check dependency on non-leaf (S3)
      └─ depender/dependee không phải leaf thì báo lỗi S3.
```

---

## 2) Trạng thái bảng theo từng pass

### Sau `createEmptySymbolTable(ast)`

- `actorsByName = {}`
- `elementsByQualifiedName = {}`
- `dependenciesByName = {}`

### Sau Pass 1 (`runDeclarationPass`)

- `actorsByName` đã có các `ActorSymbol`
- mỗi `ActorSymbol.elementTable` đã có `ElementSymbol`
- `elementsByQualifiedName` đã có key dạng `Actor.Element`
- mỗi `ElementSymbol.relations` đã có `RelationEntry` (target còn raw)
- `dependenciesByName` đã có `DependencySymbol` (depender/dependee raw)

### Sau Pass 2 (`runResolutionPass`)

- `RelationEntry.resolvedTarget` được gán (nếu resolve được)
- `DependencySymbol.depender/dependee` được gán (nếu resolve được)
- `ElementSymbol.leaf` được recompute theo refinement edges

---

## 3) Lookup rule đang dùng khi resolve relation target

Trong `resolveElementReference(ownerActor, targetText, table)`:

- Nếu `targetText` chứa dấu `.` -> lookup global bằng `table.resolveElement("Actor.Element")`
- Nếu không chứa `.` -> lookup local trong `ownerActor.elementTable`

---

## 4) Nơi lưu lỗi trong quá trình build

`GoalSymbolTableBuilder.issues` (list `SemanticIssue`)

- `S5`: duplicate declaration (actor/element/dependency)
- `S1`: undeclared reference (relation target / dependency endpoint)

Pipeline merge thêm:

- `S3`: dependency endpoint không phải leaf
