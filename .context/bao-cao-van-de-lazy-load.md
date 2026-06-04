# Báo Cáo Vấn Đề: LazyInitializationException

> Dự án: ERP Spring Boot  
> Ngày: 02/06/2026  
> Tính năng: `GET /api/orders/my-orders`

---

## Vấn Đề

```java
org.hibernate.LazyInitializationException: 
failed to lazily initialize a collection of role: com.anno.ERP_SpringBoot_Experiment.model.entity.Order.orderItems: 
could not initialize proxy - no Session
    at OrderItemMapperImpl.toDto(OrderItemMapperImpl.java:103)
    at OrderMapperImpl.toDto(OrderMapperImpl.java:148)
    at OrderService.getMyOrders(OrderService.java:149)
```

## Nguyên Nhân

`Order.orderItems` được khai báo là **LAZY** (mặc định của `@OneToMany`):

```java
// Order.java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, 
           orphanRemoval = true, fetch = FetchType.LAZY)  // ← LAZY
List<OrderItem> orderItems = new ArrayList<>();
```

Khi `getMyOrders()` được gọi:

```
Bước 1: Gọi OrderService.getMyOrders()
Bước 2: orderRepository.findByCustomerId() → Query DB → trả về Proxy Order
Bước 3: Method kết thúc → Hibernate đóng session
Bước 4: orderMapper.toDto(order) → gọi order.getOrderItems()
Bước 5: Hibernate cố lazy load nhưng session đã đóng
Bước 6: → LazyInitializationException 💥
```

## Cách Giải Quyết

Thêm `@Transactional(readOnly = true)` vào method:

```java
// OrderService.java — TRƯỚC (lỗi)
@Override
public Response<PagingResponse<OrderDto>> getMyOrders(OrderSearchRequest r) {
    ...
    var p = orderRepository.findByCustomerId(u.getId(), pageable);
    return p.map(orderMapper::toDto);  // ❌ Session đóng trước khi map
}

// OrderService.java — SAU (fix)
@Override
@Transactional(readOnly = true)  // ← giữ session mở trong suốt method
public Response<PagingResponse<OrderDto>> getMyOrders(OrderSearchRequest r) {
    ...
    var p = orderRepository.findByCustomerId(u.getId(), pageable);
    return p.map(orderMapper::toDto);  // ✅ Còn session, load được orderItems
}
```

## Luồng Sau Fix

```
Bước 1: @Transactional(readOnly = true) → Mở Hibernate session
Bước 2: Query DB → Proxy Order (orderItems chưa load)
Bước 3: orderMapper.toDto() → gọi order.getOrderItems()
Bước 4: Hibernate lazy load orderItems từ DB (vì còn session)
Bước 5: Kết thúc method → Đóng session
Bước 6: → OK ✅
```

## Các Giải Pháp Khác

| Giải pháp | Mô tả | Khi nào dùng |
|---|---|---|
| `@Transactional(readOnly = true)` ✅ | Giữ session mở trong method | Method đọc có map entity (ưu tiên dùng) |
| `JOIN FETCH` trong JPQL | Eager load collection từ query | Khi cần tối ưu, tránh N+1 query |
| `spring.jpa.open-in-view=true` | OSIV mặc định Spring Boot (giữ session suốt request) | Mặc định, có thể tắt nếu lo ngại performance |

## Ví dụ Nhỏ

```
Giả sử Order có 2 OrderItem:

Order(id=1, orderNumber="ORD-0001")
  ├── OrderItem(id=1, productName="Áo Thun", quantity=2)
  └── OrderItem(id=2, productName="Quần Jean", quantity=1)

Khi gọi getMyOrders():
  1. Repository: SELECT * FROM orders WHERE customer_id = ?
     → Trả về Order proxy (orderItems = LAZY proxy)
  
  2. KHÔNG có @Transactional:
     → Session đóng sau khi repository trả về
     → orderMapper.toDto() gọi order.getOrderItems().size()
     → LazyInitializationException 💥
  
  3. CÓ @Transactional(readOnly = true):
     → Session còn mở
     → orderMapper.toDto() gọi order.getOrderItems()
     → Hibernate: SELECT * FROM order_items WHERE order_id = 1
     → Map thành công ✅
```

## Kinh Nghiệm

- Method nào có **đọc entity** và **map sang DTO** đều cần `@Transactional(readOnly = true)`
- `readOnly = true` là optimization hint cho Hibernate (không dirty check)
- `@OneToMany(fetch = FetchType.EAGER)` là giải pháp tồi (luôn load dù không cần)
