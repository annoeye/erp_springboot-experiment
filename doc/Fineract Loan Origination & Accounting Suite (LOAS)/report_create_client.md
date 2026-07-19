# Tổng hợp API Tạo Khách Hàng (Client Onboarding)

## 1. Dữ liệu Đầu vào (Request Payload)
Endpoint: `POST /fineract-provider/api/v1/clients`

Dưới đây là cấu trúc JSON chuẩn đúc kết từ phiên chat. Chú thích mức độ bắt buộc được ghi chú trực tiếp bên cạnh các trường.

```json
{
  "officeId": 1,                      // # Bắt buộc (Chi nhánh)
  "legalFormId": 1,                   // # Bắt buộc (1: Cá nhân, 2: Doanh nghiệp)
  "firstname": "Trần",                // # Bắt buộc
  "lastname": "Thu Trà",              // # Bắt buộc
  "active": true,                     // # Bắt buộc (Kích hoạt khách hàng ngay lập tức)
  "activationDate": "06-07-2026",     // # Bắt buộc (Nếu active=true)
  "dateFormat": "dd-MM-yyyy",         // # Bắt buộc (Để Fineract parse đúng định dạng ngày)
  "locale": "en",                     // # Bắt buộc
  
  "middlename": "Thị",                // # Có thể thiếu (Tên đệm)
  "mobileNo": "0901234567",           // # Có thể thiếu (Số điện thoại)
  "dateOfBirth": "15-08-1995",        // # Có thể thiếu (Ngày sinh)
  "externalId": "APP-USER-9999",      // # Có thể thiếu (Dùng để map với ID trên App Mobile/Web của bạn)
  "submittedOnDate": "06-07-2026",    // # Có thể thiếu
  
  "address": [                        // # Có thể thiếu (ĐIỀU KIỆN: Phải bật cờ Enable-Address trong cấu hình)
    {
      "addressTypeId": 29,            // # Bắt buộc phải có nếu truyền address (ID phải lấy chuẩn từ m_code_value)
      "isActive": true,               // # Có thể thiếu
      "addressLine1": "Số 123",       // # Có thể thiếu
      "city": "Hà Nội",               // # Có thể thiếu
      "postalCode": "100000"          // # Có thể thiếu
    }
  ],
  
  "datatables": [                     // # Có thể thiếu (Dùng cho các trường dị tự chế)
    {
      "registeredTableName": "Extra", // # Bắt buộc (Bảng này phải được tạo trước trong hệ thống)
      "data": {
        "zalo": "zalo.me/090"         // # Có thể thiếu
      }
    }
  ]
}
```

---

## 2. Cấu trúc Tên bảng & Luồng lưu trữ (Database Flow)
Hệ thống Fineract áp dụng tính chuẩn hóa cao, dữ liệu từ API trên sẽ được phân rã và lưu vào các cấp bảng như sau:

### Cấp 1: Bảng Lõi (Core Table)
- **`m_client`**: Nơi lưu trữ thông tin cơ bản nhất. 
  - Các trường được lưu thẳng vào đây: `firstname`, `lastname`, `middlename`, `mobile_no`, `date_of_birth`, `external_id`, `activation_date`.
  - Đặc biệt: Fineract tự động gộp 3 tên lại và lưu thêm vào cột `display_name` để tiện query tìm kiếm.

### Cấp 2: Bảng Từ điển (Dictionary Tables)
Dùng để đối chiếu các con số ID trong JSON ra ý nghĩa chữ thực tế.
- **`m_code`**: Định nghĩa tên các nhóm danh mục (Ví dụ: nhóm `ADDRESS_TYPE`, nhóm `Customer Identifier`).
- **`m_code_value`**: Bảng chứa TẤT CẢ các danh mục con của toàn hệ thống (Ví dụ: Thường trú, Hộ chiếu, Tạm trú). 
  - Các biến như `addressTypeId` hay `legalFormId` sẽ đối chiếu với ID của bảng này.

### Cấp 3: Bảng Râu ria / Bảng Phụ (Extension Tables)
Nơi lưu trữ các trường thông tin 1-Nhiều hoặc thông tin mở rộng.
- **Lưu Địa chỉ (Nếu truyền mảng `address`)**:
  - **`m_address`**: Bảng chứa thông tin chữ thực tế của địa chỉ (Số nhà, Tên đường, Thành phố...).
  - **`m_client_address`**: Bảng trung gian (Mapping Table) nối `client_id` (Khách hàng) với `address_id` (Địa chỉ) và `address_type_id` (Loại địa chỉ).
- **Lưu Thông tin tự chế (Nếu truyền mảng `datatables`)**:
  - Dữ liệu chạy thẳng vào một bảng vật lý do Fineract sinh ra lúc Admin cấu hình (Ví dụ: bảng `Extra`). Nó sẽ có chung 1 cột khóa ngoại là `client_id`.
- **Lưu Giấy tờ tùy thân (Không hỗ trợ truyền chung API này, phải gọi API rời)**:
  - **`m_client_identifier`**: Bảng lưu CCCD, Hộ chiếu...

---

## 3. Ý nghĩa API & Vị trí trong Workflow

### Ý nghĩa của API `POST /clients`
Đây là API **Cửa Ngõ Nền Tảng**. Nó đại diện cho nghiệp vụ "Mở Hồ Sơ Khách Hàng" (Onboarding / KYC) trong Ngân hàng.
Hệ thống Fineract xoay quanh hạt nhân là Client. Khách hàng chính là mỏ neo. Nếu không có Client, hệ thống không thể tạo Khoản vay (Loan), không thể mở Sổ tiết kiệm (Savings) và không có bất kỳ giao dịch kế toán nào xảy ra.

### Vị trí trong Workflow "Mua trả góp Thẻ tín dụng"
API này đóng vai trò là **Bước Đầu Tiên** trong toàn bộ vòng đời kinh doanh của công ty bạn.

**Tiến trình (Workflow) đầy đủ cụ thể:**
1. **[HIỆN TẠI] Tạo Khách hàng (`POST /clients`)** 👉 Đăng ký thông tin định danh của người dùng từ App vào Core Banking.
2. **[TIẾP TỚI 1] Bổ sung CCCD (`POST /clients/{id}/identifiers`)** 👉 Gắn giấy tờ tùy thân để hoàn tất pháp lý.
3. **[TIẾP TỚI 2] Mở Khoản Vay Trả Góp (`POST /loans`)** 👉 Căn cứ vào `clientId`, tạo một khoản vay (Loan) 12 triệu với kỳ hạn 12 tháng tương đương với giao dịch quẹt thẻ.
4. **[TIẾP TỚI 3] Kích hoạt Giải Ngân (`POST /loans/{id}?command=disburse`)** 👉 Hệ thống bắt đầu sinh ra lịch trả nợ (Repayment Schedule) cho 12 tháng tới, đồng thời tự động sinh Bút toán Kế toán.
5. **[TIẾP TỚI 4] Thu Tiền Đóng Hàng Tháng (`POST /loans/{id}/transactions?command=repayment`)** 👉 Mỗi lần App Mobile báo khách đã thanh toán, gọi API này để cấn trừ nợ trên Core Banking.
