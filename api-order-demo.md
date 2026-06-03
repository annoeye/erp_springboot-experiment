# API Order — Demo Data

> Base URL: `/api/orders`  
> Auth: JWT (Bearer token)

---

## Danh sách API

| # | Method | Endpoint | Vai trò | Mô tả |
|---|--------|----------|---------|-------|
| 1 | `POST` | `/api/orders` | CUSTOMER, ADMIN | Tạo đơn hàng |
| 2 | `GET` | `/api/orders/{orderId}` | CUSTOMER, ADMIN | Xem chi tiết đơn |
| 3 | `GET` | `/api/orders/number/{orderNumber}` | CUSTOMER, ADMIN | Xem đơn theo mã |
| 4 | `POST` | `/api/orders/my-orders` | CUSTOMER, ADMIN | Danh sách đơn của tôi |
| 5 | `POST` | `/api/orders/cancel` | CUSTOMER, ADMIN | Hủy đơn hàng |
| 6 | `POST` | `/api/orders/search` | ADMIN | Tìm kiếm đơn hàng |
| 7 | `PUT` | `/api/orders/shipping` | ADMIN | Cập nhật vận chuyển |
| 8 | `PUT` | `/api/orders/delivery` | ADMIN | Cập nhật ngày giao |
| 9 | `PUT` | `/api/orders/admin-notes` | ADMIN | Cập nhật ghi chú |
| 10 | `POST` | `/api/orders/confirm` | ADMIN | Xác nhận đơn |
| 11 | `POST` | `/api/orders/complete` | ADMIN | Hoàn thành đơn |
| 12 | `GET` | `/api/orders/pending` | ADMIN | Đơn chờ xử lý |
| 13 | `GET` | `/api/orders/in-progress` | ADMIN | Đơn đang giao |
| 14 | `GET` | `/api/orders/statistics` | ADMIN | Thống kê đơn |
| 15 | `POST` | `/api/orders/transition` | ADMIN | Chuyển trạng thái |
| 16 | `POST` | `/api/orders/ship` | ADMIN | Giao cho tài xế |
| 17 | `GET` | `/api/orders/delivery-pin/{orderNumber}` | ADMIN | Xem PIN shipper |
| 18 | `DELETE` | `/api/orders/delivery-pin/{orderNumber}` | ADMIN | Xóa PIN shipper |

---

## 1️⃣ Tạo đơn hàng — `POST /api/orders`

### Request — Tạo từ danh sách items (COD)

```json
{
  "items": [
    { "attributesSku": "ATTR001", "quantity": 2 },
    { "attributesSku": "ATTR002", "quantity": 1 }
  ],
  "is_from_cart": false,
  "address_id": "1",
  "shipping_method": "GHN",
  "payment_method": "COD",
  "customer_notes": "Giao hàng giờ hành chính",
  "language": "vn"
}
```

### Request — Tạo từ giỏ hàng (COD)

```json
{
  "is_from_cart": true,
  "address_id": "1",
  "payment_method": "COD",
  "customer_notes": ""
}
```

### Request — Tạo từ booking (Online Payment)

```json
{
  "booking_id": "1",
  "address_id": "1",
  "payment_method": "VNPAY",
  "language": "vn",
  "bank_code": "NCB"
}
```

### Response — Thành công (COD: PENDING → CONFIRMED → PROCESSING)

```json
{
  "status": "OK",
  "message": "Thành công",
  "data": {
    "id": 1,
    "orderNumber": "ORD-20260601-0001",
    "status": ["PENDING", "CONFIRMED", "PROCESSING"],
    "currentStatus": "PROCESSING",
    "currentStatusDescription": "Đơn hàng đang được đóng gói và chuẩn bị giao",
    "customerId": 1,
    "customerName": "Nguyễn Văn A",
    "customerEmail": "a@example.com",
    "customerPhone": "0912345678",
    "orderItems": [
      {
        "id": 1,
        "orderId": 1,
        "productId": 1,
        "attributesId": 1,
        "productName": "Áo Thun Nam",
        "productSku": "SP001",
        "attributesSku": "ATTR001",
        "variantOptions": [
          { "name": "Màu sắc", "values": ["Đỏ"] },
          { "name": "Kích cỡ", "values": ["L"] }
        ],
        "quantity": 2,
        "unitPrice": 199000,
        "salePrice": 179000,
        "subtotal": 358000,
        "imageUrl": "https://minio.example.com/products/ao-thun-1.jpg"
      },
      {
        "id": 2,
        "orderId": 1,
        "productId": 1,
        "attributesId": 2,
        "productName": "Áo Thun Nam",
        "productSku": "SP001",
        "attributesSku": "ATTR002",
        "variantOptions": [
          { "name": "Màu sắc", "values": ["Xanh"] },
          { "name": "Kích cỡ", "values": ["M"] }
        ],
        "quantity": 1,
        "unitPrice": 199000,
        "salePrice": 179000,
        "subtotal": 179000,
        "imageUrl": "https://minio.example.com/products/ao-thun-2.jpg"
      }
    ],
    "subtotal": 537000,
    "discountAmount": 0,
    "discountCode": null,
    "taxAmount": 0,
    "shippingFee": 30000,
    "totalAmount": 567000,
    "shippingInfo": {
      "id": 1,
      "fullAddress": "123 Nguyễn Huệ, Quận 1, TP.HCM",
      "phone": "0912345678",
      "recipientName": "Nguyễn Văn A"
    },
    "customerNotes": "Giao hàng giờ hành chính",
    "shippingMethod": "GHN",
    "estimatedDeliveryDate": null,
    "auditInfo": {
      "createdBy": "admin",
      "createdAt": "2026-06-01T10:30:00",
      "updatedBy": "admin",
      "updatedAt": "2026-06-01T10:30:00"
    }
  }
}
```

