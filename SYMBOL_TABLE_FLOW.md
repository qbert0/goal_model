# Symbol Table Build Flow & Semantic Checks

Tài liệu này mô tả luồng **xây dựng Symbol Table** từ AST và **các kiểm tra semantic** sau khi đã có symbol table cho ngôn ngữ GOAL V1 (chưa có OCL).

Entry point trong code:

- `goal/src/main/java/org/vnu/sme/goal/parser/semantic/pipeline/GoalSemanticPipelineSkeleton.java` — orchestrator chính, chạy 2 pass + gọi tuần tự các hàm validate.
- `goal/src/main/java/org/vnu/sme/goal/parser/semantic/symbols/GoalSymbolTableBuilder.java` — dựng bảng + ghi `S1`/`S5` trong lúc build.
- `goal/src/main/java/org/vnu/sme/goal/parser/semantic/GoalSemanticAnalyzer.java` — chứa toàn bộ business rule cho `S2`, `S3`, `S4`, `S6`, `S7`, `S8`.

---

## 1) Call tree đầy đủ

```text
GoalSemanticPipelineSkeleton.run(ast, model, err)
├─ builder = new GoalSymbolTableBuilder()
│  └─ Khởi tạo builder, giữ state issue list trong quá trình build (S1/S5).
├─ analyzer = new GoalSemanticAnalyzer()
│  └─ Holder các hàm validate semantic; OPERATOR_MATRIX là hằng số nội bộ.
├─ table = createEmptySymbolTable(ast)
│  └─ new GoalSymbolTable(modelName)
├─ declarationPass(ast, table, err)
│  └─ builder.runDeclarationPass(ast, table)
│     ├─ for each ActorDeclCS: registerActor(...)
│     │  ├─ create ActorSymbol (ActorKind từ ActorCS/AgentCS/RoleCS)
│     │  ├─ put vào table.actorsByName            (duplicate => S5)
│     │  └─ for each IntentionalElementCS: registerElement(...)
│     │     ├─ create ElementSymbol (GOAL/TASK/QUALITY/RESOURCE)
│     │     ├─ put vào actor.elementTable          (duplicate trong actor => S5)
│     │     ├─ put vào table.elementsByQualifiedName ("Actor.Element")
│     │     └─ OutgoingLink -> RelationEntry(raw target token, chưa resolve)
│     └─ for each DependencyCS: registerDependency(...)
│        ├─ create DependencySymbol(raw depender/dependee refs)
│        ├─ create dependum ElementSymbol inline (owner ảo "__dependency__<name>")
│        └─ put vào table.dependenciesByName       (duplicate => S5)
├─ resolutionPass(ast, table, err)
│  └─ builder.runResolutionPass(ast, table)
│     ├─ resolveRelationTargets(table)
│     │  ├─ targetText có "." -> table.resolveElement("Actor.Element")
│     │  ├─ targetText không "." -> ownerActor.elementTable.get(targetText)
│     │  └─ resolve fail => S1
│     ├─ resolveDependencyReferences(table)
│     │  ├─ resolve qualified depender/dependee
│     │  └─ resolve fail => S1
│     └─ recomputeLeafFlags(table)
│        ├─ reset all element.leaf = true
│        └─ refinement target (REFINE_AND/REFINE_OR) => target.leaf = false
├─ computeDerivedFlags(table, err)
│  └─ Hiện chỉ log; leaf đã recompute trong resolutionPass.
└─ validateSemanticRules(ast, table, model, err)
   ├─ merged = builder.getIssues()                    [S1, S5]
   ├─ analyzer.validateOperatorMatrix(table)          [S2]
   │  └─ Bỏ qua relation chưa resolve.
   │  └─ Bỏ qua => khi source ≠ QUALITY (đã do S7 xử lý).
   │  └─ Bỏ qua <> khi source ≠ RESOURCE (đã do S8 xử lý).
   │  └─ Tra OPERATOR_MATRIX[source.kind][operator] -> EnumSet<ElementKind>.
   │  └─ target.kind không nằm trong set hợp lệ => S2.
   ├─ analyzer.validateActorRelationships(ast, table) [S4 + S1 cho actor ref]
   │  └─ duyệt isARefs / participatesInRefs trên ActorDeclCS.
   │  └─ resolve fail => S1; sai cặp kind => S4.
   ├─ analyzer.validateDependencyOnLeaf(table)        [S3]
   │  └─ depender hoặc dependee không phải leaf => S3.
   ├─ analyzer.validateSelfReference(table)           [S6]
   │  └─ source == resolvedTarget => S6.
   ├─ analyzer.validateQualifySourceIsQuality(table)  [S7]
   │  └─ operator=QUALIFY và source.kind != QUALITY => S7.
   └─ analyzer.validateNeededBySourceIsResource(table) [S8]
      └─ operator=NEEDED_BY và source.kind != RESOURCE => S8.
└─ printSemanticIssues(issues, err)
   └─ Bật khi `-Dgoal.dump.semantic.steps=true`.
```

