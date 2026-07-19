# Báo cáo API: Gắn Phí vào Hệ thống (Dynamic Charge Attachment)

### 1. Dữ liệu Giao tiếp (Request Payload)
- **Endpoint:** `/fineract-provider/api/v1/loans`
- **HTTP Method:** `POST`
- **JSON Request (Đã làm sạch mảng Charges):**
```json
{
  "clientId": 4,
  "productId": 1,
  "principal": "12000",
  // ... (Các trường kỳ hạn, lãi suất mặc định) ...
  "expectedDisbursementDate": "06 July 2026",
  "submittedOnDate": "06 July 2026",
  "locale": "en",
  "dateFormat": "dd MMMM yyyy",
  
  // --- Mảng Gắn Phí Động (Dynamic Attachment) ---
  "charges": [
    {
      "chargeId": 1,     // ID của Phí Chuyển đổi (Origination Fee) đã tạo ở bước trước
      "amount": 50       // Có thể ghi đè lại số tiền (Override) hoặc để trống lấy mặc định
    }
  ]
}
```

> [!TIP]
> **Khám phá Đắt giá từ thực chiến:** Nếu sử dụng phương pháp gắn phí Tĩnh (`PUT /loanproducts/{id}`), Fineract sẽ kiểm tra cực kỳ khắt khe việc trùng khớp tiền tệ (bao gồm cả `currencyCode` và `decimalPlaces`). Nếu không khớp từng li từng tí, bạn sẽ ăn lỗi 403 Domain Rule Violation.
> Ngược lại, việc gắn phí Động thông qua mảng `"charges"` lúc tạo Loan sẽ bỏ qua constraint khắt khe này, cho phép tính linh hoạt cao hơn rất nhiều (Ví dụ: 1 app BNPL cho phép nhân viên sales linh động thay đổi loại phí tùy khách VIP hay khách thường).

### 2. Cấu trúc Tên Bảng & Luồng Lưu Trữ (Database Flow)
- **Cấp 1 (Bảng Lõi):** `m_loan` - Lưu hồ sơ gốc của khoản vay.
- **Cấp 2 (Bảng Mapping):** `m_loan_charge` - Bảng cực kỳ quan trọng. Fineract sẽ insert 1 dòng vào đây chứa `loan_id` và `charge_id`. Bảng này giúp hệ thống nhớ được khoản vay nào đang gánh những loại phí nào.
- **Cấp 3 (Bảng Dòng tiền):** Các bảng `m_loan_repayment_schedule` sẽ tự động cập nhật thêm cột `fee_charges_amount` vào lịch trả nợ.

### 3. Ý nghĩa API & Vị trí trong Workflow
- **Ý nghĩa Kinh doanh:** Giải quyết bài toán thu phí linh hoạt. Có người vay 10tr thì thu 50$, vay 20tr thu 100$. Gắn phí lúc tạo hồ sơ giúp các hệ thống Frontend (App, Web) dễ dàng tùy biến giao diện nộp hồ sơ.
- **Vị trí trong Workflow:** Nằm ở bước **Tạo Hồ sơ vay (Loan Origination)**.
