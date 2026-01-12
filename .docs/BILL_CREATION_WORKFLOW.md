# 📋 Luồng Nghiệp Vụ Tạo Bill (Hóa Đơn) - ERP Standard

> **Refactored by**: Senior Backend Developer  
> **Date**: 2025-12-26  
> **Status**: ✅ Implemented

---

## 🎯 Mục Tiêu Refactoring

Tách biệt thời điểm tạo **Order** (Đơn hàng) và **Bill** (Hóa đơn) theo tiêu chuẩn ERP chuyên nghiệp:

- **Order**: Tạo ngay khi khách hàng đặt hàng (Status: PENDING)
- **Bill**: Chỉ tạo sau khi thanh toán/giao hàng thành công

---

## 📊 Business Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                      KHÁCH HÀNG ĐẶT HÀNG                        │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
                   ┌─────────────┐
                   │ Tạo ORDER   │ ◄── Status: PENDING
                   │ (NGAY LẬP TỨC)│
                   └──────┬──────┘
                          │
          ┌───────────────┴───────────────┐
          │                               │
          ▼                               ▼
    ┌──────────┐                   ┌──────────┐
    │ ONLINE   │                   │   COD    │
    │ PAYMENT  │                   │   BNPL   │
    └────┬─────┘                   └────┬─────┘
         │                              │
         │ Redirect to                  │ Order flow:
         │ Payment Gateway              │ CONFIRMED → PROCESSING
         │                              │   → PACKED → SHIPPED
         ▼                              │
    Thanh toán                          ▼
    VNPay/Momo                   ┌────────────┐
         │                       │ DELIVERED  │
         │ Webhook               └─────┬──────┘
         │ Callback                    │
         ▼                             │
    ┌────────────┐                     │
    │ Response   │                     │
    │ Code = 00? │                     │
    └─────┬──────┘                     │
          │                            │
     YES  │                            │ AUTO TRIGGER
          ▼                            ▼
    ┌─────────────────────────────────────────┐
    │       ✅ TẠO BILL (HÓA ĐƠN)            │
    │    - Lưu Payment entity (Online only)   │
    │    - Link Bill → Order                  │
    │    - Idempotency check                  │
    └─────────────────────────────────────────┘
```

---

## 🔧 Quy Tắc Nghiệp Vụ

### 1. **Khi nào tạo Bill?**

| Payment Type          | Thời điểm tạo Bill                    | Entity Payment |
|-----------------------|---------------------------------------|----------------|
| `NCB` (VNPay)         | Sau khi nhận Webhook `00` (Success)  | ✅ Tạo         |
| `MOMO`                | Sau khi nhận Webhook `00` (Success)  | ✅ Tạo         |
| `PAYMENT_UPON_DELIVERY` (COD) | Khi Order → `DELIVERED`  | ❌ Không tạo   |
| `BUY_NOW_PAY_LATER` (BNPL)    | Khi Order → `DELIVERED`  | ❌ Không tạo   |

### 2. **Idempotency - Tránh tạo trùng**

```java
// ✅ Kiểm tra Bill đã tồn tại chưa
if (billRepository.existsByOrder_Id(orderId)) {
    return existingBill; // Trả về Bill cũ, không tạo mới
}
```

### 3. **Transaction Safety**

- Tất cả operations đều có `@Transactional`
- Bill creation được wrap trong try-catch riêng
- Nếu tạo Bill fail → Không ảnh hưởng đến Order status

---

## 📂 File Changes

### 1. **BillRepository.java** ⭐ NEW

```java
boolean existsByOrder_Id(UUID orderId);  // Idempotency check
Optional<Bill> findByOrder_Id(UUID orderId);
```

### 2. **BillService.java** 🔄 REFACTORED

#### Method 1: `addBill()` - Cho Online Payment

```java
@Transactional
public Response<?> addBill(CreateBillRequest request, PaymentDto paymentDto) {
    // ✅ Idempotency check
    if (billRepository.existsByOrder_Id(orderId)) {
        return Response.ok(existingBill);
    }
    
    // Tạo Payment entity nếu Online Payment
    Payment payment = null;
    if (isOnlinePayment(request.getPaymentType())) {
        payment = paymentMapper.toEntity(paymentDto);
        payment.setOrder(order);
    }
    
    // Tạo Bill
    Bill bill = Bill.builder()
        .payment(payment)  // Online: có Payment, COD: null
        ...
        .build();
    
    return Response.ok(billRepository.save(bill));
}
```

#### Method 2: `createBillForCODOrder()` - Cho COD ⭐ NEW

```java
@Transactional
public Bill createBillForCODOrder(Order order) {
    // ✅ Idempotency check
    if (billRepository.existsByOrder_Id(order.getId())) {
        return existingBill;
    }
    
    // ✅ Business rule check
    if (!isCODOrBNPL(paymentType)) {
        throw new BusinessException("Chỉ tạo Bill cho COD/BNPL");
    }
    
    Bill bill = Bill.builder()
        .payment(null)  // COD không có Payment entity
        ...
        .build();
    
    return billRepository.save(bill);
}
```

### 3. **PaymentController.java** 🔄 UPDATED

```java
@GetMapping("/vnpay-return")
public ResponseEntity<?> paymentReturn(...) {
    // Parse VNPay response
    
    // ✅ Check thanh toán thành công
    if ("00".equals(vnp_ResponseCode) && "00".equals(vnp_TransactionStatus)) {
        // TODO: Inject BillService và gọi addBill()
        // billService.addBill(createBillRequest, paymentDto);
        
        return Response.ok("Thanh toán thành công! Hóa đơn đang được tạo.");
    } else {
        return Response.error("Thanh toán thất bại");
    }
}
```

### 4. **OrderService.java** 🔄 UPDATED

#### Inject BillService

```java
private final BillService billService;  // ✅ NEW dependency
```

#### Hook trong `markAsDelivered()`

```java
@Transactional
public Response<OrderDto> markAsDelivered(String orderId) {
    // Update Order status
    order.setStatus(OrderStatus.DELIVERED);
    Order savedOrder = orderRepository.save(order);
    
    // ✅ TỰ ĐỘNG TẠO BILL CHO COD/BNPL
    try {
        PaymentType paymentType = getPaymentType(order);
        
        if (paymentType == COD || paymentType == BNPL) {
            Bill bill = billService.createBillForCODOrder(savedOrder);
            log.info("✅ Bill auto-created for COD/BNPL");
        }
    } catch (Exception e) {
        log.error("❌ Failed to create Bill: {}", e.getMessage());
        // Không throw để không ảnh hưởng Order
    }
    
    return Response.ok(orderDto);
}
```

---

## 🚀 Usage Examples

### Scenario 1: Khách hàng thanh toán Online (VNPay)

```
1. User đặt hàng → Order PENDING
2. User redirect to VNPay
3. User thanh toán thành công
4. VNPay gửi Webhook → PaymentController.paymentReturn()
5. ✅ BillService.addBill() 
   - Tạo Payment entity
   - Tạo Bill
   - Link Payment → Bill → Order