---

## 2) Trạng thái bảng theo từng pass

### Sau `createEmptySymbolTable(ast)`

- `actorsByName = {}`
- `elementsByQualifiedName = {}`
- `dependenciesByName = {}`

### Sau Pass 1 (`runDeclarationPass`)

- `actorsByName` đã có các `ActorSymbol`.
- Mỗi `ActorSymbol.elementTable` đã có `ElementSymbol`.
- `elementsByQualifiedName` đã có key dạng `Actor.Element`.
- Mỗi `ElementSymbol.relations` đã có `RelationEntry` với `targetRef` còn raw.
- `dependenciesByName` đã có `DependencySymbol` (depender/dependee còn raw).
- `builder.issues` có thể chứa sẵn `S5` (duplicate).

### Sau Pass 2 (`runResolutionPass`)

- `RelationEntry.resolvedTarget` được gán nếu resolve được.
- `DependencySymbol.depender/dependee` được gán nếu resolve được.
- `ElementSymbol.leaf` đã được recompute theo refinement edges.
- `builder.issues` có thể có thêm `S1` (undeclared reference).

### Sau `validateSemanticRules`

- Trả về list `SemanticIssue` đã merge: builder issues + các check trong analyzer.

---

## 3) Lookup rule khi resolve relation target

Trong `resolveElementReference(ownerActor, targetText, table)`:

- Nếu `targetText` chứa `.` -> `table.resolveElement("Actor.Element")` (global qualified).
- Nếu không có `.` -> tìm trong `ownerActor.elementTable` (local scope của actor đang xét).

---

## 4) Nơi lưu lỗi và mã lỗi tương ứng

| Nơi phát sinh | Mã lỗi | Mô tả ngắn |
|---|---|---|
| `GoalSymbolTableBuilder.runDeclarationPass` | `S5` | Duplicate declaration trong cùng scope |
| `GoalSymbolTableBuilder.runResolutionPass` | `S1` | Relation target / dependency endpoint chưa khai báo |
| `GoalSemanticAnalyzer.validateOperatorMatrix` | `S2` | Cặp `(source.kind, operator, target.kind)` không hợp lệ |
| `GoalSemanticAnalyzer.validateDependencyOnLeaf` | `S3` | `depender` hoặc `dependee` không phải leaf |
| `GoalSemanticAnalyzer.validateActorRelationships` | `S1`, `S4` | Actor ref chưa khai báo / sai kind cho `:` `>` |
| `GoalSemanticAnalyzer.validateSelfReference` | `S6` | Element trỏ về chính nó |
| `GoalSemanticAnalyzer.validateQualifySourceIsQuality` | `S7` | `=>` mà source không phải `QUALITY` |
| `GoalSemanticAnalyzer.validateNeededBySourceIsResource` | `S8` | `<>` mà source không phải `RESOURCE` |

---

## 5) Giải thích trực quan các semantic check mới

