# Định hướng mở rộng Compiler GOAL — v2 (OCL Extension)
## Phạm vi: Semantic Check & Báo lỗi

> Tài liệu này chỉ mô tả **delta về phần semantic check** so với v1, dựa trên `GOAL_SymbolTable_Spec.md` và grammar `GOAL.g4`.
>
> Giả định: OclExpr node tree, GoalContract, TaskContract đã được implement và populate đầy đủ vào symbol table trước khi chạy các check này.
>
> Quy ước:
> - 🔧 **Nâng cấp** — check đã có trong v1, cần sửa hoặc mở rộng
> - 🆕 **Mới hoàn toàn** — check chưa tồn tại trong v1

---

## 1. Nhìn lại v1 — Trạng thái semantic check hiện tại

Trong `GOAL_SymbolTable_Spec.md` mục 6, v1 có 10 lỗi S1–S10, **không có lỗi nào liên quan đến OCL**. Toàn bộ S1–S10 **giữ nguyên, không cần sửa**. V2 bổ sung thêm nhóm **E-series** chạy sau S1–S10.

| # | Tên lỗi | Phạm vi |
|---|---|---|
| S1 | `UndeclaredReference` | Quan hệ giữa các element |
| S2 | `InvalidOperatorMatrix` | Quan hệ giữa các element |
| S3 | `DependencyOnNonLeaf` | Dependency |
| S4 | `InvalidActorRelationship` | Actor tổ chức |
| S5 | `DuplicateDeclaration` | Khai báo trùng tên |
| S6 | `SelfReference` | Quan hệ tự trỏ |
| S7 | `QualifySourceNotQuality` | Toán tử `=>` |
| S8 | `NeededBySourceNotResource` | Toán tử `<>` |
| S9 | `CircularRefinement` | Đồ thị Refinement |
| S10 | `MixedRefinementType` | Đồ thị Refinement |

---

## 2. Những gì tôi hiểu nhầm trước khi đọc grammar — Đính chính

Sau khi đọc `GOAL.g4`, một số điểm trong roadmap cũ cần đính chính trước khi viết lại:

| Điểm nhầm | Thực tế trong grammar |
|---|---|
| `achieve` bắt buộc có `for unique (...)` | Grammar có **2 dạng** `achieveClause`: dạng đơn giản `achieve: <expr>` và dạng đầy đủ `achieve for unique (...) in <expr>: <expr>`. E4/E5 chỉ áp dụng cho dạng đầy đủ nếu thiếu phần bắt buộc |
| Spec chỉ có `and`, `not` | Grammar có đủ `or`, `implies`, `not`, `and` — `or` là hợp lệ |
| `pre` bắt buộc phải có `post` | `taskBody` định nghĩa `preClause?` và `postClause?` **độc lập** — `pre` đứng một mình là **hợp lệ về mặt grammar**, C1 không cần confirm nữa |
| E1/E2 cần semantic check vì `quality`/`resource` có thể có contract | Grammar dùng `elementBody` (chỉ có `descriptionClause`) cho `qualityDecl` và `resourceDecl`, **không có slot nào cho contract** — E1/E2 **không thể xảy ra** vì parser đã block, không cần semantic check |
| `dependum` là struct riêng | Grammar định nghĩa `dependumClause: DEPENDUM intentionalElement` — dependum tái dùng rule `intentionalElement`, tức là một `goal`/`task`/`quality`/`resource` đầy đủ |
| `NEEDED_BY` và `NEQ` là 2 token khác nhau | Cùng lexeme `<>` — parser phân biệt bằng context: trong `equalityExpr` là `NEQ`, trong `relation` là `NEEDED_BY` |
| `CONTRIB_HURT` (`->`) và `ARROW` cần lexer phân biệt | Cùng lexeme `->` — parser phân biệt bằng context: trong `pathSuffix` là `ARROW`, trong `relationList` là `CONTRIB_HURT` |

---

## 3. Các check mới — Nhóm E-series 🆕

Sau khi đính chính, E-series thực sự cần implement gồm **5 lỗi**, chia 2 nhóm:

- **E1–E2**: Kiểm tra tính hợp lệ của `achieve for unique` khi dùng dạng đầy đủ
- **E3–E5**: Kiểm tra tính hợp lệ bên trong cây OclExpr

---

### 3.1 Nhóm Achieve Syntax (E1–E2)