### Response — Thành công (Online Payment: PENDING → WAITING_PAYMENT)

```json
{
  "status": "OK",
  "message": "Thành công",
  "data": {
    "id": 2,
    "orderNumber": "ORD-20260601-0002",
    "status": ["PENDING", "WAITING_PAYMENT"],
    "currentStatus": "WAITING_PAYMENT",
    "currentStatusDescription": "Đơn hàng đang chờ thanh toán trực tuyến từ khách hàng",
    ...
    "totalAmount": 567000
  }
}
```

---

## 2️⃣ Xem chi tiết đơn — `GET /api/orders/{orderId}`

### Request

```
GET /api/orders/1
```

### Response — Giống create, chỉ khác `status` theo trạng thái hiện tại

```json
{
  "status": "OK",
  "data": {
    "id": 1,
    "orderNumber": "ORD-20260601-0001",
    "status": ["PENDING", "CONFIRMED", "PROCESSING"],
    "currentStatus": "PROCESSING",
    ...
  }
}
```

---

## 3️⃣ Xem đơn theo mã — `GET /api/orders/number/{orderNumber}`

### Request

```
GET /api/orders/number/ORD-20260601-0001
```

### Response — Giống getOrderById

---

## 4️⃣ Danh sách đơn của tôi — `POST /api/orders/my-orders`

### Request — Có sắp xếp + lọc theo trạng thái, thời gian

```json
{
  "orderStatus": "PROCESSING",
  "startDate": "2026-06-01T00:00:00",
  "endDate": "2026-06-30T23:59:59",
  "page": 0,
  "size": 10,
  "sortBy": "auditInfo.createdAt",
  "sortDirection": "DESC"
}
```

### Request — Mặc định (page 0, size 10, không sort)

```json
{}
```

### Response

```json
{
  "status": "OK",
  "data": {
    "contents": [
      {
        "id": 1,
        "orderNumber": "ORD-20260601-0001",
        "status": ["PENDING", "CONFIRMED", "PROCESSING"],
        "currentStatus": "PROCESSING",
        "currentStatusDescription": "Đơn hàng đang được đóng gói và chuẩn bị giao",
        "subtotal": 537000,
        "totalAmount": 567000,
        "orderItems": [...],
        "customerName": "Nguyễn Văn A",
        "auditInfo": {
          "createdAt": "2026-06-01T10:30:00"
        }
      }
    ],
    "paging": {
      "pageNumber": 0,
      "pageSize": 10,
      "totalElements": 1,
      "totalPages": 1
    }
  }
}
```

---

## 5️⃣ Hủy đơn hàng — `POST /api/orders/cancel`

Chỉ hủy được nếu trạng thái hiện tại cho phép (PENDING, WAITING_PAYMENT, CONFIRMED, PROCESSING).

### Request

```json
{
  "orderId": "1",
  "cancellationReason": "Khách muốn đổi sản phẩm khác"
}
```

### Response — Thành công

```json
{
  "status": "OK",
  "message": "Thành công",
  "data": {
    "id": 1,
    "orderNumber": "ORD-20260601-0001",
    "status": ["PENDING", "CONFIRMED", "PROCESSING", "CANCELLED"],
    "currentStatus": "CANCELLED",
    "currentStatusDescription": "Đơn hàng đã bị hủy và không thể tiếp tục xử lý",
    "cancellationReason": "Khách muốn đổi sản phẩm khác",
    "cancelledAt": "2026-06-01T11:00:00",
    "cancelledBy": "admin",
    ...
  }
}
```

