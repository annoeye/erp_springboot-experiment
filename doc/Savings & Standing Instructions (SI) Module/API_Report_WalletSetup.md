# Báo cáo API: Khởi tạo Sản phẩm Ví điện tử (Wallet Product Setup)

### 1. Dữ liệu Giao tiếp (Request Payload)
- **Endpoint:** `/fineract-provider/api/v1/savingsproducts`
- **HTTP Method:** `POST`
- **JSON Request (Đã làm sạch):**
```json
{
  "name": "E-Wallet",
  "shortName": "EWAL",
  "description": "Ví điện tử không kỳ hạn",
  "currencyCode": "USD",
  "digitsAfterDecimal": 2,
  "inMultiplesOf": 1,
  "locale": "en",
  
  // --- Cấu hình Lãi suất bằng 0 cho Ví ---
  "nominalAnnualInterestRate": 0,
  "interestCompoundingPeriodType": 1,        // Daily (Mặc định bắt buộc)
  "interestPostingPeriodType": 4,            // Monthly (Mặc định bắt buộc)
  "interestCalculationType": 1,              // Daily Balance (Mặc định bắt buộc)
  "interestCalculationDaysInYearType": 365,
  
  // --- Cấu hình Kế toán Ví ---
  "accountingRule": 2,                       // Cash-based accounting
  "savingsReferenceAccountId": 1,            // Cash Account (Asset) - Nơi cất tiền mặt
  "savingsControlAccountId": 7,              // Savings Control Account (Liability) - Ghi nhận nợ phải trả khách hàng
  "incomeFromFeeAccountId": 3,
  "incomeFromPenaltyAccountId": 3,
  "interestOnSavingsAccountId": 4,           // Expense Account
  "transfersInSuspenseAccountId": 5,         // Suspense (Bắt buộc phải là Liability Account)
  "overdraftPortfolioControlId": 2,
  "incomeFromInterestId": 3,
  "writeOffAccountId": 4
}
```

> [!WARNING]
> Fineract vô cùng khắt khe về định dạng Tài khoản (GL Account Type). Ví dụ `transfersInSuspenseAccountId` đối với Sản phẩm Vay có thể là Asset, nhưng đối với Sản phẩm Tiết kiệm (Savings) bắt buộc phải là **Liability**. Nếu map sai, bạn sẽ gặp lỗi 403 Domain Rule Violation: "expected account type was one among LIABILITY".

### 2. Cấu trúc Tên Bảng & Luồng Lưu Trữ (Database Flow)
- **Cấp 1 (Bảng Lõi):** `m_savings_product` - Lưu toàn bộ thiết lập và cấu hình lãi suất của Ví.
- **Cấp 2 (Bảng Mapping):** `acc_product_mapping` - Insert các dòng mapping giữa `product_id` (của Savings Product) và `gl_account_id` tương ứng với từng `financial_account_type` (như Control, Reference, Suspense...).

### 3. Ý nghĩa API & Vị trí trong Workflow
- **Ý nghĩa Kinh doanh:** API này biến Fineract không chỉ là một hệ thống cho vay (Lending) mà còn là một hệ thống nhận tiền gửi (Deposit). Việc cấu hình lãi suất 0% và map đúng tài khoản Kế toán giúp nó hoạt động hoàn hảo dưới vai trò của một **Ví Điện Tử (E-Wallet)**.
- **Vị trí trong Workflow:** Thiết lập nền tảng sản phẩm trước khi có thể mở Ví cá nhân cho bất kỳ Client nào.