Grammar định nghĩa `achieveClause` có **2 dạng hợp lệ**:

```
// Dạng 1 — đơn giản
achieve: <expression>

// Dạng 2 — đầy đủ
achieve for unique (<typedVarList>) in <expression> : <expression>
```

Lỗi E1 và E2 **chỉ áp dụng cho dạng 2** khi biến lặp hoặc tập nguồn bị thiếu. Tuy nhiên vì grammar đã bắt buộc cú pháp dạng 2 phải có đủ `typedVarList` và cả 2 `expression`, hai lỗi này thực chất là **lưới an toàn semantic** — dùng để bắt trường hợp AST build sai.

#### E1 — `AchieveMissingIterVar` 🆕

**Điều kiện:** `GoalContract.type = ACHIEVE_UNIQUE` (dạng đầy đủ) nhưng `GoalContract.iterVars` là null hoặc rỗng sau khi build từ AST.

**Nguồn gốc:** Spec v2 Mục 3.3 — `achieve for unique (s: Student, c: Class) in ...` yêu cầu ít nhất một `typedVar`.

**Cách phát hiện:**
```
Với mỗi ElementSymbol có kind=GOAL
  và goalContract.type = ACHIEVE_UNIQUE:
    nếu goalContract.iterVars == null OR iterVars.isEmpty():
        → E1: AchieveMissingIterVar
```

**Ví dụ vi phạm** (cú pháp không hợp lệ ở grammar nhưng AST có thể build sai):
```
goal FulfillRegistrations {
    achieve for unique () in self.pendingRequests:
        c.enrolledStudents->includes(s)
}
```

---

#### E2 — `AchieveMissingSource` 🆕

**Điều kiện:** `GoalContract.type = ACHIEVE_UNIQUE` nhưng `GoalContract.sourceExpr` là null sau khi build từ AST.

**Nguồn gốc:** Spec v2 Mục 3.3 — phần `in <expression>` chỉ định tập nguồn, bắt buộc trong dạng đầy đủ.

**Cách phát hiện:**
```
Với mỗi ElementSymbol có kind=GOAL
  và goalContract.type = ACHIEVE_UNIQUE:
    nếu goalContract.sourceExpr == null:
        → E2: AchieveMissingSource
```

**Ví dụ vi phạm** (AST build được nhưng sourceExpr null):
```
goal FulfillRegistrations {
    achieve for unique (s: Student, c: Class):   // → E2: thiếu "in <expr>"
        c.enrolledStudents->includes(s)
}
```

**Ví dụ hợp lệ:**
```
// Dạng đầy đủ — có đủ iterVars và sourceExpr
goal FulfillRegistrations {
    achieve for unique (s: Student, c: Class) in self.pendingRequests:
        c.enrolledStudents->includes(s) and
        s.invoices->exists(i | i.relatedClass = c and i.status = "ISSUED")
}

// Dạng đơn giản — không cần iterVars và sourceExpr → không áp dụng E1/E2
goal AllDriversLicensed {
    achieve: self.drivers->forAll(d | d.licensed = true)
}
```

---

### 3.2 Nhóm OclExpr Validity (E3–E5)

Ba lỗi này yêu cầu **duyệt cây OclExpr** sau khi đã build xong. Cần implement hàm `validateOclExpr(node: OclExpr)` duyệt đệ quy từ gốc xuống lá, được gọi trên `bodyExpr` của mọi `GoalContract` và `TaskContract`.

#### E3 — `InvalidCollectionChain` 🆕

**Điều kiện:** Toán tử `->` (`ARROW`) được dùng ngay sau kết quả của `size()`, `max()`, `min()`, `isEmpty()`, `notEmpty()`, `includes()`, `excludes()` — những `aggregateCall` trả về **scalar hoặc boolean**, không phải collection.

**Nguồn gốc:** Spec v2 Mục 6.3 — các Aggregation Functions trả về giá trị đơn. Chỉ `collect` (và `iteratorCall` nói chung qua `COLLECT`) mới trả về collection có thể chain tiếp.

**Tham chiếu grammar:**
```
pathSuffix
    : ARROW collectionCall    // ->forAll, ->size, ->collect, ...
    ;

aggregateCall
    : aggregateOp LPAREN argumentList? RPAREN   // size(), max(), min(), includes(), ...
    | COUNT LPAREN iteratorVars BAR expression RPAREN
    ;
```

