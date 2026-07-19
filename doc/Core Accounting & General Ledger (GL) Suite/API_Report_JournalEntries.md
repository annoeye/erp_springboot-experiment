# Báo cáo API: Kiểm tra Bút toán Kế toán (Journal Entries)

### 1. Dữ liệu Giao tiếp (Request Payload)
- **Endpoint:** `/fineract-provider/api/v1/journalentries?loanId={loanId}`
- **HTTP Method:** `GET`
- **JSON Response (Lấy từ kết quả test thực tế - Đã rút gọn):**
```json
{
  "totalFilteredRecords": 4,
  "pageItems": [
    // --- Bút toán cho giao dịch Giải ngân (Disbursement 12000$) ---
    {
      "transactionId": "L3",
      "glAccountName": "Loan Portfolio",
      "entryType": {"value": "DEBIT"},
      "amount": 12000.00,
      "reversed": false              // Bút toán đang có hiệu lực
    },
    {
      "transactionId": "L3",
      "glAccountName": "Cash Account",
      "entryType": {"value": "CREDIT"},
      "amount": 12000.00,
      "reversed": false
    },
    
    // --- Bút toán cho giao dịch Thu nợ (Repayment 1000$) ---
    {
      "transactionId": "L4",
      "glAccountName": "Cash Account",
      "entryType": {"value": "DEBIT"},
      "amount": 1000.00,
      "reversed": false
    },
    {
      "transactionId": "L4",
      "glAccountName": "Loan Portfolio",
      "entryType": {"value": "CREDIT"},
      "amount": 1000.00,
      "reversed": false
    }
  ]
}
```

> [!IMPORTANT]
> **Nguyên tắc Kế toán "Không xóa bút toán" (No delete):** 
> Fineract tuân thủ nghiêm ngặt chuẩn mực kế toán ngân hàng quốc tế. Trong trường hợp giao dịch bị Revert hoặc thao tác sai (Undo Disbursal), hệ thống **tuyệt đối không xóa dòng dữ liệu (DELETE record)** trong Database. Thay vào đó, nó sẽ update cờ `"reversed": true` cho các dòng bị hủy, và tự động sinh ra các **bút toán bù trừ (Compensatory entries)** đối ứng. 
> 
> *Lời khuyên cho Dev:* Khi viết Query báo cáo tổng hợp dư nợ, luôn nhớ thêm mệnh đề `WHERE reversed = false` để không bị cộng nhầm các bút toán đã hủy!

### 2. Cấu trúc Tên Bảng & Luồng Lưu Trữ (Database Flow)
- **Cấp 1 (Bảng Lõi):** `acc_gl_journal_entry` - Bảng trung tâm lưu trữ mọi bút toán Nợ/Có. Mỗi khi có một giao dịch tài chính xảy ra, Fineract sẽ chạy qua Rules Engine, tính toán tài khoản đã map ở bước trước, và insert số chẵn các dòng (Ví dụ: 1 Nợ - 1 Có, hoặc 1 Nợ - 2 Có) vào bảng này để đảm bảo Tổng Nợ = Tổng Có.
- **Cấp 2 (Bảng Dấu vết Giao dịch):** Bảng này thường được link với `m_loan_transaction` (thông qua `entity_id` và `entity_type` = LOAN). ID của giao dịch giải ngân hay thu nợ ở bảng `m_loan_transaction` sẽ là bằng chứng để truy nguyên nguồn gốc sinh ra bút toán này.
- **Cấp 3 (Bảng Cập nhật Số dư):** Sau khi insert vào Journal Entry, hệ thống có thể cập nhật số dư lũy kế vào bảng `acc_gl_account` hoặc các bảng thống kê (như `acc_gl_journal_entry_running_balance`) phục vụ cho xuất báo cáo siêu tốc.

### 3. Ý nghĩa API & Vị trí trong Workflow
- **Ý nghĩa Kinh doanh:** Đây là bằng chứng sống cho thấy Hệ thống Kế toán Bút toán kép (Double-entry) đã hoạt động hoàn hảo. Nó tự động cân đối dòng tiền: 
  - Khi giải ngân (Tiền ra), tài khoản Tiền mặt giảm (Credit), và khoản mục Tài sản dư nợ tăng (Debit). 
  - Khi thu tiền (Tiền vào), tài khoản Tiền mặt tăng (Debit), khoản mục dư nợ giảm (Credit).
- **Vị trí trong Workflow:** Đây là API truy vấn ở **cuối chu trình** để đội ngũ Kế toán (Accountant/Auditor) đối soát số liệu hoặc tích hợp với các hệ thống ERP kế toán tổng hợp bên thứ 3.
