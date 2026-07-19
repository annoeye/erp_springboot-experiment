# Báo cáo API: Cấp Ví & Nạp tiền (Wallet Onboarding & Top-up)

### 1. Dữ liệu Giao tiếp (Request Payload)
- **Endpoint:** `/fineract-provider/api/v1/savingsaccounts/{id}/transactions?command=deposit`
- **HTTP Method:** `POST`
- **JSON Request (Giả lập Top-up từ VNPAY):**
```json
{
  "locale": "en",
  "dateFormat": "dd MMMM yyyy",
  "transactionDate": "06 July 2026",
  "transactionAmount": "5000",
  "receiptNumber": "VNPAY-8888",     // Cực kỳ quan trọng để đối soát với cổng thanh toán ngoài
  "paymentTypeId": 1                 // Bắt buộc (1 = Money Transfer)
}
```

> [!WARNING]
> Nếu Sản phẩm Ví được bật Kế toán (Cash-based = 2), mọi giao dịch nạp tiền (Deposit) đều **bắt buộc phải truyền `paymentTypeId`**. Nếu thiếu, Fineract sẽ bắn lỗi `validation.msg.savingsaccount.transaction.paymentTypeId.cannot.be.blank` (Mã 400 Bad Request). Fineract cần biết dòng tiền nạp vào đến từ kênh nào (Tiền mặt, Chuyển khoản, Momo...) để map vào đúng GL Account nguồn quỹ (Fund Source).

### 2. Cấu trúc Tên Bảng & Luồng Lưu Trữ (Database Flow)
- **Cấp 1 (Bảng Lõi):** `m_savings_account_transaction` - Insert 1 dòng ghi nhận số tiền 5000, cập nhật `running_balance` của Ví lên 5000.
- **Cấp 2 (Bảng Chứng từ):** Hệ thống sẽ sinh ra một chuỗi `transaction_id` và đính kèm `receipt_number` ("VNPAY-8888") phục vụ cho báo cáo đối soát chéo (Reconciliation).

### 3. Ý nghĩa API & Vị trí trong Workflow
- **Ý nghĩa Kinh doanh:** Biến hệ thống Fineract thành một hệ sinh thái thanh toán độc lập. Khách hàng nạp tiền từ Bank/Ví ngoài vào App. Fineract giữ vai trò sổ cái (Ledger) quản lý số dư khả dụng của khách hàng.
- **Vị trí trong Workflow:** Sau khi kích hoạt Ví cá nhân. Cần có số dư trong ví trước khi kích hoạt Lệnh tự động thu nợ (Auto-Repayment).
