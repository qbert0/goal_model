# Đặc tả Symbol Table — Ngôn ngữ GOAL

> Tài liệu tham khảo nội bộ cho giai đoạn Semantic Analysis của compiler GOAL.

---

## 1. Tổng quan kiến trúc

Symbol Table của GOAL có **2 tầng lồng nhau**, phản ánh đúng cấu trúc phạm vi (scope) của ngôn ngữ:

```
GlobalScope
├── modelName: String
├── ActorTable:      { name → ActorSymbol }
└── DependencyTable: { name → DependencySymbol }
```

Mỗi `ActorSymbol` lại chứa một bảng con:

```
ActorSymbol
└── ElementTable: { name → ElementSymbol }
```

---

## 2. Các loại Symbol

### 2.1 `ActorSymbol`

Đại diện cho khai báo `actor`, `agent`, hoặc `role`.

| Trường | Kiểu | Mô tả |
|---|---|---|
| `name` | `String` | Tên định danh của tác nhân |
| `kind` | `ACTOR \| AGENT \| ROLE` | Phân loại tác nhân |
| `parent` | `ActorSymbol?` | Tác nhân cha trong quan hệ `is-a` (`:`) |
| `participatesIn` | `ActorSymbol?` | Tác nhân được tham gia trong quan hệ `participates-in` (`>`) |
| `elementTable` | `Map<String, ElementSymbol>` | Bảng các phần tử chủ đích bên trong |
| `sourceLocation` | `Location` | Vị trí trong mã nguồn (dòng, cột) |

**Ràng buộc quan hệ tổ chức:**

| Quan hệ | Ký hiệu | Kiểu nguồn hợp lệ | Kiểu đích hợp lệ |
|---|---|---|---|
| is-a | `:` | `ACTOR` | `ACTOR` |
| is-a | `:` | `ROLE` | `ROLE` |
| participates-in | `>` | `AGENT` | `ROLE` |
| participates-in | `>` | `AGENT` | `AGENT` |

---

### 2.2 `ElementSymbol`

Đại diện cho `goal`, `task`, `quality`, hoặc `resource` bên trong một tác nhân.

| Trường | Kiểu | Mô tả |
|---|---|---|
| `name` | `String` | Tên định danh phần tử |
| `kind` | `GOAL \| TASK \| QUALITY \| RESOURCE` | Phân loại phần tử |
| `ownerActor` | `ActorSymbol` | Tác nhân sở hữu phần tử này |
| `description` | `String?` | Nội dung thuộc tính `description = "..."` nếu có |
| `isLeaf` | `Boolean` | `true` nếu phần tử chưa bị phân rã (refined) — xem mục 4 |
| `relations` | `List<RelationEntry>` | Danh sách các quan hệ xuất phát từ phần tử này |
| `goalContract` | `GoalContract?` | Hợp đồng mục tiêu OCL (`maintain` / `avoid` / `achieve`) |
| `taskContract` | `TaskContract?` | Hợp đồng tác vụ OCL (`pre` / `post`) |
| `sourceLocation` | `Location` | Vị trí trong mã nguồn |

---

### 2.3 `RelationEntry`

Một mục quan hệ gắn vào `ElementSymbol` nguồn. Mỗi phần tử có thể có nhiều `RelationEntry` (phân tách bằng dấu `,` trong mã nguồn).

| Trường | Kiểu | Mô tả |
|---|---|---|
| `operator` | `OperatorKind` | Loại toán tử quan hệ |
| `targetRef` | `String` | Tên đích thô từ AST, ví dụ `"FastDelivery"` hoặc `"DeliverySystem.AssignDriver"` |
| `resolvedTarget` | `ElementSymbol?` | Điền sau Pass 2 — kết quả tra cứu symbol |
| `sourceLocation` | `Location` | Vị trí trong mã nguồn |

**Enum `OperatorKind`:**

| Giá trị | Ký hiệu trong code | Nhóm |
|---|---|---|
| `AND_REFINE` | `&>` | Refinement |
| `OR_REFINE` | `\|>` | Refinement |
| `MAKE` | `++>` | Contribution |
| `HELP` | `+>` | Contribution |
| `HURT` | `->` | Contribution |
| `BREAK` | `-->` | Contribution |
| `QUALIFY` | `=>` | Qualification |
| `NEEDED_BY` | `<>` | NeededBy |

---

### 2.4 `DependencySymbol`

Đại diện cho một khối `dependency` khai báo ở cấp global.

