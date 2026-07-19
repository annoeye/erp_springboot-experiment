# BÁO CÁO 1: LẤY MẪU KHỞI TẠO KHÁCH HÀNG (CLIENT TEMPLATE)

## 1. Chi tiết Dữ liệu Giao tiếp (Req & Res)

**Mục đích:** Lấy cấu hình mặc định và danh sách dữ liệu nền (Dropdown list) từ hệ thống để chuẩn bị thông tin tạo mới khách hàng trên giao diện.

* **API Endpoint:** `GET https://localhost:8443/fineract-provider/api/v1/clients/template`
* **Query Parameters:** `?staffInSelectedOfficeOnly=false`
* **HTTP Headers bắt buộc:**
  * `Fineract-Platform-TenantId: default`
  * `Authorization: Basic bW1mb3M6cFzc3dvcmQ=`

**Request (Yêu cầu):**
Hệ thống sử dụng phương thức `GET` nên không có dữ liệu Body. Tham số được truyền trực tiếp trên URL để lọc dữ liệu.

**Response (Kết quả trả về - Code 200 OK):**
Dữ liệu trả về chứa thông tin cấu hình hệ thống tại thời điểm gọi.

```json
{
  "activationDate": "2026-07-03",
  "officeId": 1,
  "officeOptions": [
    {
      "id": 1,
      "name": "Head Office",
      "nameDecorated": "Head Office"
    }
  ],
  "savingProductOptions": [
    {
      "id": 4,
      "name": "account overdraft",
      "allowOverdraft": false,
      "withdrawalFeeForTransfers": false
    }
  ],
  "staffOptions": [
    {
      "id": 1,
      "firstname": "xyz",
      "lastname": "sjs",
      "displayName": "sjs, xyz",
      "officeId": 1,
      "officeName": "Head Office",
      "isLoanOfficer": true,
      "isActive": true
    }
  ],
  "datatables": [
    {
      "applicationTableName": "m_client",
      "registeredTableName": "Address Details",
      "columnHeaderData": [
        {
          "columnName": "client_id",
          "columnType": "bigint",
          "columnLength": 0,
          "columnDisplayType": "INTEGER",
          "isColumnPrimaryKey": true,
          "isColumnNullable": false,
          "columnValues": []
        }
      ]
    }
  ]
}
```

---

## 2. Cấu trúc Tên bảng & Luồng lưu (Database Flow)

Vì đây là API `GET` (Truy vấn), hệ thống hoàn toàn chỉ **Đọc (Read)** dữ liệu từ các cấp bảng, KHÔNG có dữ liệu nào được ghi mới. Cấu trúc bảng được hệ thống quét như sau:

* **Cấp 1 - Bảng Chi nhánh (`m_office`)**: Quét toàn bộ chi nhánh đang có để trả về mảng `officeOptions`. Giúp giao diện hiển thị danh sách văn phòng cho khách hàng chọn.
* **Cấp 2 - Bảng Nhân viên (`m_staff`)**: Quét bảng nhân sự (lọc theo các nhân viên đang hoạt động) để trả về mảng `staffOptions`. 
* **Cấp 3 - Bảng Sản phẩm (`m_savings_product`)**: Lọc các sản phẩm tiết kiệm đang hoạt động trả về mảng `savingProductOptions`.
* **Cấp 4 - Bảng Tùy biến (`x_registered_table` & `x_registered_table_metadata`)**: Kiểm tra xem Admin có đính kèm thêm bảng `Datatables` nào cho Khách hàng hay không để trả về cấu trúc mảng `datatables` (như bảng `Address Details` trong ví dụ trên).

---

## 3. Ý nghĩa API & Vị trí trong Workflow

* **Ý nghĩa:** Tránh việc lập trình viên Frontend (Web/App) phải hard-code (đóng cứng) danh sách chi nhánh hay nhân viên vào ứng dụng. Nhờ API này, hệ thống giao diện sẽ luôn tự động cập nhật ngay khi Admin Fineract thay đổi cấu trúc bên dưới Database.
* **Vị trí Workflow:** Nằm ở vị trí **Tiền đề (Bước 0)** trong quy trình Mở Hồ Sơ. Nó được gọi ngầm ngay lập tức khi người dùng vừa chuyển sang màn hình "Tạo mới Khách hàng".
* **Chức năng tiếp tới:** Sau khi gọi API này, Frontend sẽ lấy được danh sách `officeOptions` (Ví dụ Head Office có ID là 1). Người dùng chọn trên giao diện, và Frontend sẽ nhét cái ID=1 đó vào Body JSON của API tiếp theo (`POST /clients`) để tạo Khách hàng.
