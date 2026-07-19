# LUỒNG 4: THU NỢ & TẤT TOÁN (REPAYMENT)

Quản lý quá trình Khách hàng trả tiền hàng tháng hoặc thanh toán dứt điểm toàn bộ số nợ.

## Thu tiền trả góp (Repayment)
* **Hoàn cảnh:** Khách hàng chuyển khoản tiền để thanh toán định kỳ. Backend bắt được Webhook từ Ngân hàng và gọi API này để gạch nợ trên Core.
* **API:** `POST /fineract-provider/api/v1/loans/{loanId}/transactions?command=repayment`
* **Quy tắc về Ngày thanh toán:** `transactionDate` không được phép nằm ở tương lai, và không được phép sớm hơn ngày Giải ngân khoản vay.
* **JSON Request Mẫu:**
```json
{
  "transactionDate": "06-07-2026",
  "transactionAmount": 1000,
  "dateFormat": "dd-MM-yyyy",
  "locale": "en",
  "note": "Thanh toán kỳ 1 qua App Mobile"
}
```
* **Kết quả xử lý ngầm của hệ thống:**
  1. Sinh ra 1 dòng ghi nhận Giao dịch trong bảng `m_loan_transaction` với type là Repayment.
  2. Cập nhật bảng Lịch trả nợ `m_loan_repayment_schedule` (Cột Dư nợ/Đã trả sẽ được update).
  3. Cập nhật Tổng dư nợ trong bảng `m_loan`.
  4. Nếu số tiền đóng vừa đủ để cấn trừ 100% dư nợ gốc + lãi + phí, trạng thái Khoản vay tự động đóng lại: **600 (Closed - Obligations Met)**.
