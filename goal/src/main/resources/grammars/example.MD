# Ví dụ Expression trong Ngôn ngữ GOAL

## Mục lục
1. [MAINTAIN – Điều kiện an toàn](#1-maintain)
2. [AVOID – Trạng thái cấm](#2-avoid)
3. [ACHIEVE – Trạng thái đích](#3-achieve)
4. [PRE / POST – Hợp đồng tác vụ](#4-pre--post)
5. [Collection Operations – forAll, exists, collect](#5-collection-operations)
6. [Basic Operators – So sánh, số học, hàm thống kê](#6-basic-operators)

---

## 1. MAINTAIN

Chỉ định các điều kiện an toàn (**safety properties**) phải **luôn đúng** trong suốt quá trình hệ thống vận hành.

### 1a – `forAll` cơ bản
> Mọi lớp học đều không được vượt quá sức chứa tối đa.

```goal
goal CapacityLimit {
    maintain:
        self.classes->forAll(c |
            c.enrolledStudents->size() <= c.maxCapacity
        )
}
```

### 1b – `forAll` với toán tử số học
> Mọi giảng viên dạy không quá 3 lớp mỗi kỳ.

```goal
goal TeacherWorkload {
    maintain:
        self.teachers->forAll(t |
            t.assignedClasses->size() <= 3
        )
}
```

### 1c – `forAll` kết hợp so sánh chuỗi và `or`
> Mọi sinh viên đã đăng ký đều phải có trạng thái hợp lệ.

```goal
goal ValidEnrollmentStatus {
    maintain:
        self.students->forAll(s |
            s.status = "ACTIVE" or s.status = "PROBATION"
        )
}
```

---

## 2. AVOID

Chỉ định các trạng thái cấm (**forbidden states**) tuyệt đối không bao giờ được phép xảy ra. Nếu expression trả về `true` thì hệ thống **vi phạm**.

### 2a – `exists` lồng nhau (phát hiện xung đột lịch học)
> Không có sinh viên nào đăng ký hai lớp trùng khung giờ.

```goal
goal NoScheduleConflict {
    avoid:
        self.students->exists(s |
            s.enrolledClasses->exists(c1 |
                s.enrolledClasses->exists(c2 |
                    c1 <> c2 and c1.timeSlot = c2.timeSlot
                )
            )
        )
}
```

### 2b – `exists` kết hợp so sánh số học
> Không có phòng nào bị đặt vượt quá số ghế cho phép.

```goal
goal NoOverbooking {
    avoid:
        self.rooms->exists(r |
            r.bookedSeats > r.totalSeats
        )
}
```

### 2c – `not` + `exists`
> Không tồn tại lớp học không có giảng viên đang hoạt động.

```goal
goal NoOrphanClass {
    avoid:
        self.classes->exists(c |
            not c.assignedTeachers->exists(t | t.active = true)
        )
}
```

---

## 3. ACHIEVE

Chỉ định các trạng thái đích (**liveness properties**) mà hệ thống cuối cùng phải đạt được.

Có **hai dạng cú pháp**:

| Dạng | Cú pháp |
|------|---------|
| Đơn giản | `achieve: <expression>` |
| Cho từng cặp duy nhất | `achieve for unique (<vars>) in <collection>: <expression>` |

### 3a – `achieve` đơn giản
> Tất cả yêu cầu đăng ký đều phải được xử lý.

```goal
goal NoPendingRequests {
    achieve:
        self.pendingRequests->isEmpty()
}
```

### 3b – `achieve for unique` (cú pháp mục 3.3 trong spec)
> Với mỗi cặp `(sinh viên, lớp)` trong danh sách chờ: sinh viên phải được ghi danh **và** hóa đơn phải được tạo.

```goal
goal FulfillRegistrations {
    achieve for unique (s: Student, c: Class) in self.pendingRequests:
        c.enrolledStudents->includes(s) and
        s.invoices->exists(i |
            i.relatedClass = c and i.status = "ISSUED"
        )
}
```

### 3c – `achieve for unique` với điều kiện phức hợp
> Mỗi đơn đăng ký được duyệt phải có biên lai và email xác nhận.

```goal
goal CompleteApprovedOrders {
    achieve for unique (o: Order) in self.approvedOrders:
        o.receipt->exists(r | r.amount >= o.totalFee) and
        o.confirmationSent = true
}
```

---

## 4. PRE / POST

Định nghĩa **hợp đồng tác vụ** thông qua điều kiện tiền quyết và hậu quyết.

- **`pre`**: Điều kiện phải đúng **trước** khi tác vụ bắt đầu.
- **`post`**: Điều kiện phải đúng **sau** khi tác vụ hoàn thành.
- **`@pre`**: Tham chiếu đến giá trị của một thuộc tính **trước** khi tác vụ chạy — dùng trong `post` để so sánh trạng thái trước/sau.

### 4a – Pre + Post cơ bản

```goal
task EnrollStudent {
    pre:
        self.pendingRequests->exists(r |
            r.student = s and r.class = c
        )
    post:
        not self.pendingRequests->exists(r |
            r.student = s and r.class = c
        ) and
        c.enrolledStudents->includes(s)
}
```

### 4b – Pre kiểm tra giới hạn sức chứa

```goal
task AddStudentToClass {
    pre:
        c.enrolledStudents->size() < c.maxCapacity and
        not c.enrolledStudents->includes(s)
    post:
        c.enrolledStudents->includes(s) and
        c.enrolledStudents->size() = c.enrolledStudents@pre->size() + 1
}
```

> `c.enrolledStudents@pre->size()` — truy cập tập `enrolledStudents` tại thời điểm **trước** khi tác vụ chạy, rồi gọi `size()` trên đó.

### 4c – Post dùng `@pre` và phép trừ

```goal
task UnenrollStudent {
    pre:
        c.enrolledStudents->includes(s)
    post:
        not c.enrolledStudents->includes(s) and
        c.enrolledStudents->size() = c.enrolledStudents@pre->size() - 1
}
```

---

## 5. Collection Operations

Các phép toán trên tập hợp theo phong cách OCL, sử dụng ký hiệu `->`.

| Phép toán | Cú pháp | Ý nghĩa |
|-----------|---------|---------|
| `forAll` | `col->forAll(x \| cond)` | Mọi phần tử đều thỏa mãn `cond` |
| `exists` | `col->exists(x \| cond)` | Ít nhất một phần tử thỏa mãn `cond` |
| `collect` | `col->collect(x \| expr)` | Tạo tập hợp mới từ kết quả `expr` |
| `size` | `col->size()` | Số phần tử trong tập hợp |
| `count` | `col->count(x \| cond)` | Đếm phần tử thỏa mãn `cond` |
| `max` | `col->max()` | Giá trị lớn nhất |
| `min` | `col->min()` | Giá trị nhỏ nhất |
| `includes` | `col->includes(e)` | Tập hợp có chứa phần tử `e` |
| `excludes` | `col->excludes(e)` | Tập hợp không chứa phần tử `e` |
| `isEmpty` | `col->isEmpty()` | Tập hợp rỗng |
| `notEmpty` | `col->notEmpty()` | Tập hợp không rỗng |

### 5a – `collect` + `max()` — chuỗi (chaining)
> Giờ đóng cửa hệ thống phải ≥ giờ mở cửa lớp muộn nhất.

```goal
goal SystemAvailability {
    maintain:
        self.systemCloseTime >= self.doors->collect(d | d.openingTime)->max()
}
```

### 5b – `collect` + `min()`
> Ngân sách phân bổ phải đủ để trang trải chi phí thấp nhất.

```goal
goal BudgetSufficiency {
    maintain:
        self.allocatedBudget >= self.projects->collect(p | p.estimatedCost)->min()
}
```

### 5c – `size()` kết hợp `implies`
> Sinh viên hoàn thành ≥ 30 môn thì được đánh dấu đủ điều kiện tốt nghiệp.

```goal
goal GraduationEligibility {
    maintain:
        self.students->forAll(s |
            s.completedCourses->size() >= 30
                implies s.graduationEligible = true
        )
}
```

### 5d – `count()` với lambda
> Số sinh viên GPA < 5.0 không được vượt 10% tổng số sinh viên của lớp.

```goal
goal AcademicQualityControl {
    maintain:
        self.classes->forAll(c |
            c.enrolledStudents->count(s | s.gpa < 5.0) <=
            c.enrolledStudents->size() / 10
        )
}
```

---

## 6. Basic Operators

### Bảng tổng hợp toán tử

| Nhóm | Toán tử | Ví dụ |
|------|---------|-------|
| So sánh | `=` `>=` `<=` `>` `<` `<>` | `s.score >= 0` |
| Số học | `+` `-` `*` `/` | `s.credits + course.credits` |
| Logic | `and` `or` `not` `implies` | `a = 1 and b = 2` |

### 6a – Toán tử so sánh: `=`, `>=`, `<=`

```goal
task ValidateScore {
    pre:
        s.score >= 0 and s.score <= 100
    post:
        s.grade = "PASS"
}
```

### 6b – Toán tử số học: `+` trong pre và post

```goal
task RegisterCourse {
    pre:
        not s.enrolledCourses->includes(course) and
        s.currentCredits + course.credits <= s.maxCredits
    post:
        s.enrolledCourses->includes(course) and
        s.currentCredits = s.currentCredits@pre + course.credits
}
```

### 6c – `includes` / `excludes`
> Sau khi thu hồi quyền truy cập: tài khoản bị loại khỏi danh sách hoạt động và thêm vào danh sách bị chặn.

```goal
task RevokeAccess {
    pre:
        self.activeAccounts->includes(account)
    post:
        self.activeAccounts->excludes(account) and
        self.blockedAccounts->includes(account)
}
```

### 6d – `isEmpty` / `notEmpty`
> Chỉ gửi thông báo khi danh sách người nhận không rỗng.

```goal
task SendNotification {
    pre:
        self.recipients->notEmpty()
    post:
        self.sentLog->includes(notification)
}
```

---

## Tổng hợp cấu trúc cú pháp

```
// Goal types
goal <Name> {
    maintain: <expression>
    avoid:    <expression>
    achieve:  <expression>
    achieve for unique (<var>: <Type>, ...) in <expr>: <expression>
}

// Task contract
task <Name> {
    pre:  <expression>
    post: <expression>
}

// Collection navigation
<expr>-><op>(<iterVar> | <expr>)   // forAll, exists, collect, count
<expr>-><op>()                      // size, max, min, isEmpty, notEmpty
<expr>-><op>(<expr>)                // includes, excludes

// @pre – tham chiếu trạng thái trước khi tác vụ chạy
<attr>@pre
<collection>@pre-><op>(...)
```