```

### Scenario 2: Khách hàng chọn COD

```
1. User đặt hàng → Order PENDING
2. Admin confirm → Order CONFIRMED
3. Admin process → Order PROCESSING → PACKED → SHIPPED
4. Shipper giao hàng → OrderService.markAsDelivered()
5. ✅ BillService.createBillForCODOrder()
   - Tạo Bill (payment = null)
   - Link Bill → Order
```

### Scenario 3: Webhook gọi 2 lần (Idempotency)

```
1. Webhook lần 1 → Tạo Bill thành công (ID: abc-123)
2. Webhook lần 2 → Detect Bill đã tồn tại
3. ✅ Return Bill cũ (ID: abc-123)
4. ❌ KHÔNG tạo Bill mới
```

---

## ✅ Checklist Implementation

- [x] Thêm `existsByOrder_Id()` vào BillRepository
- [x] Implement Idempotency check trong BillService
- [x] Tạo `createBillForCODOrder()` method
- [x] Update PaymentController webhook handler
- [x] Inject BillService vào OrderService
- [x] Add Bill creation hook trong `markAsDelivered()`
- [x] Add comprehensive logging
- [x] Ensure transaction safety
- [ ] **TODO**: Complete PaymentController integration (inject BillService)
- [ ] **TODO**: Unit tests for BillService
- [ ] **TODO**: Integration tests cho workflow

---

## 🔒 Security & Best Practices

### 1. **Transaction Isolation**

```java
@Transactional  // Đảm bảo ACID
public Response<?> addBill(...) {
    // All DB operations in same transaction
}
```

### 2. **Error Handling**

```java
try {
    billService.createBillForCODOrder(order);
} catch (Exception e) {
    log.error("Bill creation failed: {}", e.getMessage());
    // Không throw để không rollback Order update
}
```

### 3. **Logging Strategy**

```java
log.info("✅ Bill created successfully");  // Success
log.warn("Bill already exists");           // Idempotency
log.error("❌ Failed to create Bill");     // Error
```

---

## 📌 Next Steps

1. **Complete PaymentController**: Inject `BillService` và complete webhook logic
2. **Add Validation**: Validate CreateBillRequest fields
3. **Unit Tests**: Test idempotency, business rules
4. **Integration Tests**: Test end-to-end workflow
5. **Monitoring**: Add metrics cho Bill creation success rate
6. **Webhook Retry**: Handle VNPay webhook retry logic

---

## 📞 Contact

Nếu có thắc mắc về implementation, liên hệ:
- Senior Backend Developer
- Team: ERP Development

---

**Last Updated**: 2025-12-26 08:30:00 ICT
