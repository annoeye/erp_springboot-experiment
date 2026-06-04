# Kịch bản Kiểm thử & Yêu cầu Nghiệp vụ (Test Plan for BFF & Lazy Loading)

Tài liệu này không chỉ chứa các lệnh CURL để test, mà nó là **Bộ Yêu Cầu Nghiệp Vụ (Business Logic Requirements)** ép buộc hệ thống phải thỏa mãn khi hoạt động. Bất kỳ AI/Developer nào thực thi xong code đều phải đảm bảo các Test Case dưới đây vượt qua.

## 1. Nghiệp Vụ Kiểm Thử Đặc Thù (BFF Business Logic)

Luồng kiểm thử tập trung đánh giá 3 năng lực cốt lõi sau của hệ thống vừa được thiết kế lại:

*   **Năng lực Trộn Data (Data Enrichment):** Đảm bảo Data trả về không còn là những chuỗi ID trống rỗng, mà phải được "đắp thịt" nhờ trộn Dữ liệu Động (từ DB) và Dữ liệu Tĩnh (từ RAM Cache).
*   **Nghiệp vụ Tải Lười (Lazy Loading vs Eager Loading):** Đánh giá tính toán học của API. Khi search mảng rộng (List), hệ thống trả payload nhỏ gọn (chỉ kèm Thumbnail). Chỉ khi xem 1 đơn hàng cụ thể, luồng Tải lười mới được kích hoạt để bốc toàn bộ Dữ liệu Tĩnh siêu nặng (Địa chỉ chi tiết, Mô tả sản phẩm).
*   **Sức bền (Fault Tolerance):** Khi kho dữ liệu Động (ví dụ hệ thống tính toán tồn kho) đứt gãy, Frontend vẫn hiển thị được lịch sử giao dịch (Nhờ data tĩnh ở Cache cứu nguy).

---

## 2. Kịch bản Gọi (Test Scenarios)

### Bước 1: Login lấy Token (Sử dụng Payload Thực)
Yêu cầu bắt buộc: Dùng Terminal thực hiện call API lấy Bearer Token của Admin.
```bash
curl -X POST http://localhost:8080/api/auth/login \
-H "Content-Type: application/json" \
-d '{
  "usernameOrEmail": "ADMIN",
  "password": "admin",
  "deviceInfo": {
    "deviceType": "Desktop",
    "osName": "Windows",
    "browserName": "Chrome"
  }
}'
```

### Bước 2: Test Nghiệp vụ Trộn Dữ Liệu Tĩnh + Động (Eager Loading cho List)
Kiểm tra API Search đã được gộp luồng. Chú ý Response: Không có `deliveryAddress` chi tiết (tránh phình Data).
```bash
curl -X POST http://localhost:8080/api/orders/search \
-H "Authorization: Bearer <TOKEN_Ở_BƯỚC_1>" \
-H "Content-Type: application/json" \
-d '{ "page": 0, "size": 10, "sortBy": "createdAt" }'
```
**Expected Business Response (Nghiệp vụ mong đợi):**
```json
{
  "orderId": 1001,
  "status": "PENDING", // <--- DATA ĐỘNG (Lấy từ DB Core)
  "totalAmount": 150000, 
  "customerName": "Nguyễn Văn A", // <--- DATA TĨNH (RAM Cache - Ép vào bằng Virtual Thread)
  "items": [
    {
      "productName": "Áo thun", // <--- DATA TĨNH (RAM Cache)
      "thumbnailUrl": "http://img.com/a.jpg", // <--- DATA TĨNH (RAM Cache)
      "quantity": 2
    }
  ]
}
```

### Bước 3: Test Nghiệp vụ Tải Lười (Lazy Loading cho Detail)
Kiểm tra API Search khi truyền thêm cờ yêu cầu lấy chi tiết (`lazyLoadDetails=true`). Lúc này Virtual Threads mới quét thêm các bộ Cache nặng.
```bash
curl -X POST http://localhost:8080/api/orders/search \
-H "Authorization: Bearer <TOKEN>" \
-H "Content-Type: application/json" \
-d '{ "orderId": "1001", "lazyLoadDetails": true }'
```
**Expected Business Response:**
```json
{
  "orderId": 1001,
  "status": "PENDING",
  "customerName": "Nguyễn Văn A",
  "deliveryAddress": "123 Đường Tôn Đức Thắng, Phường Bến Nghé, Quận 1...", // <--- TẢI LƯỜI (Vừa bốc từ Cache)
  "shipperPhone": "0987654321", // <--- TẢI LƯỜI
  "items": [
    {
      "productName": "Áo thun",
      "description": "Chất liệu cotton 100% thấm hút mồ hôi...", // <--- TẢI LƯỜI (Vừa bốc từ Cache)
      ...
    }
  ]
}
```

### Bước 4: Test Nghiệp Vụ Chống Sập (Circuit Breaker)
Chặn kết nối của DB hoặc ép lỗi hàm tính toán Dữ liệu động (VD Tồn kho). Gọi lại Bước 2.
**Expected:** Hệ thống không văng mã 500. `status` đơn hàng vẫn hiển thị, nhưng phần Tồn kho hoặc Dữ liệu ngoại lai biến thành `null` hoặc chuỗi báo lỗi mềm `(Đang bảo trì)`. Frontend vẫn render ra giao diện bình thường với Dữ liệu Tĩnh (Tên tuổi, Ảnh).
