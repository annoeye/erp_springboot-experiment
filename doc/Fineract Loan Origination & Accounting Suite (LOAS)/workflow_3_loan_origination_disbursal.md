# LUỒNG 3: CẤP TÍN DỤNG & GIẢI NGÂN (LOAN ORIGINATION & DISBURSAL)

Đây là luồng kinh doanh cốt lõi, diễn ra N lần mỗi khi có một khách hàng bấm nút "Đăng ký trả góp" trên App.

## Bước 1: Nộp hồ sơ Khoản vay (Submit Loan Application)
* **API:** `POST /fineract-provider/api/v1/loans`
* **Ý nghĩa:** Ráp Khách hàng (`clientId`) và Sản phẩm (`productId`) lại với nhau.
* **JSON Request Mẫu:**
```json
{
  "clientId": 4,
  "productId": 1,
  "loanType": "individual",
  "principal": 12000,
  "loanTermFrequency": 12,
  "loanTermFrequencyType": 2,
  "numberOfRepayments": 12,
  "repaymentEvery": 1,
  "repaymentFrequencyType": 2,
  "interestRatePerPeriod": 0,
  "amortizationType": 1,
  "interestType": 1,
  "interestCalculationPeriodType": 1,
  "transactionProcessingStrategyCode": "mifos-standard-strategy",
  "expectedDisbursementDate": "06-07-2026",
  "submittedOnDate": "06-07-2026",
  "dateFormat": "dd-MM-yyyy",
  "locale": "en",
  "externalId": "TXN-CREDIT-98765"
}
```
* **Kết quả:** Trả về `loanId` (Ví dụ: 4). Dữ liệu chạy vào bảng `m_loan`. Trạng thái: **100 (Pending)**.

## Bước 2: Phê Duyệt (Approve)
* **API:** `POST /fineract-provider/api/v1/loans/4?command=approve`
* **JSON Request Mẫu:**
```json
{
  "approvedOnDate": "06-07-2026",
  "approvedLoanAmount": 12000,
  "expectedDisbursementDate": "06-07-2026",
  "dateFormat": "dd-MM-yyyy",
  "locale": "en",
  "note": "Phê duyệt giao dịch trả góp thẻ tín dụng"
}
```
* **Kết quả:** Trạng thái chuyển sang **200 (Approved)**.

## Bước 3: Giải Ngân (Disburse)
* **API:** `POST /fineract-provider/api/v1/loans/4?command=disburse`
* **JSON Request Mẫu:**
```json
{
  "actualDisbursementDate": "06-07-2026",
  "transactionAmount": 12000,
  "dateFormat": "dd-MM-yyyy",
  "locale": "en",
  "note": "Xác nhận giải ngân thành công"
}
```
* **Kết quả:** Trạng thái chuyển sang **300 (Active)**. Lịch trả nợ chính thức được sinh ra trong bảng `m_loan_repayment_schedule`. Kế toán bắt đầu được kích hoạt.
