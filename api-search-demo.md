# API Search — Demo Data

---

## 1️⃣ Product Search

**Endpoint:** `POST /api/merchandise/search-Product`

**Response type:** `Page<ProductDto>` (Spring Page — JSON trực tiếp, không wrap trong `Response`)

---

### 1.1 Request — Tìm kiếm đầy đủ tiêu chí

```json
{
  "keyword": "áo",
  "productIds": ["1", "2"],
  "categoryIds": ["1", "2"],
  "statuses": ["ACTIVE"],
  "minSoldQuantity": 10,
  "maxSoldQuantity": 1000,
  "minRevenue": 100000,
  "maxRevenue": 50000000,
  "minOrders": 1,
  "maxOrders": 500,
  "minView": 50,
  "minRating": 3.5,
  "minReviews": 5,
  "createdBy": "admin",
  "createdFrom": "2026-01-01T00:00:00",
  "createdTo": "2026-06-01T23:59:59",
  "updatedFrom": "2026-05-01T00:00:00",
  "updatedTo": "2026-06-01T23:59:59",
  "paging": {
    "page": 1,
    "size": 10,
    "orders": {
      "name": "asc",
      "totalSoldQuantity": "desc"
    }
  }
}
```

### 1.2 Request — Tìm kiếm đơn giản (chỉ keyword)

```json
{
  "keyword": "áo thun",
  "paging": {
    "page": 1,
    "size": 20
  }
}
```

### 1.3 Response

```json
{
  "content": [
    {
      "id": 1,
      "name": "Áo Thun Nam",
      "skuInfo": {
        "sku": "SP001"
      },
      "mediaItems": [
        {
          "url": "https://minio.example.com/products/ao-thun-1.jpg",
          "type": "IMAGE"
        }
      ],
      "status": "ACTIVE",
      "viewCount": 1200,
      "totalSoldQuantity": 150,
      "totalRevenue": 14950000.00,
      "discountPercent": 10.0,
      "discountStartDate": "2026-05-01T00:00:00",
      "discountEndDate": "2026-06-30T23:59:59",
      "categoryName": "Thời trang Nam"
    },
    {
      "id": 2,
      "name": "Áo Sơ Mi Trắng",
      "skuInfo": {
        "sku": "SP002"
      },
      "mediaItems": [
        {
          "url": "https://minio.example.com/products/ao-so-mi-1.jpg",
          "type": "IMAGE"
        }
      ],
      "status": "ACTIVE",
      "viewCount": 850,
      "totalSoldQuantity": 89,
      "totalRevenue": 11570000.00,
      "discountPercent": 0.0,
      "discountStartDate": null,
      "discountEndDate": null,
      "categoryName": "Thời trang Nam"
    }
  ],
  "page": 1,
  "size": 10,
  "totalElements": 2,
  "totalPages": 1
}
```

---

## 2️⃣ Attributes Search

**Endpoint:** `POST /api/merchandise/search-Attributes`

**Response type:** `Response<PagingResponse<AttributesDto>>`

---

### 2.1 Request — Tìm kiếm đầy đủ tiêu chí

```json
{
  "keyword": "size L",
  "productIds": ["1", "2", "3"],
  "skus": ["SP001-RED-L", "SP001-BLUE-M"],
  "statuses": ["IN_STOCK", "LOW_STOCK"],
  "minPrice": 50000,
  "maxPrice": 500000,
  "minSalePrice": 40000,
  "maxSalePrice": 450000,
  "minStockQuantity": 5,
  "maxStockQuantity": 200,
  "minSoldQuantity": 10,
  "maxSoldQuantity": 500,
  "minCostPrice": 30000,
  "maxCostPrice": 300000,
  "createdBy": "admin",
  "createdFrom": "2026-01-01T00:00:00",
  "createdTo": "2026-06-01T23:59:59",
  "updatedFrom": "2026-05-01T00:00:00",
  "updatedTo": "2026-06-01T23:59:59",
  "paging": {
    "page": 1,
    "size": 10,
    "orders": {
      "price": "asc",
      "soldQuantity": "desc"
    }
  }
}
```

### 2.2 Request — Tìm kiếm đơn giản (chỉ keyword)

```json
{
  "keyword": "áo thun",
  "paging": {
    "page": 1,
    "size": 20
  }
}
```

### 2.3 Response