**Cách phát hiện:** Khi duyệt `NavExpr`, theo dõi kiểu trả về của từng `pathSuffix`:

```
duyệt pathSuffix theo thứ tự, track returnType:

    ARROW + iteratorCall(FORALL | EXISTS):  returnType = BOOLEAN
    ARROW + iteratorCall(COLLECT):          returnType = COLLECTION
    ARROW + aggregateCall(SIZE|MAX|MIN
            |IS_EMPTY|NOT_EMPTY
            |INCLUDES|EXCLUDES|COUNT):      returnType = SCALAR_OR_BOOL
    DOT + IDENT:                            returnType = OBJECT

    Nếu gặp ARROW khi returnType == SCALAR_OR_BOOL:
        → E3: InvalidCollectionChain
```

**Ví dụ vi phạm:**
```
self.students->size()->forAll(x | x > 0)     // → E3: size() không trả về collection
self.values->max()->collect(x | x + 1)       // → E3: max() không trả về collection
self.items->isEmpty()->exists(x | x > 0)     // → E3: isEmpty() trả về boolean
```

**Ví dụ hợp lệ:**
```
// collect trả về collection → có thể chain ->max()
self.doors->collect(d | d.openingTime)->max()    // → OK

// count trả về số → không chain thêm
self.items->count(x | x > 0)                    // → OK
```

---

#### E4 — `EmptyContractBody` 🆕

**Điều kiện:** `GoalContract.bodyExpr == null` hoặc `TaskContract` tồn tại nhưng cả `precondition` lẫn `postcondition` đều null.

**Nguồn gốc:** Ngầm định từ Spec v2 — một contract keyword xuất hiện nhưng không có biểu thức theo sau là vô nghĩa. Grammar đã bắt buộc `expression` sau mỗi keyword, nên đây là lưới an toàn semantic thứ hai.

**Lưu ý về `pre` đứng một mình:**
Grammar định nghĩa `preClause?` và `postClause?` **độc lập** trong `taskBody` — tức là `pre` không có `post` là **hợp lệ**. E4 **không** báo lỗi khi chỉ có `pre` mà không có `post`.

**Cách phát hiện:**
```
Với mỗi ElementSymbol có kind=GOAL và goalContract != null:
    nếu goalContract.bodyExpr == null:
        → E4: EmptyContractBody

Với mỗi ElementSymbol có kind=TASK và taskContract != null:
    nếu taskContract.precondition == null AND taskContract.postcondition == null:
        // Chỉ lỗi khi CẢ HAI đều null — tức là taskContract tồn tại nhưng rỗng hoàn toàn
        → E4: EmptyContractBody
    // pre có mà không có post → OK (hợp lệ theo grammar)
    // post có mà không có pre → OK (hợp lệ theo grammar)
```

**Ví dụ vi phạm:**
```
// goalContract được tạo ra (keyword xuất hiện) nhưng bodyExpr null
agent DeliverySystem {
    goal CapacityLimit {
        maintain:          // → E4: keyword có nhưng không có expression theo sau
    }

    task RegisterStudent {
        pre:               // → E4: cả pre lẫn post đều không có expression
        post:
    }
}
```

**Ví dụ hợp lệ:**
```
agent DeliverySystem {
    goal CapacityLimit {
        maintain:
            self.classes->forAll(c | c.enrolledStudents->size() <= c.maxCapacity)
    }

    // pre đứng một mình không có post → OK theo grammar
    task ProcessOrder &> DeliverPackage {
        pre:
            self.pendingRequests->exists(r | r.orderId = orderId)
    }

    // post đứng một mình không có pre → OK theo grammar
    task ConfirmDelivery {
        post:
            self.deliveries->exists(d | d.status = "COMPLETED")
    }
}
```

---

#### E5 — `DuplicateGoalContractType` 🆕

**Điều kiện:** Một `goal` khai báo nhiều hơn một `goalClause`. Trong grammar:

```
goalBody
    : descriptionClause?
      goalClause?        // chỉ 1 goalClause duy nhất
    ;

goalClause
    : achieveClause
    | maintainClause
    | avoidClause
    ;
```

Grammar đã giới hạn `goalClause?` (tối đa 1). Tuy nhiên nếu parser implementation xử lý lỏng lẻo và cho phép 2 block xuất hiện, semantic checker cần bắt lại.