### Response — Lỗi (trạng thái không cho phép hủy)

```json
{
  "status": "BAD_REQUEST",
  "message": "Không thể chuyển từ Đã giao hàng sang Đã hủy",
  "errorCode": "INVALID_STATUS_TRANSITION"
}
```

---

## 6️⃣ Tìm kiếm đơn hàng (Admin) — `POST /api/orders/search`

### Request — Tìm theo mã + trạng thái

```json
{
  "orderNumber": "ORD-20260601",
  "orderStatus": "PROCESSING",
  "customerName": "Nguyễn Văn",
  "customerEmail": "a@example.com",
  "customerPhone": "0912345678",
  "minAmount": 100000,
  "maxAmount": 1000000,
  "startDate": "2026-06-01T00:00:00",
  "endDate": "2026-06-30T23:59:59",
  "page": 0,
  "size": 10,
  "sortBy": "auditInfo.createdAt",
  "sortDirection": "DESC"
}
```

### Request — Tìm kiếm đơn giản

```json
{
  "orderNumber": "ORD-20260601-0001"
}
```

### Response

```json
{
  "status": "OK",
  "data": {
    "contents": [
      {
        "id": 1,
        "orderNumber": "ORD-20260601-0001",
        "status": ["PENDING", "CONFIRMED", "PROCESSING"],
        "currentStatus": "PROCESSING",
        "currentStatusDescription": "Đơn hàng đang được đóng gói và chuẩn bị giao",
        "customerId": 1,
        "customerName": "Nguyễn Văn A",
        "customerEmail": "a@example.com",
        "customerPhone": "0912345678",
        "orderItems": [...],
        "subtotal": 537000,
        "totalAmount": 567000,
        "adminNotes": "Giao hàng ưu tiên",
        "cancelledBy": null,
        "confirmedBy": "admin",
        "confirmedAt": "2026-06-01T10:35:00",
        "completedAt": null,
        "auditInfo": {...}
      }
    ],
    "paging": {
      "pageNumber": 0,
      "pageSize": 10,
      "totalElements": 1,
      "totalPages": 1
    }
  }
}
```

---

## 7 → 11: Admin Operations

### 7. Cập nhật vận chuyển — `PUT /api/orders/shipping`

```json
{
  "orderId": "1",
  "shippingMethod": "GHTK",
  "shippingInfo": "Giao tận nơi trong giờ hành chính"
}
```

### 8. Cập nhật ngày giao — `PUT /api/orders/delivery`

```json
{
  "orderId": "1",
  "estimatedDeliveryDate": "2026-06-03T18:00:00",
  "actualDeliveryDate": "2026-06-03T15:30:00"
}
```

### 9. Cập nhật ghi chú admin — `PUT /api/orders/admin-notes`

```json
{
  "orderId": "1",
  "adminNotes": "Khách VIP, ưu tiên giao sớm"
}
```

### 10. Xác nhận đơn — `POST /api/orders/confirm`

Chỉ từ PENDING → CONFIRMED. Tự động reserve inventory.

```json
{
  "orderId": "1",
  "confirmationInfo": "Xác nhận qua điện thoại",
  "confirmedAt": "2026-06-01T10:35:00",
  "confirmedBy": "admin"
}
```

### 11. Hoàn thành đơn — `POST /api/orders/complete`

Chỉ từ DELIVERED → COMPLETED.

```json
{
  "orderId": "1",
  "completionInfo": "Khách đã nhận hàng",
  "completedAt": "2026-06-03T15:30:00"
}
```

---

## 12 → 13: Dashboard Lists

### 12. Đơn chờ xử lý — `GET /api/orders/pending`

Lấy các đơn có trạng thái PENDING, CONFIRMED.

### 13. Đơn đang giao — `GET /api/orders/in-progress`

Lấy các đơn có trạng thái PROCESSING, SHIPPED.

---

## 14. Thống kê đơn hàng — `GET /api/orders/statistics`

```
GET /api/orders/statistics?startDate=2026-06-01&endDate=2026-06-30
```

---

## 15. Chuyển trạng thái (Dashboard) — `POST /api/orders/transition`

Chuyển trạng thái qua dashboard. Hỗ trợ: PROCESSING, DELIVERED, READY_FOR_PICKUP, RETURNING, RETURNED, COMPLETED.

> **Lưu ý:** SHIPPING dùng endpoint riêng `/ship`.

### Request — PROCESSING → SHIPPING ❌ (không dùng transition)

