# Báo cáo API: Kích hoạt Lệnh Thu Tự Động (Standing Instructions)

### 1. Dữ liệu Giao tiếp (Request Payload)
- **Endpoint:** `/fineract-provider/api/v1/standinginstructions`
- **HTTP Method:** `POST`
- **JSON Request (Tạo Lệnh Auto-Deduction):**
```json
{
  "name": "Auto Repay E-Wallet",
  "fromOfficeId": 1,
  "fromClientId": 4,
  "fromAccountType": 2,          // Nguồn tiền là Savings Account (Ví điện tử)
  "fromAccountId": 1,            // ID của Ví
  
  "toOfficeId": 1,
  "toClientId": 4,
  "toAccountType": 1,            // Đích đến là Loan Account (Khoản Vay)
  "toAccountId": 6,              // ID Khoản vay
  
  "transferType": 2,             // 2 = Loan Repayment (Định danh luồng chuyển khoản để trả nợ)
  "instructionType": 2,          // 2 = Dues (Chỉ quét đúng số tiền phải đóng hàng kỳ)
  "recurrenceType": 2,           // 2 = As per Dues (Ngày chạy quét theo đúng ngày đóng họ)
  "priority": 1,                 // 1 = Urgent
  "status": 1,                   // 1 = Active
  "validFrom": "06 July 2026",
  "locale": "en",
  "dateFormat": "dd MMMM yyyy"
}
```

> [!TIP]
> **Khám phá Đắt giá từ thực chiến:** Rất nhiều trường hợp bị lỗi 500 Internal Server Error tại API này nếu khai báo thiếu `transferType: 2` (Loan Repayment) hoặc nhầm lẫn giữa `recurrenceType: 1` (Periodic) và `2` (As per Dues). Nhờ việc cấu hình đúng `instructionType: 2` và `recurrenceType: 2`, Fineract sẽ thông minh tự động đọc lịch trả nợ (Repayment Schedule) của khoản vay và chỉ rút ĐÚNG SỐ TIỀN CẦN THIẾT từ Ví thay vì rút số tiền cố định.

### 2. Cấu trúc Tên Bảng & Luồng Lưu Trữ (Database Flow)
- **Cấp 1 (Bảng Lõi):** `m_standing_instruction` - Bảng khai báo liên kết siêu liên kết giữa 2 thực thể tài khoản (Savings và Loan).

### 3. Ý nghĩa API & Vị trí trong Workflow
- **Ý nghĩa Kinh doanh:** Đây chính là tính năng "Chạm" (Auto-deduction) của các Super App (MoMo, ZaloPay) tự động trừ tiền ví nộp cho tổ chức tài chính. Khách hàng không cần mở app bấm "Thanh toán", chỉ cần nạp đủ tiền vào ví trước ngày thanh toán.
- **Vị trí trong Workflow:** Bắt buộc thực hiện sau khi Cấp khoản vay và Cấp Ví thành công.