**Nguồn gốc:** Spec v2 Mục 3 — mỗi `goal` chỉ có một loại vị từ duy nhất.

**Cách phát hiện:**
```
Với mỗi ElementSymbol có kind=GOAL:
    đếm số goalContract được gán
    nếu count > 1:
        → E5: DuplicateGoalContractType
```

**Ví dụ vi phạm:**
```
goal CapacityControl {
    maintain:
        self.classes->forAll(c | c.enrolledStudents->size() <= c.maxCapacity)
    avoid:                           // → E5
        self.classes->exists(c | c.enrolledStudents->size() = 0)
}
```

---

### 3.3 Nhóm USE Context Binding (E6–E7)

Hai lỗi này phụ thuộc vào việc bind context từ USE `MModel`:

- `self` được bind vào root class (ưu tiên `SystemState`, nếu không có thì lấy class đầu tiên).
- Kiểu trong iterator var (`s: Student`) cần resolve được vào type registry/USE model.

#### E6 — `InvalidSelfNavigation` 🆕

**Điều kiện:** Có path bắt đầu bằng `self.` nhưng một segment không tồn tại trên class hiện tại (không phải attribute, cũng không phải role/association end).

**Nguồn gốc:** Context codebase hiện tại (`OclUseValidator`) + USE model.

**Cách phát hiện (ý tưởng semantic):**
```
resolve self as rootClass
for each segment trong path self.a.b.c:
    nếu currentClass không có attribute/role tương ứng segment:
        → E6: InvalidSelfNavigation
    ngược lại chuyển currentClass sang type kế tiếp
```

**Ví dụ vi phạm:**
```
goal CheckVehiclePool {
    maintain:
        self.vehicles->forAll(v | v.status = "READY")
        // nếu root class không có thuộc tính/role "vehicles" -> E6
}
```

**Ví dụ hợp lệ:**
```
goal CapacityLimit {
    maintain:
        self.classes->forAll(c | c.enrolledStudents->size() <= c.maxCapacity)
        // classes tồn tại trên root class, các segment sau cũng resolve được -> OK
}
```

---

#### E7 — `UnknownIterVarType` 🆕

**Điều kiện:** Trong iterator var declaration, tên kiểu không resolve được (ví dụ không có class/enum tương ứng trong USE model hoặc type registry).

**Nguồn gốc:** GOAL.g4 `typedVar` cho phép `qualifiedName` bất kỳ; semantic cần bước resolve type.

**Cách phát hiện (ý tưởng semantic):**
```
for each iterator var (name: typeName):
    nếu resolve(typeName) thất bại trong type registry / useModel:
        → E7: UnknownIterVarType
```

**Ví dụ vi phạm:**
```
goal FulfillRegistrations {
    achieve for unique (s: StudentX, c: Class) in self.pendingRequests:
        c.enrolledStudents->includes(s)
        // StudentX không tồn tại trong USE model -> E7
}
```

**Ví dụ hợp lệ:**
```
goal FulfillRegistrations {
    achieve for unique (s: Student, c: Class) in self.pendingRequests:
        c.enrolledStudents->includes(s)
        // Student, Class đều resolve được -> OK
}
```

---

## 4. Bảng tổng hợp tất cả lỗi v1 + v2

### S-series (v1 — giữ nguyên)

| # | Tên lỗi | Điều kiện phát hiện | Nguồn gốc |
|---|---|---|---|
| S1 | `UndeclaredReference` | `resolvedTarget = null` sau Pass 2 | Spec v1 Mục 1.4 |
| S2 | `InvalidOperatorMatrix` | Cặp `(source.kind, target.kind, operator)` không có trong ma trận | Spec v1 Mục 1.4 Hình 1 |
| S3 | `DependencyOnNonLeaf` | `depender.isLeaf = false` hoặc `dependee.isLeaf = false` | Spec v1 Mục 1.5 |
| S4 | `InvalidActorRelationship` | Vi phạm ràng buộc kiểu trong bảng actor | Spec v1 Mục 1.6 |
| S5 | `DuplicateDeclaration` | Hai symbol cùng tên trong cùng scope | Spec v1 Mục 1.2, 1.3 |
| S6 | `SelfReference` | `resolvedTarget = sourceElement` | Spec v1 Mục 1.4 |
| S7 | `QualifySourceNotQuality` | Toán tử `=>` dùng với nguồn không phải `QUALITY` | Spec v1 Mục 1.4.3 |
| S8 | `NeededBySourceNotResource` | Toán tử `<>` dùng với nguồn không phải `RESOURCE` | Spec v1 Mục 1.4.4 |
| S9 | `CircularRefinement` | Chu trình trong đồ thị Refinement | Confirm ngoài spec |
| S10 | `MixedRefinementType` | Một đích nhận cả `AND_REFINE` lẫn `OR_REFINE` | Confirm ngoài spec |

