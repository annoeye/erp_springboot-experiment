# LUỒNG 1: MỞ HỒ SƠ KHÁCH HÀNG (CLIENT ONBOARDING)

Đây là luồng tiền đề bắt buộc. Mọi giao dịch tài chính trong Fineract đều phải gắn với một Khách hàng cụ thể.

## Bước 1: Lấy dữ liệu nền (Tùy chọn)
* **API:** `GET /fineract-provider/api/v1/clients/template?staffInSelectedOfficeOnly=false`
* **Ý nghĩa:** Trả về danh sách các Chi nhánh (`officeOptions`), Nhân viên (`staffOptions`) đang có sẵn trên Core Banking để Web/App có thể render ra màn hình Dropdown cho người dùng chọn.
* **Dữ liệu lấy ra:** `officeId` (Ví dụ: 1)

## Bước 2: Tạo mới Khách hàng (Bắt buộc)
* **API:** `POST /fineract-provider/api/v1/clients`
* **Ý nghĩa:** Ghi nhận thông tin định danh của người dùng vào hệ thống Core Banking.
* **JSON Request Mẫu:**
```json
{
  "officeId": 1,
  "legalFormId": 1,
  "firstname": "Nguyễn Văn",
  "lastname": "A",
  "active": true,
  "activationDate": "06-07-2026",
  "dateFormat": "dd-MM-yyyy",
  "locale": "en",
  "mobileNo": "0901234567",
  "externalId": "APP-USER-9999"
}
```
* **Lưu ý:** `externalId` rất quan trọng, dùng để map ID của User trên App Mobile với ID của Fineract.
* **Kết quả trả về:** Nhận được `clientId` (Ví dụ: 4). Dữ liệu được lưu vào bảng `m_client`.