```json
{
  "status": "OK",
  "message": "Thành công",
  "data": {
    "contents": [
      {
        "id": 1,
        "name": "Áo Thun Nam - Size L - Đỏ",
        "sku": {
          "sku": "SP001-RED-L"
        },
        "price": 199000.0,
        "salePrice": 179000.0,
        "stockQuantity": 50,
        "statusProduct": "IN_STOCK",
        "variantOptions": [
          { "name": "Màu sắc", "value": "Đỏ" },
          { "name": "Kích cỡ", "value": "L" }
        ],
        "keywords": ["áo thun", "thời trang nam", "size L"],
        "specifications": [
          {
            "groupName": "Chất liệu",
            "specs": [
              { "name": "Chất vải", "value": "Cotton 100%" },
              { "name": "Kiểu dáng", "value": "Regular fit" }
            ]
          },
          {
            "groupName": "Kích thước",
            "specs": [
              { "name": "Chiều dài", "value": "70cm" },
              { "name": "Rộng ngực", "value": "52cm" }
            ]
          }
        ],
        "promotions": [
          {
            "name": "Giảm 10%",
            "discountPercent": 10,
            "startDate": "2026-05-01T00:00:00",
            "endDate": "2026-06-30T23:59:59"
          }
        ],
        "auditInfo": {
          "createdBy": "admin",
          "createdAt": "2026-01-15T08:30:00",
          "updatedBy": "admin",
          "updatedAt": "2026-05-20T14:22:00"
        },
        "product": {
          "id": 1,
          "name": "Áo Thun Nam",
          "skuInfo": { "sku": "SP001" },
          "status": "ACTIVE"
        }
      },
      {
        "id": 2,
        "name": "Áo Thun Nam - Size M - Xanh",
        "sku": {
          "sku": "SP001-BLUE-M"
        },
        "price": 199000.0,
        "salePrice": 179000.0,
        "stockQuantity": 5,
        "statusProduct": "LOW_STOCK",
        "variantOptions": [
          { "name": "Màu sắc", "value": "Xanh" },
          { "name": "Kích cỡ", "value": "M" }
        ],
        "keywords": ["áo thun", "thời trang nam", "size M"],
        "specifications": [
          {
            "groupName": "Chất liệu",
            "specs": [
              { "name": "Chất vải", "value": "Cotton 100%" },
              { "name": "Kiểu dáng", "value": "Regular fit" }
            ]
          }
        ],
        "promotions": [],
        "auditInfo": {
          "createdBy": "admin",
          "createdAt": "2026-01-15T08:30:00",
          "updatedBy": "admin",
          "updatedAt": "2026-06-01T09:00:00"
        },
        "product": {
          "id": 1,
          "name": "Áo Thun Nam",
          "skuInfo": { "sku": "SP001" },
          "status": "ACTIVE"
        }
      }
    ],
    "paging": {
      "pageNumber": 1,
      "pageSize": 10,
      "totalPage": 1,
      "totalRecord": 2
    }
  }
}
```

---

## Bảng so sánh search fields

| Field | Product (`GetProductRequest`) | Attributes (`AttributesSearchRequest`) |
|---|---|---|
| `keyword` | ✅ tìm theo `name` | ✅ tìm theo `name` |
| `statuses` | ✅ `ACTIVE` / `INACTIVE` | ✅ `IN_STOCK` / `OUT_OF_STOCK` / `LOW_STOCK` |
| `productIds` | ✅ | ✅ |
| `categoryIds` | ✅ | ❌ |
| `skus` | ❌ | ✅ |
| `minPrice` / `maxPrice` | ❌ | ✅ |
| `minSalePrice` / `maxSalePrice` | ❌ | ✅ |
| `minStockQuantity` / `maxStockQuantity` | ❌ | ✅ |
| `minSoldQuantity` / `maxSoldQuantity` | ✅ | ✅ |
| `minRevenue` / `maxRevenue` | ✅ | ❌ |
| `minCostPrice` / `maxCostPrice` | ❌ | ✅ |
| `minOrders` / `maxOrders` | ✅ | ❌ |
| `minView` | ✅ | ❌ |
| `minRating` | ✅ | ❌ |
| `minReviews` | ✅ | ❌ |
| `createdBy` | ✅ | ✅ |
| `createdFrom` / `createdTo` | ✅ | ✅ |
| `updatedFrom` / `updatedTo` | ✅ | ✅ |
| `paging` + `orders` | ✅ | ✅ |