### E-series (v2 — mới hoàn toàn)

| # | Tên lỗi | Nhóm | Điều kiện phát hiện | Nguồn gốc |
|---|---|---|---|---|
| E1 | `AchieveMissingIterVar` | Achieve Syntax | `type=ACHIEVE_UNIQUE` nhưng `iterVars` rỗng | Spec v2 Mục 3.3 |
| E2 | `AchieveMissingSource` | Achieve Syntax | `type=ACHIEVE_UNIQUE` nhưng `sourceExpr` null | Spec v2 Mục 3.3 |
| E3 | `InvalidCollectionChain` | OclExpr Validity | `->` chain sau `aggregateCall` trả về scalar/boolean | Spec v2 Mục 6.3 + grammar `aggregateCall` |
| E4 | `EmptyContractBody` | OclExpr Validity | Contract tồn tại nhưng body null | Ngầm định từ Spec v2 |
| E5 | `DuplicateGoalContractType` | Contract Validity | Một `goal` có nhiều hơn một `goalClause` | Spec v2 Mục 3 + grammar `goalBody` |
| E6 | `InvalidSelfNavigation` | OclExpr Validity | Path bắt đầu bằng `self.` có thuộc tính/role không tồn tại trong USE model | Context hiện tại từ codebase (`OclUseValidator`) |
| E7 | `UnknownIterVarType` | OclExpr Validity | Iterator var khai báo kiểu (`qualifiedName`) không resolve được trong type registry/USE model | Context codebase hiện tại + nhu cầu semantic v2 |

### Nguồn gốc E-series

| # | Tên lỗi | Nguồn gốc | Trích dẫn / Ghi chú |
|---|---|---|---|
| E1 | `AchieveMissingIterVar` | Spec v2 Mục 3.3 | Cú pháp `achieve for unique (s: Student, ...) in ...` — `typedVarList` là bắt buộc trong dạng đầy đủ |
| E2 | `AchieveMissingSource` | Spec v2 Mục 3.3 | Phần `in <expression>` là bắt buộc trong dạng đầy đủ |
| E3 | `InvalidCollectionChain` | Spec v2 Mục 6.3 + GOAL.g4 `aggregateCall` | `size()`, `max()`, `min()`, `isEmpty()`, `notEmpty()`, `includes()`, `excludes()`, `count()` trả về scalar/boolean — không thể chain `->` |
| E4 | `EmptyContractBody` | Ngầm định từ Spec v2 | Grammar bắt buộc `expression` sau keyword — check này là lưới an toàn semantic |
| E5 | `DuplicateGoalContractType` | Spec v2 Mục 3 + GOAL.g4 `goalBody` | Grammar giới hạn `goalClause?` — check này là lưới an toàn semantic nếu parser implementation lỏng lẻo |
| E6 | `InvalidSelfNavigation` | Context codebase hiện tại (`OclUseValidator`) + USE model | `self` bind vào root class của `MModel` (ưu tiên `SystemState`, nếu không có thì lấy class đầu tiên), rồi resolve từng segment của path |
| E7 | `UnknownIterVarType` | GOAL.g4 `typedVar` + context codebase hiện tại | Grammar cho phép `qualifiedName` bất kỳ, hiện chưa có semantic step resolve kiểu iterator var vào USE/type registry |

---

## 5. Thứ tự chạy checks