| Trường | Kiểu | Mô tả |
|---|---|---|
| `name` | `String` | Tên định danh của dependency |
| `depender` | `ElementSymbol` | Phần tử đang cần sự trợ giúp (đã resolve) |
| `dependee` | `ElementSymbol` | Phần tử cung cấp sự trợ giúp (đã resolve) |
| `dependumTable` | `Map<String, ElementSymbol>` | Bảng các phần tử trung gian nội tuyến — scope riêng của dependency |
| `sourceLocation` | `Location` | Vị trí trong mã nguồn |

**Scope của dependum:** Các element khai báo bên trong khối `dependency` nằm trong **dependency scope** riêng biệt, tách khỏi `ActorTable` và `GlobalScope`. Một dependency có thể chứa nhiều element. Tên element trong dependency không xung đột với tên element trong actor khác.

**Ràng buộc:** `depender` và `dependee` phải là **leaf element** (`isLeaf = true`). Xem mục 4.

---

### 2.5 `GoalContract`

Phần mở rộng OCL gắn vào `ElementSymbol` có `kind = GOAL`.

| Trường | Kiểu | Mô tả |
|---|---|---|
| `type` | `MAINTAIN \| AVOID \| ACHIEVE` | Loại vị từ mục tiêu |
| `variable` | `String?` | Biến lặp trong `achieve for unique (s: T, ...)` |
| `expression` | `OclExpr` | Cây biểu thức OCL |

---

### 2.6 `TaskContract`

Phần mở rộng OCL gắn vào `ElementSymbol` có `kind = TASK`.

| Trường | Kiểu | Mô tả |
|---|---|---|
| `precondition` | `OclExpr?` | Biểu thức điều kiện tiền quyết (`pre:`) |
| `postcondition` | `OclExpr?` | Biểu thức điều kiện hậu quyết (`post:`) |

---

## 3. Ma trận hợp lệ Nguồn–Đích (Operator Validation Matrix)

Dùng để kiểm tra semantic sau khi resolve. Ô `✓` = hợp lệ, ô `✗` = lỗi semantic.

| **Nguồn \ Đích** | `GOAL` | `QUALITY` | `TASK` | `RESOURCE` |
|---|---|---|---|---|
| `GOAL` | `&>` `\|>` (Refinement) | `++>` `+>` `->` `-->` (Contribution) | `&>` `\|>` (Refinement) | ✗ |
| `QUALITY` | `=>` (Qualification) | `++>` `+>` `->` `-->` (Contribution) | `=>` (Qualification) | `=>` (Qualification) |
| `TASK` | `&>` `\|>` (Refinement) | `++>` `+>` `->` `-->` (Contribution) | `&>` `\|>` (Refinement) | ✗ |
| `RESOURCE` | ✗ | `++>` `+>` `->` `-->` (Contribution) | `<>` (NeededBy) | ✗ |

---

## 4. Quy tắc xác định `isLeaf`

`isLeaf` được tính trong **Pass 1**, dựa vào việc phần tử có xuất hiện ở **phía đích** của quan hệ Refinement hay không.

```
Với mỗi RelationEntry (op=AND_REFINE hoặc op=OR_REFINE):
    target.isLeaf = false
    
Mặc định ban đầu: tất cả ElementSymbol.isLeaf = true
```

**Ví dụ:**

```
task AssignDriver &> DeliverPackage { }   // → DeliverPackage.isLeaf = false
task VerifyDriverLicense &> AssignDriver { } // → AssignDriver.isLeaf = false
resource DriverDatabase <> AssignDriver { }  // AssignDriver đích của <>, không ảnh hưởng isLeaf
```

Kết quả:
- `DeliverPackage` → `isLeaf = false`
- `AssignDriver` → `isLeaf = false`
- `VerifyDriverLicense` → `isLeaf = true`
- `DriverDatabase` → `isLeaf = true`

---

## 5. Quy trình xây dựng (2 Pass)

### Pass 1 — Declaration Pass

Duyệt toàn bộ AST, **chỉ tạo symbol, chưa resolve tham chiếu**.

```
1. Tạo GlobalScope
2. Với mỗi actor/agent/role declaration:
   a. Tạo ActorSymbol, thêm vào ActorTable
   b. Với mỗi element bên trong:
      - Tạo ElementSymbol (isLeaf = true mặc định)
      - Thêm vào elementTable của actor
      - Lưu relations thô (targetRef là string)
3. Với mỗi dependency declaration:
   - Tạo DependencySymbol (depender/dependee/dependum chưa resolve)
   - Thêm vào DependencyTable
4. Cập nhật isLeaf:
   - Duyệt lại tất cả relations có op=AND_REFINE hoặc OR_REFINE
   - Đánh dấu target.isLeaf = false (tra cứu trong cùng actor scope)
```