Phần này trình bày từng rule mới được implement trong `GoalSemanticAnalyzer`, kèm ví dụ tối giản và lý do thiết kế.

### 5.1 S2 — InvalidOperatorMatrix

**Cơ sở dữ liệu**: hằng số `OPERATOR_MATRIX` trong `GoalSemanticAnalyzer`, kiểu `Map<ElementKind, Map<OutgoingLink.Kind, EnumSet<ElementKind>>>`. Đây là biểu diễn 1-1 của bảng ma trận trong `SYMBOL.md` mục 3.

```text
GOAL     -> &> | |>             : { GOAL, TASK }
            ++> | +> | -> | --> : { QUALITY }
TASK     -> &> | |>             : { GOAL, TASK }
            ++> | +> | -> | --> : { QUALITY }
QUALITY  -> =>                  : { GOAL, TASK, RESOURCE }
            ++> | +> | -> | --> : { QUALITY }
RESOURCE -> <>                  : { TASK }
            ++> | +> | -> | --> : { QUALITY }
```

**Trình tự xử lý**:

1. Bỏ qua relation chưa resolve (đã thuộc `S1`).
2. Nếu là `=>` mà source không phải `QUALITY` → đã thuộc `S7`, không bắn S2 nữa.
3. Nếu là `<>` mà source không phải `RESOURCE` → đã thuộc `S8`, không bắn S2 nữa.
4. Còn lại tra cứu `OPERATOR_MATRIX[source.kind][operator]`. Nếu `target.kind` không nằm trong set hợp lệ → emit `S2`.

**Ví dụ phát hiện S2**:

```text
agent System {
    goal G { }
    task T &> G   { }   // OK: TASK &> GOAL
    resource R &> G { } // S2: RESOURCE không có entry &>
    quality Q => Q { }  // S2: QUALITY => QUALITY (target sai), không phải S7
}
```

Lưu ý: case cuối là điểm tinh tế — `S7` chỉ check source, nên target sai vẫn phải thuộc về S2.

### 5.2 S4 — InvalidActorRelationship (kèm S1 cho actor ref)

**Ràng buộc kiểu actor (theo spec)**:

| Toán tử | Cặp hợp lệ |
|---|---|
| `:` (is-a) | `ACTOR -> ACTOR`, `ROLE -> ROLE` |
| `>` (participates-in) | `AGENT -> ROLE`, `AGENT -> AGENT` |

**Trình tự**:

1. Duyệt từng `ActorDeclCS` trong AST.
2. Với mỗi token trong `isARefs` / `participatesInRefs`:
   - Resolve token → `ActorSymbol` qua `actorsByName`.
   - Không tìm thấy → `S1` "Undeclared actor reference".
   - Tìm thấy nhưng sai cặp kind → `S4`.

**Ví dụ phát hiện S4**:

```text
actor Customer { }
role Dispatcher { }

actor VIP : Customer { }            // OK: ACTOR : ACTOR
role Senior : Dispatcher { }        // OK: ROLE : ROLE
agent Bot > Dispatcher { }          // OK: AGENT > ROLE
role X : Customer { }               // S4: ROLE : ACTOR
actor Y > Customer { }              // S4: ACTOR > ACTOR
agent Z : Customer { }              // S4: AGENT : ACTOR
agent Q > NotExist { }              // S1: actor "NotExist" chưa khai báo
```

### 5.3 S6 — SelfReference

Element không được tạo relation trỏ về chính nó.

**Trình tự**: với mỗi `RelationEntry` đã resolve, so sánh `source == resolvedTarget` (bằng tham chiếu object, không phải tên).

**Ví dụ**:

```text
agent System {
    task T &> T { }   // S6: T tự refine chính nó
    quality Q +> Q { } // S6: Q tự đóng góp cho chính nó
}
```

Lưu ý: case `task T &> Other.T` (tên giống nhưng khác actor) **không** phải self-reference.

### 5.4 S7 — QualifySourceNotQuality

Toán tử `=>` (Qualification) chỉ hợp lệ khi nguồn là `QUALITY`.

