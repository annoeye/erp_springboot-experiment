# Báo cáo API: Khởi tạo Danh mục Phí (Charge Setup)

### 1. Dữ liệu Giao tiếp (Request Payload)
- **Endpoint:** `/fineract-provider/api/v1/charges`
- **HTTP Method:** `POST`
- **JSON Request (Tạo Phí Chuyển đổi Trả góp - Origination Fee):**
```json
{
  "name": "Origination Fee",            // Tên của loại phí
  "currencyCode": "USD",                // Mã tiền tệ (phải lưu ý trùng khớp với Loan Product sau này)
  "locale": "en",
  "chargeAppliesTo": 1,                 // 1 = Loan (Áp dụng cho Khoản vay)
  "chargeTimeType": 1,                  // 1 = Thu tại thời điểm Giải ngân (Disbursement)
  "chargeCalculationType": 1,           // 1 = Flat (Số tiền cố định)
  "amount": 50,                         // Số tiền phí thu (Ví dụ: 50$)
  "penalty": false,                     // Đây là Phí dịch vụ, không phải Tiền phạt
  "active": true,
  "chargePaymentMode": 0,               // 0 = Regular (Thu bình thường)
  "incomeAccountId": 3                  // ID của GL Account dùng để ghi nhận Doanh thu (Ví dụ ID=3: Fee Income)
}
```

- **JSON Request (Tạo Phí Phạt Trả chậm - Late Penalty):**
```json
{
  "name": "Late Payment Penalty",
  "currencyCode": "USD",
  "locale": "en",
  "chargeAppliesTo": 1,
  "chargeTimeType": 9,                  // 9 = Overdue Fees (Thu khi quá hạn)
  "chargeCalculationType": 1,
  "amount": 10,
  "penalty": true,                      // Đánh dấu đây là Tiền phạt
  "active": true,
  "chargePaymentMode": 0,
  "incomeAccountId": 3                  
}
```

### 2. Cấu trúc Tên Bảng & Luồng Lưu Trữ (Database Flow)
- **Cấp 1 (Bảng Lõi):** `m_charge` - Bảng trung tâm lưu trữ toàn bộ từ điển Phí của hệ thống. Dữ liệu như tên, số tiền, loại tính toán (flat/%)... sẽ nằm ở đây.
- **Cấp 2 (Bảng Dấu vết):** Tùy thuộc vào việc phí được gắn vào Sản phẩm hay Khoản vay, nó sẽ sinh ra ID được tham chiếu ở các bảng mapping tương ứng.

### 3. Ý nghĩa API & Vị trí trong Workflow
- **Ý nghĩa Kinh doanh:** Đây là nơi cấu hình các chính sách "hái ra tiền" cho ngân hàng. Phân biệt rõ Phí (Fee - không chịu thuế/hoặc chịu thuế tùy vùng) và Phạt (Penalty - thường dùng để răn đe).
- **Vị trí trong Workflow:** Nằm ở bước **System Setup**. Cần phải có các tài khoản GL Income/Expense trước, sau đó mới tạo danh mục Phí.