### Pass 2 — Resolution Pass

Sau Pass 1, toàn bộ symbol đã tồn tại. Tiến hành resolve tham chiếu.

```
1. Với mỗi RelationEntry trong toàn bộ ElementSymbol:
   a. Parse targetRef:
      - Nếu dạng "ElementName": tìm trong elementTable của actor hiện tại
      - Nếu dạng "ActorName.ElementName": tìm actor trước, rồi element trong actor đó
   b. Gán resolvedTarget
   c. Nếu không tìm thấy → ERROR: Undeclared reference

2. Với mỗi DependencySymbol:
   a. Resolve depender (dạng "ActorName.ElementName")
   b. Resolve dependee (dạng "ActorName.ElementName")
   c. Kiểm tra isLeaf của depender và dependee → nếu false → ERROR

3. Với actor relationships (is-a, participates-in):
   a. Resolve tên tác nhân đích → ActorSymbol
   b. Kiểm tra ràng buộc kiểu (xem bảng mục 2.1)
```

---

## 6. Danh sách Semantic Checks

Sau khi hoàn thành 2 pass, thực hiện các kiểm tra sau:

| # | Tên lỗi | Điều kiện phát hiện |
|---|---|---|
| S1 | `UndeclaredReference` | `resolvedTarget = null` sau Pass 2 |
| S2 | `InvalidOperatorMatrix` | Cặp `(source.kind, target.kind, operator)` không có trong ma trận mục 3 |
| S3 | `DependencyOnNonLeaf` | `depender.isLeaf = false` hoặc `dependee.isLeaf = false` |
| S4 | `InvalidActorRelationship` | Vi phạm ràng buộc kiểu trong bảng mục 2.1 |
| S5 | `DuplicateDeclaration` | Hai symbol cùng tên trong cùng scope — scope được xét riêng biệt: `ActorTable` (global), `ElementTable` (trong từng actor), `DependencyTable` (global), `dependumTable` (trong từng dependency) |
| S6 | `SelfReference` | `resolvedTarget = sourceElement` (phần tử tự quan hệ với chính nó) |
| S7 | `QualifySourceNotQuality` | Toán tử `=>` dùng với nguồn không phải `QUALITY` |
| S8 | `NeededBySourceNotResource` | Toán tử `<>` dùng với nguồn không phải `RESOURCE` |
| S9 | `CircularRefinement` | Tồn tại chu trình trong đồ thị Refinement, ví dụ `A &> B` và `B &> A` |
| S10 | `MixedRefinementType` | Một element đích nhận đồng thời cả `AND_REFINE` (`&>`) và `OR_REFINE` (`\|>`) từ các element khác nhau |

### Nguồn gốc các lỗi (Traceability)

| # | Tên lỗi | Nguồn gốc | Trích dẫn / Ghi chú |
|---|---|---|---|
| S1 | `UndeclaredReference` | Spec v1.0 — Mục 1.4 | Ma trận liên kết yêu cầu Nguồn và Đích phải là các phần tử tồn tại trong mô hình |
| S2 | `InvalidOperatorMatrix` | Spec v1.0 — Mục 1.4, Hình 1 | Ô `n/a` trong ma trận liên kết giữa các thành phần chủ đích là tổ hợp bị cấm |
| S3 | `DependencyOnNonLeaf` | Spec v1.0 — Mục 1.5 | *"Mọi mối quan hệ phụ thuộc phải được thiết lập trên các phần tử lá — tức là các phần tử ở mức chi tiết nhất chưa qua tinh chỉnh"* |
| S4 | `InvalidActorRelationship` | Spec v1.0 — Mục 1.6 | Bảng ràng buộc kiểu: `is-a` chỉ actor→actor hoặc role→role; `participates-in` chỉ agent→role hoặc agent→agent |
| S5 | `DuplicateDeclaration` | Spec v1.0 — Mục 1.2, 1.3 | Mỗi tác nhân và phần tử mang tên định danh duy nhất; ngầm định từ cơ chế tra cứu theo tên trong symbol table |
| S6 | `SelfReference` | Spec v1.0 — Mục 1.4 | Ma trận định nghĩa quan hệ giữa các phần tử khác nhau; không có trường hợp phần tử tự trỏ vào chính nó |
| S7 | `QualifySourceNotQuality` | Spec v1.0 — Mục 1.4.3 | *"Qualification: dùng để gán một thuộc tính chất lượng làm tiêu chuẩn"* — nguồn bắt buộc là `quality` |
| S8 | `NeededBySourceNotResource` | Spec v1.0 — Mục 1.4.4 | *"NeededBy: một tài nguyên ở Nguồn được cung cấp cho một tác vụ ở Đích"* — nguồn bắt buộc là `resource`, đích bắt buộc là `task` |
| S9 | `CircularRefinement` | Confirm ngoài spec — 2025 | Spec không đề cập; xác nhận thủ công: chu trình trong Refinement là vô nghĩa về mặt ngữ nghĩa và bị cấm |
| S10 | `MixedRefinementType` | Confirm ngoài spec — 2025 | Spec không đề cập; xác nhận thủ công: một element đích không thể vừa nhận `AND_REFINE` vừa nhận `OR_REFINE` |

