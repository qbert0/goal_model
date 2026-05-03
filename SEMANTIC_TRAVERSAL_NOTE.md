# Ghi chú: Duyệt cây vs Flatten trong GOAL v1.0

## Kết luận nhanh

- Hiện tại semantic pipeline **không duyệt cây khai báo theo recursive visitor tổng quát**.
- Pipeline đang chạy theo các pass trên symbol table/list đã thu thập từ AST (cách làm "phẳng theo scope").
- Với **GOAL v1.0 (chưa có OCL)**, cách này là **chấp nhận được và hợp lý**.

## Vì sao hiện tại vẫn ổn

- Cấu trúc AST hiện tại có độ sâu cố định:
  - `GoalModel -> ActorDecl -> IntentionalElement`
  - `Dependency` ở top-level, có `dependumElement` đơn.
- Semantic check chính (S1..S10) chỉ cần tra cứu theo scope actor/global và đồ thị refinement.
- Đệ quy hiện có tập trung ở kiểm tra chu trình refinement (S9, DFS trên graph), không phải đệ quy cây khai báo.

## Khi nào cần chuyển sang recursive traversal đầy đủ

Nên refactor sang visitor đệ quy khi mở rộng ngôn ngữ theo hướng:

- Có nested declaration nhiều tầng (scope lồng nhau thực sự).
- Mở rộng OCL cần walk expression tree sâu và context-sensitive.
- Dependency scope chứa nhiều phần tử nội tuyến (dependum table đúng nghĩa).

## Lưu ý đồng bộ spec và code

- `SYMBOL.md` mô tả dependency có `dependumTable` như một scope riêng.
- Code hiện tại mới giữ một `dependum` trong `DependencySymbol`, chưa thành table/scope đầy đủ.
- Nếu roadmap có nested trong dependency, đây là điểm cần nâng cấp sớm.

## Đề xuất thực dụng cho phiên bản hiện tại

- Giữ kiến trúc pass hiện tại cho v1.0 để đảm bảo tiến độ.
- Khi bắt đầu thêm OCL/nested scope, tạo các hàm visitor rõ ràng:
  - `visitModel`
  - `visitActor`
  - `visitElement`
  - `visitDependency`
  - `visitExpression` (cho OCL)