```
[Đã có từ v1 — giữ nguyên]
S1  → UndeclaredReference
S2  → InvalidOperatorMatrix
S3  → DependencyOnNonLeaf
S4  → InvalidActorRelationship
S5  → DuplicateDeclaration
S6  → SelfReference
S7  → QualifySourceNotQuality
S8  → NeededBySourceNotResource
S9  → CircularRefinement        (DFS trên đồ thị Refinement)
S10 → MixedRefinementType

[Mới trong v2 — chạy sau S1–S10]
E5  → DuplicateGoalContractType (duyệt elementTable, check trước khi đi vào cây OCL)
E1  → AchieveMissingIterVar     (lọc element có goalContract.type=ACHIEVE_UNIQUE)
E2  → AchieveMissingSource      (lọc element có goalContract.type=ACHIEVE_UNIQUE)
E4  → EmptyContractBody         (duyệt element có contract != null)
E3  → InvalidCollectionChain    (duyệt đệ quy cây OclExpr — chạy cuối vì tốn nhất)
```

---

## 6. Phần chưa confirm — Còn lại sau khi đọc grammar

Đọc grammar đã giải quyết được C1 (pre đứng một mình là hợp lệ) và C4 (or, implies có trong grammar). Các câu hỏi còn lại:

| # | Câu hỏi | Câu trả lời | Ảnh hưởng đến lỗi nào |
|---|---|---|---|
| C2 | `self` trong OCL trỏ đến đối tượng nào — tác nhân sở hữu, hay element đó, hay context object bên ngoài? | Trong codebase hiện tại, `self` trỏ tới root class của USE `MModel` (ưu tiên `SystemState`, nếu không có thì lấy class đầu tiên), không trỏ tới actor/goal/task của GOAL model | Validate navigation sau `self.` bằng `E6: InvalidSelfNavigation` |
| C3 | Tên kiểu trong `achieve for unique (s: Student, c: Class)` có được validate không? Grammar cho phép `qualifiedName` bất kỳ | Theo codebase hiện tại: chưa có semantic step validate/resolve kiểu iterator var; grammar chỉ check cú pháp | Nếu cần semantic typing → thêm `E7: UnknownIterVarType`, cần type registry mới trong symbol table |

---

*Tài liệu này được tạo phục vụ môn học Nguyên lý Ngôn ngữ Lập trình — ĐH Công nghệ, ĐHQGHN — 2025.*

---

## 7. Context bổ sung (theo codebase hiện tại)

### 7.1 Class input đến từ file `.use`

Trong flow hiện tại, semantic và OCL validation không tự sinh class từ file `.goal`. Thay vào đó:

- USE model (`MModel`) được lấy từ session (nguồn là file `.use` đã nạp trong USE).
- `GoalLoader` truyền `session.system().model()` vào compiler và view.
- Các check liên quan OCL navigation dựa trên `MModel` này để resolve class/attribute/association.

Hệ quả: mọi path kiểu `self.xxx.yyy` chỉ hợp lệ nếu các segment tồn tại trong mô hình class của USE.

### 7.2 `self` trong OCL là gì

Theo implementation hiện tại (`OclUseValidator`):

- `self` được bind vào một `rootClass` lấy từ `MModel`.
- Ưu tiên class tên `SystemState`.
- Nếu không có `SystemState`, fallback sang class đầu tiên trong `model.classes()`.
- Sau đó resolver đi từng segment của path (`self.a.b.c`) theo class hiện tại:
  - tìm attribute trước;
  - nếu không có thì tìm role/association end;
  - nếu vẫn không có thì báo unknown property.

Lưu ý: `self` không trỏ tới actor/goal/task trong GOAL model; nó là context object của USE model.

### 7.3 Các lỗi E-series có tính redundant (với parser chuẩn)

Nếu pipeline luôn đi theo đường chuẩn `ANTLR parser -> GoalAstBuilder -> semantic`, các lỗi sau chủ yếu là defensive checks:

- `E1: AchieveMissingIterVar` — redundant vì grammar bắt buộc `typedVarList` trong dạng `achieve for unique`.
- `E2: AchieveMissingSource` — redundant vì grammar bắt buộc phần `in <expression>` trong dạng `achieve for unique`.
- `E4: EmptyContractBody` — redundant vì grammar bắt buộc `expression` sau `achieve/maintain/avoid/pre/post`.
- `E5: DuplicateGoalContractType` — redundant vì `goalBody` chỉ cho tối đa một `goalClause?`.

Ngược lại, các lỗi sau không redundant và vẫn có giá trị semantic:

- `E3: InvalidCollectionChain` (type semantics của chain `->` không được grammar đảm bảo).
- `E6: InvalidSelfNavigation` (phụ thuộc resolve path trên USE model).
- `E7: UnknownIterVarType` (phụ thuộc type resolution, grammar không check được nghĩa kiểu).