---

**Ví dụ S9 — CircularRefinement:**

```
task A &> B { }
task B &> A { }   // → lỗi: A → B → A tạo thành chu trình
```

Phát hiện bằng thuật toán DFS trên đồ thị Refinement sau Pass 2. Nếu tồn tại back edge → báo lỗi S9.

**Ví dụ S10 — MixedRefinementType:**

```
task X &> C { }   // C nhận AND-refinement từ X
task Y |> C { }   // C nhận OR-refinement từ Y → lỗi: không thể trộn lẫn
```

Phát hiện bằng cách theo dõi `refinementTypeOf[target]`: lần đầu ghi nhận kiểu (`AND` hoặc `OR`), lần sau nếu khác kiểu → báo lỗi S10.

### Mã nguồn

```
istar SmartDelivery

agent DeliverySystem {
    goal DeliverPackage { description = "Hoan thanh viec giao hang" }
    quality HighReliability { }
    quality FastDelivery +> HighReliability { }
    task ProcessOrder &> DeliverPackage { }
    task AssignDriver &> DeliverPackage, ++> FastDelivery { }
    task VerifyDriverLicense &> AssignDriver { }
    resource DriverDatabase <> AssignDriver { }
    quality SystemSecurity => DriverDatabase { }
}
```

### Symbol Table kết quả

```
GlobalScope
├── modelName: "SmartDelivery"
└── ActorTable:
    └── "DeliverySystem": ActorSymbol {
            kind: AGENT,
            elementTable: {
                "DeliverPackage": ElementSymbol {
                    kind: GOAL,
                    isLeaf: false,          ← bị AssignDriver và ProcessOrder trỏ vào
                    description: "Hoan thanh viec giao hang",
                    relations: []
                },
                "HighReliability": ElementSymbol {
                    kind: QUALITY,
                    isLeaf: true,
                    relations: []
                },
                "FastDelivery": ElementSymbol {
                    kind: QUALITY,
                    isLeaf: true,
                    relations: [
                        { op: HELP, targetRef: "HighReliability",
                          resolvedTarget: → HighReliability }
                    ]
                },
                "ProcessOrder": ElementSymbol {
                    kind: TASK,
                    isLeaf: true,
                    relations: [
                        { op: AND_REFINE, targetRef: "DeliverPackage",
                          resolvedTarget: → DeliverPackage }
                    ]
                },
                "AssignDriver": ElementSymbol {
                    kind: TASK,
                    isLeaf: false,          ← bị VerifyDriverLicense trỏ vào
                    relations: [
                        { op: AND_REFINE, targetRef: "DeliverPackage",
                          resolvedTarget: → DeliverPackage },
                        { op: MAKE,       targetRef: "FastDelivery",
                          resolvedTarget: → FastDelivery }
                    ]
                },
                "VerifyDriverLicense": ElementSymbol {
                    kind: TASK,
                    isLeaf: true,
                    relations: [
                        { op: AND_REFINE, targetRef: "AssignDriver",
                          resolvedTarget: → AssignDriver }
                    ]
                },
                "DriverDatabase": ElementSymbol {
                    kind: RESOURCE,
                    isLeaf: true,
                    relations: [
                        { op: NEEDED_BY, targetRef: "AssignDriver",
                          resolvedTarget: → AssignDriver }
                    ]
                },
                "SystemSecurity": ElementSymbol {
                    kind: QUALITY,
                    isLeaf: true,
                    relations: [
                        { op: QUALIFY, targetRef: "DriverDatabase",
                          resolvedTarget: → DriverDatabase }
                    ]
                }
            }
        }
```

---

*Tài liệu này được tạo phục vụ môn học Nguyên lý Ngôn ngữ Lập trình — ĐH Công nghệ, ĐHQGHN — 2025.*