```json
{
  "orderId": "1",
  "targetStatus": "SHIPPING",
  "note": ""
}
// → LỖI: "Không hỗ trợ: SHIPPING"
```

### Request — PROCESSING → READY_FOR_PICKUP

```json
{
  "orderId": "1",
  "targetStatus": "READY_FOR_PICKUP",
  "note": "Đã đóng gói xong, chờ khách đến lấy"
}
```

### Request — DELIVERED → COMPLETED

```json
{
  "orderId": "1",
  "targetStatus": "COMPLETED",
  "note": "Khách đã nhận hàng"
}
```

### Request — DELIVERED → RETURNING

```json
{
  "orderId": "1",
  "targetStatus": "RETURNING",
  "note": "Khách yêu cầu trả hàng do lỗi sản phẩm"
}
```

---

## 16. Giao hàng cho tài xế — `POST /api/orders/ship`

Chuyển PROCESSING → SHIPPING. Cần thông tin tài xế. Tự sinh delivery token.

### Request

```json
{
  "orderId": "1",
  "shipperId": "shipper-001",
  "shipperName": "Trần Văn B",
  "shipperPhone": "0987654321",
  "note": "Giao hàng trước 18h",
  "estimatedDeliveryDate": "2026-06-03T18:00:00",
  "pickupDeadline": "2026-06-02T10:00:00"
}
```

### Response

```json
{
  "status": "OK",
  "message": "Đã giao đơn cho tài xế. Gửi link cho shipper để bắt đầu giao hàng",
  "data": {
    "orderId": "1",
    "deliveryToken": "550e8400-e29b-41d4-a716-446655440000",
    "deliveryUrl": "/api/delivery/550e8400-e29b-41d4-a716-446655440000",
    "message": "Đã giao đơn cho tài xế. Gửi link cho shipper để bắt đầu giao hàng"
  }
}
```

---

## 17 → 18: Delivery PIN

### 17. Xem PIN — `GET /api/orders/delivery-pin/{orderNumber}`

```
GET /api/orders/delivery-pin/ORD-20260601-0001
```

### 18. Xóa PIN — `DELETE /api/orders/delivery-pin/{orderNumber}`

```
DELETE /api/orders/delivery-pin/ORD-20260601-0001
```

---

## Sơ đồ trạng thái (State Machine)

```
                         ┌──────────────────────────────┐
                         │           PENDING             │
                         └──────┬──────────────┬─────────┘
                                │              │
                          ┌─────▼─────┐  ┌─────▼──────────┐
                          │ CONFIRMED │  │ WAITING_PAYMENT │◄──── Online Payment
                          └─────┬─────┘  └─────┬──────────┘
                                │              │
                          ┌─────▼─────┐  ┌─────▼──────────┐
                          │ PROCESSING │  │     FAILED     │ (terminal)
                          └──┬─────┬──┘  └────────────────┘
                             │     │
                    ┌────────▼─┐ ┌─▼────────────┐
                    │ SHIPPING │ │ READY_FOR_PICKUP │
                    └──┬────┬──┘ └───┬───────────┘
                       │    │        │
                  ┌────▼─┐ ┌▼─────┐  │
                  │DELAYED│ │(giao)│  │ (khách lấy)
                  └──┬────┘ └──┬───┘  │
                     │         │      │
                     └────┬────┘      │
                          │           │
                    ┌─────▼───────────▼──┐
                    │     DELIVERED       │
                    └─────┬──────────────┘
                          │
                    ┌─────▼──────────┐
                    │   COMPLETED    │ (terminal)
                    └─────┬──────────┘
                          │
                    ┌─────▼──────────┐
                    │   RETURNING     │
                    └─────┬──────────┘
                          │
                    ┌─────▼──────────┐
                    │   RETURNED      │
                    └─────┬──────────┘
                          │
                    ┌─────▼──────────┐
                    │   REFUNDED      │ (terminal)
                    └────────────────┘

          CANCELLED ← (từ PENDING, WAITING_PAYMENT, CONFIRMED, PROCESSING)
```

### Bảng chuyển trạng thái cho phép