**Trình tự**: chỉ xét relation operator = `QUALIFY` và đã resolve target. Nếu `source.kind != QUALITY` → `S7`.

**Ví dụ**:

```text
agent System {
    quality SystemSecurity => DriverDatabase { }  // OK: QUALITY => RESOURCE
    task T => DriverDatabase { }                  // S7: TASK không thể là source của =>
    goal G => DriverDatabase { }                  // S7
    resource R => DriverDatabase { }              // S7
}
```

### 5.5 S8 — NeededBySourceNotResource

Toán tử `<>` (NeededBy) chỉ hợp lệ khi nguồn là `RESOURCE`, target là `TASK`.

**Trình tự**: chỉ xét relation operator = `NEEDED_BY` và đã resolve target. Nếu `source.kind != RESOURCE` → `S8`.

**Ví dụ**:

```text
agent System {
    resource DriverDatabase <> AssignDriver { }  // OK: RESOURCE <> TASK
    task T <> AssignDriver { }                    // S8: source là TASK
    goal G <> AssignDriver { }                    // S8
    resource R <> Goal1 { }                       // S2: RESOURCE đúng nhưng <> không cho target GOAL
}
```

Lưu ý: case cuối — source đúng (`RESOURCE`) nên S8 không fire, nhưng target sai (`GOAL`), nên rơi vào S2.

### 5.6 Bảng quyết định gọn (decision table)

Để tránh double-report, thứ tự ưu tiên khi 1 relation có thể vi phạm nhiều rule:

```text
relation r với operator op, source s, target t

if r.resolvedTarget == null:
    -> S1 (đã do builder bắt)
    -> KHÔNG check S2/S6/S7/S8 trên r

else:
    if s == t:
        -> S6
    
    if op == =>:
        if s.kind != QUALITY: -> S7 (skip S2)
        else: check S2 với t.kind
    
    elif op == <>:
        if s.kind != RESOURCE: -> S8 (skip S2)
        else: check S2 với t.kind
    
    else:
        check S2 trực tiếp
```

`S3` chạy độc lập trên `dependenciesByName`. `S4` chạy độc lập trên actor declarations.

### 5.7 Ví dụ tổng hợp — cùng 1 model nhiều rule

```text
istar Demo

actor A { }
role  R { }

agent X : A { }                        // S4: AGENT : ACTOR
agent Y > Missing { }                  // S1 (actor): Missing chưa khai báo

agent System {
    goal G1 { }
    quality Q1 { }
    resource Res1 { }

    task T1 &> G1 { }                  // OK
    task T2 &> Q1 { }                  // S2: TASK &> QUALITY (không hợp lệ)
    task T3 &> T3 { }                  // S6: self-reference
    task T4 &> Unknown { }             // S1: target chưa khai báo

    quality Q2 => G1 { }               // OK
    task T5 => G1 { }                  // S7: TASK không thể là source =>
    resource Res2 <> T1 { }            // OK
    goal  G2 <> T1 { }                 // S8: GOAL không thể là source <>
}

dependency D1 {
    depender System.G1                 // G1 không leaf vì bị T1 refine
    dependee  System.T1                // T1 không leaf nếu có gì refine nó
    dependum  goal Mid { }
}                                       // S3: depender hoặc dependee không phải leaf
```

Pipeline sẽ phát ra một loạt issue tương ứng, mỗi issue gắn line/column từ token gây lỗi để IDE hoặc CLI có thể trỏ chính xác vị trí.

---

## 6) Flag debug có sẵn

| Flag JVM | Tác dụng |
|---|---|
| `-Dgoal.dump.parsetree=true` | In ra parse tree sau khi parse. |
| `-Dgoal.dump.ast=true` | In ra AST đã build. |
| `-Dgoal.dump.semantic.steps=true` | Log từng bước trong pipeline semantic. |
| `-Dgoal.dump.symbols=true` | Dump symbol table sau Pass1 và Pass2. |
