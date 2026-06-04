# Kế hoạch Thực thi: Tối ưu API Order Search bằng BFF & RAM Caching

Đây là kịch bản (Workflow) thực thi nghiêm ngặt dành cho nghiệp vụ **Order Search**. Bắt buộc mọi AI Agent hoặc Developer khi thực thi phải làm đúng từng chữ trong tài liệu này. Không tự bịa thêm code, không chắp vá code cũ.

## 1. Yêu cầu Tiêu diệt Code Dư Thừa (Clean Up)
Hệ thống hiện tại đang bị rác vì chia quá nhiều hàm và gọi lồng ghép (VD: gọi Order xong vòng lặp gọi User gây N+1).
*   **Xóa bỏ triệt để:** Tìm và **DELETE HOÀN TOÀN** endpoint `/api/orders/my-orders` và các logic lân cận trong Controller và Service.
*   **Hợp nhất:** Tất cả đẩy về chung một luồng `searchOrders` (`/api/orders/search`). API này sẽ tự động kiểm tra role (Admin thì xem hết, Customer thì ép thêm điều kiện `customerId = current_user_id`).

## 2. Định nghĩa Nghiệp vụ Trộn Dữ Liệu (Data Aggregation Logic)
Thay vì dùng `OrderMapper` sinh N+1 query, ta áp dụng BFF Pattern để trộn:

### 2.1. Dữ liệu Động (Dynamic Data - DB Query)
Luồng chính sẽ query DB lấy "Bộ khung" đơn hàng:
*   `orderId`, `orderNumber`, `status` (Trạng thái đơn hàng - thay đổi liên tục).
*   `totalAmount` (Giá trị thanh toán hiện hành).
*   Các ID ngoại lai: `productId`, `customerId`.

### 2.2. Dữ liệu Tĩnh (Static Data - Caffeine RAM Cache)
Sử dụng Virtual Threads móc siêu tốc các dữ liệu này từ RAM:
*   **User Cache:** `fullName`, `deliveryAddress`.
*   **Product Cache:** `productName`, `category`, `thumbnailUrl`.

### 2.3. Nghiệp vụ Tải Lười (Lazy Loading)
Tầng BFF phải nhận biết tham số để tối ưu Data trả về:
*   **Khi Search Danh sách (List View):** Không móc Data Địa chỉ giao hàng chi tiết, Không móc Lịch sử thay đổi trạng thái. Chỉ gộp Tên Sản Phẩm và Ảnh Thumbnail.
*   **Khi Xem Chi Tiết (Lazy Load - Detail View):** Nếu request có cờ báo (hoặc gọi API `/search/{id}`), BFF kích hoạt thêm luồng Virtual Thread chạy móc Full Địa Chỉ, SĐT Shipper và Danh sách Lịch sử thao tác.

## 3. Các bước Viết Code (Execution Steps)
1.  **Sửa Entity/Mapper:** Cắt đứt các liên kết quan hệ lồng nhau (Hibernate Lazy/Eager) tự động kích hoạt. Mapper giờ chỉ map DTO đơn giản (Chỉ có ID).
2.  **Sửa Service:** Triển khai `virtualExecutor = Executors.newVirtualThreadPerTaskExecutor()`.
    *   Thread 1: `orderRepository.search(criteria)`
    *   Thread 2: Quét `ProductCache` lấy danh sách Ảnh và Tên.
    *   Thread 3: Quét `UserCache` lấy danh sách Tên Khách hàng.
    *   Trộn chúng lại trong Java code.
3.  **Tích hợp Resilience4j:** Gắn `@CircuitBreaker` vào đoạn check Tồn kho (Nếu áp dụng). Nếu sập, trả về Tồn kho = `null` thay vì báo lỗi.
