# Báo cáo API: Đối soát Kế toán Chuyển tiền (Execution & Recon)

### 1. Dữ liệu Giao tiếp (Request Payload)
- **Endpoint:** `/fineract-provider/api/v1/accounttransfers` (API lõi mà Standing Instruction gọi ngầm bên dưới)
- **HTTP Method:** `POST`
- **JSON Response (Giao dịch trừ 100$ từ Ví sang Khoản Vay):**
```json
{
  "savingsId": 1,
  "resourceId": 2
}
```

### 2. Cấu trúc Tên Bảng & Luồng Lưu Trữ (Database Flow)
- **Bảng `m_account_transfer_transaction`**: Insert 1 dòng nối `from_savings_transaction_id` (S3) và `to_loan_transaction_id` (L8).
- **Luồng Kế toán Kép (Bức tranh nghệ thuật của Fineract):**
  - Khi luân chuyển dòng tiền xuyên phân hệ (Cross-entity), hệ thống không cho phép hạch toán trực tiếp từ Nợ Ví -> Có Khoản vay, mà phải đi qua một **Tài khoản Trung gian (Clearing Account)** (Được cấu hình bằng Financial Activity 200 - Liability Transfer).

**Bút toán bên Ví (Transaction S3):**
1. `Nợ` Savings Control Account (Ví bị trừ tiền) - 100$
2. `Có` Overpayment Liability (Chuyển tiền vào tài khoản trung gian) - 100$

**Bút toán bên Khoản vay (Transaction L8):**
3. `Nợ` Overpayment Liability (Rút tiền từ tài khoản trung gian) - 100$
4. `Có` Loan Portfolio (Trừ dư nợ gốc Khoản vay) - 100$

> [!IMPORTANT]
> **Triết lý Zero-Sum Clearing:**
> Nhìn vào 4 dòng bút toán trên, tài khoản `Overpayment Liability` được ghi Có 100$ rồi ngay lập tức ghi Nợ 100$. Số dư của tài khoản trung gian này luôn bằng 0. Tiền được di chuyển hoàn hảo từ Ví sang Khoản Vay mà **không hề có lượng Tiền mặt (Cash)** nào ra vào ngân hàng. Đây là đỉnh cao của thiết kế Core Banking!

### 3. Ý nghĩa API & Vị trí trong Workflow
- **Ý nghĩa Kinh doanh:** API này biến Lệnh tự động (Standing Instruction) thành dòng tiền thật sự trên sổ cái. Mọi thứ được xử lý nội bộ, không tốn phí giao dịch chuyển khoản liên ngân hàng. Tiết kiệm khổng lồ cho công ty Tài chính.
- **Vị trí trong Workflow:** Bước cuối cùng của chu trình "Bán chéo" - Biến khách hàng vay thành khách hàng xài Ví điện tử.
