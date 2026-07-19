# Báo cáo API: Khởi tạo Tài khoản Sổ cái (GL Accounts)

### 1. Dữ liệu Giao tiếp (Request Payload)
- **Endpoint:** `/fineract-provider/api/v1/glaccounts`
- **HTTP Method:** `POST`
- **JSON Request (Đã làm sạch):**
```json
{
  "name": "Cash Account",           // Tên tài khoản hiển thị trên hệ thống báo cáo
  "glCode": "10001",                // Mã số tài khoản kế toán (phải là duy nhất)
  "manualEntriesAllowed": true,     // Cho phép kế toán viên tạo bút toán tay (Manual Journal Entry) vào tài khoản này
  "type": 1,                        // Phân loại tài khoản: 1=Asset (Tài sản), 2=Liability (Nợ phải trả), 3=Equity (Vốn), 4=Income (Thu nhập), 5=Expense (Chi phí)
  "usage": 1,                       // Mức độ sử dụng: 1=Detail (Tài khoản chi tiết dùng để hạch toán), 2=Header (Tài khoản tổng hợp dùng để gom nhóm)
  "description": "Tài khoản tiền mặt" // Mô tả ngắn gọn mục đích tài khoản
}
```

> [!TIP]
> **Lưu ý dành cho Developer:** JSON mẫu trên chỉ là ví dụ để tạo **1 tài khoản duy nhất**. Trong thực tế, Fineract đòi hỏi tối thiểu 6 tài khoản cơ bản (Asset, Income, Liability, Expense, Suspense) để cấu hình kế toán. Bạn **cần viết một script chạy vòng lặp API này** để tự động khởi tạo đủ bộ 6 tài khoản trước khi chuyển sang bước cấu hình Sản phẩm vay.

### 2. Cấu trúc Tên Bảng & Luồng Lưu Trữ (Database Flow)
- **Cấp 1 (Bảng Lõi):** `acc_gl_account` - Đây là bảng trung tâm lưu trữ toàn bộ danh mục tài khoản (Chart of Accounts). Dữ liệu từ payload (name, gl_code, account_usage, classification_enum) được insert trực tiếp vào bảng này.
- **Cấp 2 (Bảng Từ điển):** Không có bảng mapping phức tạp, do các giá trị Enum như `type` (Classification) và `usage` được hardcode cố định trong tầng Application/Domain (mã nguồn Java). Tuy nhiên, có liên kết với `m_appuser` (lưu ID người tạo/cập nhật).
- **Cấp 3 (Bảng Trung gian):** Chưa phát sinh. Ở giai đoạn khởi tạo, tài khoản GL đứng độc lập. (Việc nối dây tài khoản với Sản phẩm vay sẽ sinh ra data ở bảng mapping khác).

### 3. Ý nghĩa API & Vị trí trong Workflow
- **Ý nghĩa Kinh doanh:** API này dùng để thiết lập nền móng cho Kế toán kép (Double-entry). Nó tạo ra các "rổ" tài khoản (Cash, Portfolio, Income, Expense,...) để ứng dụng có chỗ ghi nhận các bút toán Nợ (Debit) và Có (Credit). Nếu không có các tài khoản này, ngân hàng không thể theo dõi biến động dòng tiền, không thể lên được Bảng Cân đối kế toán (Balance Sheet).
- **Vị trí trong Workflow:** Là API nền tảng thuộc pha **System Setup (Thiết lập hệ thống)**. Bắt buộc phải được chạy để định nghĩa danh mục tài khoản kế toán *trước* khi tiến hành config Kế toán cho Sản phẩm Vay (Loan Product).
