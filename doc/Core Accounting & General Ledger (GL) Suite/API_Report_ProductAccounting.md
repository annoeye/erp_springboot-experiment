# Báo cáo API: Cấu hình Kế toán cho Sản phẩm Vay (Product Accounting Mapping)

### 1. Dữ liệu Giao tiếp (Request Payload)
- **Endpoint:** `/fineract-provider/api/v1/loanproducts/1`
- **HTTP Method:** `PUT`
- **JSON Request (Đã làm sạch và Tối ưu):**
```json
{
  "locale": "en",                           // Bắt buộc đi kèm khi có thay đổi rule
  "accountingRule": 2,                      // Quan trọng nhất: Bật Kế toán dòng tiền (Cash-based accounting = 2), mặc định NONE = 1.
  
  // --- Mapping Tài khoản Tĩnh ---
  "fundSourceAccountId": 1,                 // Nguồn tiền giải ngân -> Map vào Cash Account
  "loanPortfolioAccountId": 2,              // Dư nợ gốc -> Map vào Loan Portfolio Account
  "interestOnLoanAccountId": 3,             // Thu nhập từ Lãi suất -> Map vào Interest Income
  "incomeFromFeeAccountId": 3,              // Thu nhập từ Phí -> Map vào Interest Income
  "incomeFromPenaltyAccountId": 3,          // Thu nhập từ Phạt -> Map vào Interest Income
  "incomeFromRecoveryAccountId": 3,         // Thu hồi nợ xấu -> Map vào Interest Income
  "writeOffAccountId": 4,                   // Chi phí xóa nợ -> Map vào Write-off Expense
  "overpaymentLiabilityAccountId": 5,       // Nợ phải trả khách hàng (khi khách trả dư) -> Map vào Liability Account
  "transfersInSuspenseAccountId": 6,        // Tài khoản trung gian chuyển đổi -> Map vào Suspense Asset Account

  // --- Mapping Động theo Kênh thanh toán (Cực kỳ quan trọng) ---
  "paymentChannelToFundSourceMappings": [
    {
      "paymentTypeId": 1,                   // 1 = Tiền mặt (Money Transfer)
      "fundSourceAccountId": 1              // Map luồng trả tiền mặt vào Cash GL Account
    }
    // Có thể map thêm các Type khác (như Momo, Bank Transfer) vào các GL Account tương ứng
  ]
}
```

> [!WARNING]
> Mảng `paymentChannelToFundSourceMappings` là bắt buộc khi hệ thống có sử dụng các kênh thanh toán khác nhau. Nếu thiếu mảng này, lúc Giải ngân hay Thu nợ mà Client truyền vào `paymentTypeId`, Fineract sẽ bắn lỗi **500 Internal Server Error** với thông báo: *"No fund source account mapping found for payment type"*.

### 2. Cấu trúc Tên Bảng & Luồng Lưu Trữ (Database Flow)
- **Cấp 1 (Bảng Lõi):** `m_product_loan` - Trường `accounting_type` trong bảng này sẽ được update từ `1` (NONE) thành `2` (CASH BASED).
- **Cấp 2 (Bảng Mapping Chính):** `acc_product_mapping` - Đây là bảng cực kỳ quan trọng. Fineract không lưu account_id trực tiếp trong bảng product. Khi gọi API trên, hệ thống sẽ insert/update nhiều dòng dữ liệu vào bảng `acc_product_mapping` với cấu trúc:
  - `product_id` = 1
  - `financial_account_type` = Loại nghiệp vụ (VD: 1=Fund Source, 2=Loan Portfolio,...)
  - `gl_account_id` = ID của GL Account tương ứng.
  - *Riêng phần Mapping Kênh thanh toán*, dữ liệu sẽ được lưu cùng với `payment_type` tương ứng.
- **Cấp 3 (Bảng Từ điển):** Đối chiếu với ID của các tài khoản đã tạo ở bảng `acc_gl_account` để đảm bảo tài khoản tồn tại và có loại (Asset/Liability/...) phù hợp với từng `financial_account_type`.

### 3. Ý nghĩa API & Vị trí trong Workflow
- **Ý nghĩa Kinh doanh:** Đây là bước "Nối dây điện". Nếu bạn chỉ tạo GL Account thì chúng mới chỉ là những cái thùng rỗng vô nghĩa. Việc map tài khoản vào Sản phẩm (Loan Product) giúp Fineract hiểu được: "À, với khoản vay thẻ tín dụng này, khi Giải ngân bằng Tiền mặt thì lấy tiền từ thùng Cash, khi thu tiền thì bỏ vào thùng Cash, phần gốc ghi giảm thùng Portfolio, phần lãi ghi tăng thùng Income". 
- **Vị trí trong Workflow:** Bước thiết lập cấu hình **Bắt buộc** sau khi đã định nghĩa xong Chart of Accounts và trước khi cấp bất kỳ khoản vay thực tế nào. Những khoản vay cấp *sau* thời điểm này mới bắt đầu sinh ra bút toán kế toán.
