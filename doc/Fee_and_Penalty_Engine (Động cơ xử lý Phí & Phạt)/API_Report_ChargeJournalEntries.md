# Báo cáo API: Kiểm tra Bút toán Kế toán Phí (Charge Journal Entries)

### 1. Dữ liệu Giao tiếp (Request Payload)
- **Endpoint:** `/fineract-provider/api/v1/journalentries?loanId={loanId}`
- **HTTP Method:** `GET`
- **JSON Response (Giao dịch Giải ngân kèm Thu phí 50$):**
```json
{
  "totalFilteredRecords": 4,
  "pageItems": [
    // --- Bút toán 1: Giải ngân toàn bộ (12000$) ---
    {
      "transactionId": "L6",
      "glAccountName": "Loan Portfolio",
      "entryType": {"value": "DEBIT"},
      "amount": 12000.00,
      "reversed": false
    },
    {
      "transactionId": "L6",
      "glAccountName": "Cash Account",
      "entryType": {"value": "CREDIT"},
      "amount": 12000.00,
      "reversed": false
    },
    
    // --- Bút toán 2: Tự động Thu hồi Phí (50$) ---
    {
      "transactionId": "L7",
      "glAccountName": "Cash Account",
      "entryType": {"value": "DEBIT"},           // Tiền mặt tăng lên (Thu được phí)
      "amount": 50.00,
      "reversed": false
    },
    {
      "transactionId": "L7",
      "glAccountName": "Interest and Fee Income", // Chạy chính xác vào Tài khoản Thu Nhập
      "entryType": {"value": "CREDIT"},           // Doanh thu tăng lên
      "amount": 50.00,
      "reversed": false
    }
  ]
}
```

> [!IMPORTANT]
> **Hiểu đúng về Net Disbursal Amount:**
> Thay vì giải ngân 12,050$ rồi bắt khách đóng 50$, Fineract đã xử lý vô cùng tinh tế: Nó sinh ra 2 ID Giao dịch riêng biệt (L6 và L7). 
> - Khách hàng vẫn nợ gốc: 12,000$ (Portfolio Debit).
> - Nhưng thực tế (Cash Account Credit 12,000 - Debit 50) thì kho bạc ngân hàng chỉ xuất ra **11,950$**. 
> Lượng tiền 50$ chênh lệch lập tức được hạch toán thẳng vào Doanh thu Phí (Fee Income). Sự cân bằng Kế toán kép được duy trì hoàn hảo.

### 2. Cấu trúc Tên Bảng & Luồng Lưu Trữ (Database Flow)
- **Cấp 1 (Bảng Lõi):** `acc_gl_journal_entry` - Insert 4 dòng bút toán Nợ/Có.
- **Cấp 2 (Bảng Dấu vết):** `m_loan_transaction` - Insert 2 dòng: 
  - Một dòng `transaction_type_enum = 1` (Disbursement) số tiền 12000.
  - Một dòng `transaction_type_enum = 2` (Repayment - thực chất là Fee Payment) số tiền 50.

### 3. Ý nghĩa API & Vị trí trong Workflow
- **Ý nghĩa Kinh doanh:** Bằng chứng cho thấy toàn bộ chu trình "Cấu hình Kế toán -> Định nghĩa Phí -> Giải ngân thu phí" đã hợp nhất và hoạt động tự động. Công ty tài chính không bị thất thoát phí và có báo cáo P&L (Lỗ/Lãi) ngay lập tức.
- **Vị trí trong Workflow:** Cuối chu trình.
