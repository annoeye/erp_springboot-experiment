# ERP SpringBoot Experiment — Backend Overview

## 🏗️ Công nghệ
**Java 21 + Spring Boot 3.5** | Oracle XE | Kafka | Redis | MinIO | JWT

---

## 📡 API Endpoints

### 🛒 Merchandise — `/api/merchandise`

| Method | Path | Body / Param | Response |
|--------|------|-------------|----------|
| POST | `/add-Product` | `{name, categorySku, status?, discountPercent?, discountStartDate?, discountEndDate?}` | `200` |
| PUT | `/update-Product` | `{id, name?, categoryId?, status?, discountPercent?, ...}` | `200` |
| DELETE | `/delete-Product` | `?ids=1,2,3` | `204` |
| POST | `/search-Product` | `{keyword?, statuses?, categoryIds?, minPrice?, maxPrice?, paging?}` | `Page<ProductDto>` |
| POST | `/add-Category` | `?name=Danh mục` | `201` |
| POST | `/add-Attributes` | `{name, productSku, keywords?, attributes: [{name,value,price,salePrice,stockQuantity,statusProduct}]}` | `201` |
| POST | `/search-Attributes` | `{keyword?, productIds?, statuses?, minPrice?, paging?}` | `Page<AttributesDto>` |

### 📦 Order — `/api/orders`

| Method | Path | Auth | Body / Param | Response |
|--------|------|------|-------------|----------|
| **POST** | **/** | JWT | `{items:[{attributesSku,quantity}], address_id, payment_method, discount_code?, customer_notes?, shipping_method?, language?, bank_code?}` | `201 {orderNumber, currentStatus, currentStatusDescription}` |
| GET | `/{id}` | JWT | — | Chi tiết đơn |
| GET | `/number/{orderNumber}` | JWT | — | Tra mã đơn |
| POST | `/my-orders` | JWT | `{paging?}` | Danh sách đơn |
| POST | `/search` | Admin | `{orderNumber?, customerName?, orderStatus?, startDate?, endDate?, paging?}` | Tìm kiếm |
| POST | `/cancel` | JWT | `{orderId, cancellationReason}` | `currentStatus: "CANCELLED"` |
| POST | `/confirm` | Admin | `{orderId}` | `→ CONFIRMED` |
| POST | `/process` | Admin | `{orderId}` | `→ PROCESSING` |
| POST | `/ship-details` | Admin | `{orderId, shipperId, shipperName?, shipperPhone?}` | `→ SHIPPING` |
| POST | `/deliver` | Shipper | `{orderId, recipientName?}` | `→ DELIVERED` |
| POST | `/delay` | Shipper | `{orderId, reason, newEstimatedDeliveryDate?}` | `→ DELAYED` |
| POST | `/ready-for-pickup` | Admin | `{orderId}` | `→ READY_FOR_PICKUP` |
| POST | `/pickup` | JWT | `{orderId}` | `→ DELIVERED` |
| POST | `/return` | Shipper | `{orderId, reason}` | `→ RETURNING` |
| POST | `/confirm-return` | Admin | `{orderId, condition?}` | `→ RETURNED` |
| POST | `/refund` | Admin | `{orderId, refundAmount?}` | `→ REFUNDED` |
| POST | `/payment-callback` | Public | `{orderNumber, transactionId, status, paymentMethod, amount}` | `→ PROCESSING / FAILED` |

---

## 🔁 Payment method → Auto status

| payment_method | Khi tạo đơn | Sau đó |
|---------------|-------------|--------|
| `COD` | `PENDING → CONFIRMED → PROCESSING` | Chờ admin giao |
| `VNPAY/CARD` | `PENDING → WAITING_PAYMENT` | Chờ webhook `SUCCESS` |
| — | `PENDING` | Chờ admin confirm |

---

## 📤 Kafka Events

| Topic | Event | Payload |
|-------|-------|---------|
| `order-topic` | `ORDER_CREATED` | `{eventType, orderId, orderNumber, amount, currency, paymentMethod, customerId, createdAt}` |
| `order-topic` | `ORDER_STATUS_CHANGED` | `{eventType, orderNumber, previousStatus, newStatus, previousStatusDescription, newStatusDescription, note, changedAt, changedBy, changedByRole}` |
| `order-topic` | `ORDER_CANCELLED` | `{eventType, orderNumber, reason, refundRequired, cancelledAt, cancelledBy}` |
| `payment-result` | *(nhận từ payment service)* | `{orderNumber, status: SUCCESS/FAILED, transactionId, paymentMethod, amount}` |

---

## 📋 Order Status — 14 trạng thái

| Status | Display | Description (~10 từ) |
|--------|---------|----------------------|
| `PENDING` | Chờ xác nhận | Đơn hàng đã được tạo và đang chờ được xác nhận |
| `WAITING_PAYMENT` | Chờ thanh toán | Đơn hàng đang chờ thanh toán trực tuyến từ khách hàng |
| `CONFIRMED` | Đã xác nhận | Đơn hàng đã được xác nhận và đang chờ xử lý |
| `PROCESSING` | Đang xử lý | Đơn hàng đang được đóng gói và chuẩn bị giao |
| `SHIPPING` | Đang giao hàng | Đơn hàng đang được shipper vận chuyển đến khách |
| `DELAYED` | Giao hàng chậm | Đơn hàng đang bị chậm so với thời gian dự kiến giao |
| `READY_FOR_PICKUP` | Chờ lấy hàng | Đơn hàng đã sẵn sàng để khách đến lấy tại cửa hàng |
| `DELIVERED` | Đã giao hàng | Đơn hàng đã được giao thành công đến khách hàng |
| `COMPLETED` | Hoàn thành | Đơn hàng đã hoàn tất và khách hàng đã nhận được hàng |
| `FAILED` | Thanh toán thất bại | Giao dịch thanh toán trực tuyến của đơn hàng bị thất bại |
| `CANCELLED` | Đã hủy | Đơn hàng đã bị hủy và không thể tiếp tục xử lý |
| `RETURNING` | Hoàn trả hàng | Đơn hàng đang trong quá trình được khách hàng hoàn trả |
| `RETURNED` | Đã trả hàng | Đơn hàng đã được khách hàng hoàn trả lại thành công |
| `REFUNDED` | Đã hoàn tiền | Số tiền của đơn hàng đã được hoàn trả lại cho khách hàng |

Response mẫu:
```json
{
  "status": { "code": 200, "message": "Success" },
  "data": {
    "orderNumber": "ORD-20260530-5441",
    "currentStatus": "PROCESSING",
    "currentStatusDescription": "Đơn hàng đang được đóng gói và chuẩn bị giao",
    "status": ["PENDING","CONFIRMED","PROCESSING"]
  }
}
```

---

## 🔐 Auth

- Header: `Authorization: Bearer <JWT token>`
- Admin check: `@PreAuthorize("hasRole('ADMIN')")`
- Customer/Shipper: `@PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")`
- Webhook callback: public (không auth)
