# LUỒNG 2: ĐỊNH NGHĨA SẢN PHẨM VAY (LOAN PRODUCT SETUP)

Sản phẩm vay đóng vai trò là "Cái khuôn" định nghĩa mọi luật lệ (Lãi suất, Kỳ hạn, Kế toán) cho các hợp đồng vay sau này. 
*(Lưu ý: Luồng này thường chỉ do Admin thực hiện 1 lần duy nhất khi khai trương Sản phẩm mới).*

## Tạo Sản Phẩm Vay Trả Góp 0% Lãi Suất
* **API:** `POST /fineract-provider/api/v1/loanproducts`
* **Đặc điểm thiết kế:** Tắt tính năng hạch toán kế toán kép (`accountingRule: 1`) để giảm bớt sự phức tạp khi tích hợp MVP. Lãi suất set bằng 0.

* **JSON Request Mẫu:**
```json
{
  "name": "Trả góp Thẻ tín dụng 12 Tháng",
  "shortName": "CC12M",
  "description": "Gói vay chuyển đổi trả góp thẻ tín dụng lãi suất 0%",
  "currencyCode": "USD",
  "digitsAfterDecimal": 0,
  "inMultiplesOf": 1,
  
  "principal": 12000,
  "minPrincipal": 1000,
  "maxPrincipal": 50000,
  
  "numberOfRepayments": 12,
  "repaymentEvery": 1,
  "repaymentFrequencyType": 2,
  
  "interestRatePerPeriod": 0,
  "interestRateFrequencyType": 2,
  "amortizationType": 1,
  "interestType": 1,
  "interestCalculationPeriodType": 1,
  "daysInMonthType": 1,
  "daysInYearType": 1,
  
  "isInterestRecalculationEnabled": false,
  
  "transactionProcessingStrategyCode": "mifos-standard-strategy",
  "accountingRule": 1,
  
  "dateFormat": "dd-MM-yyyy",
  "locale": "en"
}
```

* **Kết quả trả về:** Nhận được `productId` (Ví dụ: 1). Dữ liệu chạy vào bảng `m_product_loan`.
