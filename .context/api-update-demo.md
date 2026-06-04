# API Update — Demo Data

---

## 1️⃣ Product Update

**Endpoint:** `PUT /api/merchandise/update-Product`

**Response type:** `Response<?>` — message dạng string

---

### 1.1 Request — Cập nhật đầy đủ

```json
{
  "id": "1",
  "name": "Áo Thun Nam Cao Cấp",
  "categoryId": "2",
  "status": "ACTIVE",
  "discountPercent": 15.0,
  "discountStartDate": "2026-06-01T00:00:00",
  "discountEndDate": "2026-07-31T23:59:59"
}
```

### 1.2 Request — Cập nhật 1 field (tên)

```json
{
  "id": "1",
  "name": "Áo Thun Nam Siêu Nhẹ"
}
```

### 1.3 Request — Cập nhật giá + xoá giảm giá

```json
{
  "id": "1",
  "discountPercent": 0.0,
  "discountStartDate": null,
  "discountEndDate": null
}
```

### 1.4 Response — Thành công

```json
{
  "status": "OK",
  "message": "Cập nhật sản phẩm thành công.",
  "data": null
}
```

### 1.5 Response — Lỗi (sản phẩm không tồn tại)

```json
{
  "status": "NOT_FOUND",
  "message": "Sản phẩm không tồn tại.",
  "data": null,
  "errorCode": "PRODUCT_NOT_FOUND"
}
```

### Request Fields

| Field | Type | Bắt buộc | Ghi chú |
|---|---|---|---|
| `id` | String | ✅ | ID sản phẩm, được normalize (uppercase + remove dashes) |
| `name` | String | ❌ | Tên mới |
| `categoryId` | String | ❌ | ID danh mục mới, được normalize |
| `status` | `ACTIVE` / `INACTIVE` / `LOCKED` | ❌ | Trạng thái mới |
| `discountPercent` | Double | ❌ | % giảm giá (0 = xoá giảm giá) |
| `discountStartDate` | LocalDateTime | ❌ | Ngày bắt đầu giảm giá (`null` = xoá) |
| `discountEndDate` | LocalDateTime | ❌ | Ngày kết thúc giảm giá (`null` = xoá) |

---

## 2️⃣ Attributes Update

**Endpoint:** `PUT /api/merchandise/update-Attributes`

**Response type:** `Response<?>` — message dạng string

**Validation:** `salePrice` không được lớn hơn `price`, `stockQuantity` không được âm.

---

### 2.1 Request — Cập nhật đầy đủ

```json
{
  "id": "1",
  "name": "Áo Thun Nam - Size L - Đen",
  "price": 250000,
  "sale_price": 199000,
  "stock_quantity": 100,
  "statusProduct": "IN_STOCK",
  "variantOptions": [
    { "name": "Màu sắc", "values": ["Đen"] },
    { "name": "Kích cỡ", "values": ["L"] }
  ],
  "keywords": ["áo thun", "thời trang nam", "size L", "màu đen"],
  "specifications": [
    {
      "groupName": "Chất liệu",
      "specifications": [
        { "specName": "Chất vải", "specValue": "Cotton 100%" },
        { "specName": "Kiểu dáng", "specValue": "Regular fit" }
      ]
    },
    {
      "groupName": "Kích thước",
      "specifications": [
        { "specName": "Chiều dài", "specValue": "72cm" },
        { "specName": "Rộng ngực", "specValue": "54cm" }
      ]
    }
  ],
  "promotions": [
    {
      "name": "Giảm 20%",
      "discountPercent": 20,
      "startDate": "2026-06-01T00:00:00",
      "endDate": "2026-07-31T23:59:59"
    }
  ]
}
```

### 2.2 Request — Cập nhật giá và tồn kho

```json
{
  "id": "1",
  "price": 299000,
  "sale_price": 249000,
  "stock_quantity": 50
}
```

### 2.3 Request — Cập nhật tên và trạng thái

```json
{
  "id": "1",
  "name": "Áo Thun Nam - Size XL - Xám",
  "statusProduct": "OUT_OF_STOCK"
}
```

### 2.4 Response — Thành công

```json
{
  "status": "OK",
  "message": "Đã cập nhật thành công.",
  "data": null
}
```

### 2.5 Response — Lỗi (giá khuyến mãi > giá gốc)

```json
{
  "status": "BAD_REQUEST",
  "message": "Giá khuyến mãi không thể lớn hơn giá gốc.",
  "data": null,
  "errorCode": "INVALID_PRICE"
}
```

### 2.6 Response — Lỗi (attributes không tồn tại)

```json
{
  "status": "NOT_FOUND",
  "message": "Thuộc tính sản phẩm không tồn tại.",
  "data": null,
  "errorCode": "ATTRIBUTES_NOT_FOUND"
}
```

### Request Fields

| Field | Type | Bắt buộc | Ghi chú |
|---|---|---|---|
| `id` | String | ✅ | ID attributes, được normalize |
| `name` | String | ❌ | Tên mới (max 255 ký tự) |
| `price` | Double | ❌ | Giá gốc (> 0) |
| `sale_price` | Double | ❌ | Giá khuyến mãi (> 0, ≤ `price`) |
| `stock_quantity` | Integer | ❌ | Số lượng tồn kho (≥ 0) |
| `statusProduct` | `IN_STOCK` / `OUT_OF_STOCK` / `LOW_STOCK` / `NOT_ACTIVE` | ❌ | Trạng thái tồn kho |
| `variantOptions` | Array | ❌ | Danh sách biến thể (name + values) |
| `keywords` | Set | ❌ | Từ khóa SEO (max 20 từ, mỗi từ max 50 ký tự) |
| `specifications` | Array | ❌ | Thông số kỹ thuật (max 50 groups) |
| `promotions` | Array | ❌ | Khuyến mãi (max 10 items) |

### Nested Objects

**variantOptions[]:**
```json
{
  "name": "Màu sắc",
  "values": ["Đen", "Trắng"]
}
```

**specifications[]:**
```json
{
  "groupName": "Chất liệu",
  "specifications": [
    { "specName": "Chất vải", "specValue": "Cotton" }
  ]
}
```

**promotions[]:**
```json
{
  "name": "Giảm 20%",
  "discountPercent": 20,
  "startDate": "2026-06-01T00:00:00",
  "endDate": "2026-07-31T23:59:59"
}
```