| Từ → Đến | Cho phép? |
|---|---|
| PENDING → CONFIRMED | ✅ |
| PENDING → WAITING_PAYMENT | ✅ |
| PENDING → CANCELLED | ✅ |
| WAITING_PAYMENT → CONFIRMED | ✅ |
| WAITING_PAYMENT → CANCELLED | ✅ |
| WAITING_PAYMENT → FAILED | ✅ |
| CONFIRMED → PROCESSING | ✅ |
| CONFIRMED → CANCELLED | ✅ |
| PROCESSING → SHIPPING | ✅ |
| PROCESSING → READY_FOR_PICKUP | ✅ |
| PROCESSING → CANCELLED | ✅ |
| SHIPPING → DELIVERED | ✅ |
| SHIPPING → DELAYED | ✅ |
| SHIPPING → RETURNING | ✅ |
| DELAYED → SHIPPING | ✅ |
| DELAYED → RETURNING | ✅ |
| READY_FOR_PICKUP → DELIVERED | ✅ |
| READY_FOR_PICKUP → RETURNING | ✅ |
| DELIVERED → COMPLETED | ✅ |
| DELIVERED → RETURNING | ✅ |
| RETURNING → RETURNED | ✅ |
| RETURNED → REFUNDED | ✅ |

---

## Request Fields — Tổng hợp

### CreateOrderRequest

| Field | Type | Bắt buộc | Ghi chú |
|---|---|---|---|
| `items[].attributesSku` | String | ⚠️ | Nếu không `is_from_cart` và không `booking_id` |
| `items[].quantity` | Integer | ⚠️ | Như trên |
| `is_from_cart` | boolean | ❌ | `true` = lấy từ giỏ hàng user đang login |
| `booking_id` | String | ❌ | Tạo từ booking |
| `address_id` | String | ✅ | Địa chỉ giao hàng |
| `discount_code` | String | ❌ | Mã giảm giá |
| `customer_notes` | String | ❌ | Ghi chú |
| `shipping_method` | String | ❌ | GHN, GHTK, etc. |
| `payment_method` | `COD`, `VNPAY`, `MOMO` | ❌ | `COD` → auto CONFIRMED+PROCESSING |
| `language` | String | ❌ | `vn`, `en` |
| `bank_code` | String | ❌ | Cho VNPay |

### OrderSearchRequest

| Field | Type | Ghi chú |
|---|---|---|
| `orderNumber` | String | Tìm theo mã đơn |
| `customerId` | String | Normalize tự động |
| `customerName` | String | LIKE theo tên |
| `customerEmail` | String | LIKE theo email |
| `customerPhone` | String | LIKE theo SĐT |
| `orderStatus` | OrderStatus | Lọc theo trạng thái |
| `startDate` / `endDate` | LocalDateTime | Khoảng thời gian |
| `minAmount` / `maxAmount` | Double | Khoảng tiền |
| `page` | Integer | Mặc định 0 |
| `size` | Integer | Mặc định 10 |
| `sortBy` | String | `auditInfo.createdAt`, `totalAmount` |
| `sortDirection` | `ASC` / `DESC` | |

### OrderDto Response Fields (User thấy)

| Field | Type | Ghi chú |
|---|---|---|
| `id` | Long | |
| `orderNumber` | String | Mã đơn |
| `status` | OrderStatus[] | Lịch sử trạng thái |
| `currentStatus` | OrderStatus | Trạng thái hiện tại |
| `currentStatusDescription` | String | Mô tả trạng thái |
| `customerName` | String | |
| `customerEmail` | String | |
| `customerPhone` | String | |
| `orderItems` | OrderItemDto[] | Chi tiết sản phẩm |
| `subtotal` | Double | Tổng tiền hàng |
| `discountAmount` | Double | Tiền giảm |
| `discountCode` | String | |
| `taxAmount` | Double | |
| `shippingFee` | Double | Phí ship |
| `totalAmount` | Double | Tổng thanh toán |
| `shippingInfo` | Address | |
| `customerNotes` | String | |
| `cancellationReason` | String | |
| `cancelledAt` | LocalDateTime | |
| `confirmedAt` | LocalDateTime | |
| `completedAt` | LocalDateTime | |

### OrderDto Response Fields (Admin thấy thêm)

| Field | Type | Ghi chú |
|---|---|---|
| `customerId` | Long | |
| `adminNotes` | String | Ghi chú nội bộ |
| `cancelledBy` | String | |
| `confirmedBy` | String | |
| `bookingId` | String | |
| `shoppingCartId` | String | |
| `auditInfo` | AuditInfoDto | Lịch sử kiểm toán |

### OrderItemDto

| Field | Type | Ghi chú |
|---|---|---|
| `id` | Long | |
| `orderId` | Long | |
| `productId` | Long | |
| `attributesId` | Long | |
| `productName` | String | |
| `productSku` | String | |
| `attributesSku` | String | |
| `variantOptions` | VariantOptionDto[] | |
| `quantity` | Integer | |
| `unitPrice` | Double | |
| `salePrice` | Double | |
| `discountAmount` | Double | |
| `discountPercentage` | Double | |
| `subtotal` | Double | |
| `notes` | String | |
| `imageUrl` | String | |
