# Explorer Analysis — ERP Spring Boot Search Functionality
**Generated**: 2026-07-15T00:35:xx+07:00  
**Working directory**: `.agents/orchestrator_es_test/explorer_1/`

---

## Summary

The Spring Boot ERP project has **migrated its product search from Elasticsearch to JPA Specification**. The `spring-boot-starter-data-elasticsearch` dependency is still present in `pom.xml` but ProductService explicitly comments that ES is no longer used — search now runs through JPA Specification directly against the database.

---

## 1. Observation

### 1.1 pom.xml — Elasticsearch dependency

**File**: `pom.xml` (line 101–104)  
**Content**:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
</dependency>
```
Also at lines 237–240 (test scope):
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>elasticsearch</artifactId>
    <scope>test</scope>
</dependency>
```

**Answer Q1**: ✅ YES — `spring-boot-starter-data-elasticsearch` is still present in pom.xml (line 103), along with `testcontainers:elasticsearch` for test scope. However it is NOT actively used by the search feature.

---

### 1.2 MerchandiseController — Search Endpoint

**File**: `src/main/java/com/anno/ERP_SpringBoot_Experiment/web/rest/MerchandiseController.java`  
**Line 38–40**:
```java
@PostMapping("/search-Product")
@ResponseStatus(HttpStatus.OK)
Page<ProductDto> searchProduct(@Valid @RequestBody GetProductRequest request);
```
- Base path: `@RequestMapping("/api/merchandise")` (line 21)
- **Full endpoint**: `POST /api/merchandise/search-Product`
- Returns `Page<ProductDto>` (Spring Data pagination wrapper)

---

### 1.3 GetProductRequest DTO (Request Body Format)

**File**: `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/request/GetProductRequest.java`

```java
@Data
public class GetProductRequest {
    private String keyword;           // text search on product name (CONTAINS)
    private String categoryId;        // (not wired into spec builder currently)
    private String createdBy;         // exact match on createdBy field
    private List<String> productIds;  // (not wired into spec builder)
    private List<String> skus;        // (not wired into spec builder)
    private List<String> statuses;    // ["ACTIVE","LOCKED"] — IN filter
    private List<String> categoryIds; // (not wired into spec builder)
    private Integer minSoldQuantity;
    private Integer maxSoldQuantity;
    private Double minRevenue;
    private Double maxRevenue;
    private Integer minOrders;
    private Integer maxOrders;
    private Integer minView;
    private Double minRating;
    private Integer minReviews;
    private LocalDateTime createdFrom;    // ISO-8601 datetime
    private LocalDateTime createdTo;
    private LocalDateTime updatedFrom;    // (not wired into spec builder)
    private LocalDateTime updatedTo;
    private PagingRequest paging = new PagingRequest(); // default page=1, size=10
}
```

**PagingRequest** (`PagingRequest.java`):
```java
private int page = 1;
private int size = 10;
private Map<String, String> orders = new HashMap<>();  // field -> "ASC"/"DESC"
```

---

### 1.4 ProductService.searchProduct() — JPA vs ES

**File**: `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/Merchandise/ProductService.java`  
**Lines 63–64** (explicit comment):
```java
// NOTE: ProductElasticSearchService được giữ lại nhưng không còn inject vào đây.
//       Tìm kiếm hiện dùng JPA Specification trực tiếp từ DB.
```
**Lines 137–145** (`searchProducts` method):
```java
public Page<ProductDto> searchProducts(@NonNull GetProductRequest request) {
    Pageable pageable = (request.getPaging() != null) ? request.getPaging().pageable() : PageRequest.of(0, 10);
    Specification<Product> spec = buildProductSpecification(request);
    Page<Product> productPage = productRepository.findAll(spec, pageable);
    List<ProductDto> content = productPage.getContent().stream()
            .map(productMapper::toDto)
            .collect(Collectors.toList());
    return new PageImpl<>(content, pageable, productPage.getTotalElements());
}
```

**Answer Q3**: ✅ Uses **JPA Specification** — NOT Elasticsearch.

---

### 1.5 JPA Specification Classes

Located in `src/main/java/com/anno/ERP_SpringBoot_Experiment/repository/specification/`:

| Class | Purpose |
|-------|---------|
| `ProductSpecification.java` | Older builder-style spec (name, sku, status LIKE filters) — *appears to be an older version, not used by ProductService* |
| `SpecificationBuilder.java` | Generic `SpecificationBuilder<T>` — **this is what ProductService uses** |
| `AttributesSpecification.java` | For attributes search |
| `CategorySpecification.java` | For category search |
| `OrderSpecification.java` | For order search |
| `UserSpecification.java` | For user search |

**`SpecificationBuilder<T>`** supports operations: `EQUALITY`, `NEGATION`, `GREATER_THAN`, `LESS_THAN`, `LIKE`, `STARTS_WITH`, `ENDS_WITH`, `CONTAINS`, `IN`.

**Answer Q4**: ✅ YES — `ProductSpecification.java` at the path above, plus `SpecificationBuilder.java` (the one actually used by `ProductService`).

---

### 1.6 Auth Endpoint

**File**: `src/main/java/com/anno/ERP_SpringBoot_Experiment/web/rest/AuthController.java`  
**Lines 25–27**:
```java
@PostMapping("/login")
@ResponseStatus(HttpStatus.OK)
Response<AuthResponse> login(@Valid @RequestBody final UserLoginRequest body);
```
- Base path: `@RequestMapping("/api/auth")` (line 22)
- **Full endpoint**: `POST /api/auth/login`

**UserLoginRequest** fields:
```java
String usernameOrEmail;  // @NotBlank, @Size(min=3, max=50)
String password;         // @NotBlank
DeviceInfo deviceInfo;   // @NotNull — see below
```

**DeviceInfo** fields (all optional except it must be present):
```java
String deviceType;
String osName;
String osVersion;
String browserName;
String browserVersion;
Integer screenWidth;
Integer screenHeight;
String userAgent;
String ipAddress;
String language;
String timeZone;
String deviceId;
```

---

### 1.7 ProductDto Response Fields

**File**: `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/ProductDto.java`

The search returns `Page<ProductDto>` containing:
```json
{
  "content": [
    {
      "id": 1,
      "name": "Product Name",
      "skuInfo": { "sku": "prd-xx-..." },
      "mediaItems": [],
      "status": "ACTIVE",
      "viewCount": 0,
      "totalSoldQuantity": 0,
      "totalRevenue": 0.00,
      "discountPercent": null,
      "discountStartDate": null,
      "discountEndDate": null,
      "categoryName": "Category Name"
    }
  ],
  "pageable": { ... },
  "totalElements": N,
  "totalPages": N,
  ...
}
```

---

## 2. Logic Chain

1. **pom.xml line 103** confirms `spring-boot-starter-data-elasticsearch` is declared.
2. **ProductService.java lines 63-64** explicitly states: "search now uses JPA Specification directly from DB."
3. **ProductService.java line 139** calls `buildProductSpecification(request)` which uses `SpecificationBuilder<Product>`.
4. **SpecificationBuilder.java** is a pure JPA Criteria API class — no ES imports.
5. **MerchandiseController.java line 38** maps `POST /search-Product` under base `/api/merchandise`.
6. **GetProductRequest.java** defines all filterable fields; `PagingRequest` handles pagination.
7. **AuthController.java line 25** maps `POST /login` under base `/api/auth`.
8. **UserLoginRequest.java** requires `usernameOrEmail`, `password`, and `deviceInfo` object.

---

## 3. Caveats

- `categoryId`, `productIds`, `skus`, `categoryIds`, `updatedFrom`, `updatedTo` fields in `GetProductRequest` are **declared in the DTO but NOT currently wired** into `buildProductSpecification()` — they will be silently ignored.
- The older `ProductSpecification.java` exists but appears to be a legacy class not referenced by `ProductService.searchProducts()`.
- The `deviceInfo` in login is `@NotNull` — the request **will fail** with validation error if omitted. Fields within `DeviceInfo` don't have `@NotBlank`, so providing an empty object `{}` may work.
- Server port is unknown (check `application.properties`/`application.yml` — default Spring Boot is 8080).

---

## 4. Conclusion

| Question | Answer |
|---------|--------|
| Q1: ES in pom.xml? | ✅ YES — line 103, also testcontainers ES at line 238 |
| Q2: Request body for search? | See section 1.3 — keyword, statuses, min/max numerics, paging |
| Q3: ProductService uses ES or JPA? | **JPA Specification** (explicitly commented out ES) |
| Q4: Specification classes? | `ProductSpecification.java` + `SpecificationBuilder.java` in `repository/specification/` |
| Q5: Auth endpoint? | `POST /api/auth/login` with `usernameOrEmail`, `password`, `deviceInfo` |
| Q6: Exact controller mapping? | `POST /api/merchandise/search-Product` |
| Q7: Response fields? | id, name, skuInfo, mediaItems, status, viewCount, totalSoldQuantity, totalRevenue, discountPercent, discountStartDate, discountEndDate, categoryName |

---

## 5. Exact Commands for Caller

### Authenticate (get JWT):
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "usernameOrEmail": "admin",
    "password": "your_password",
    "deviceInfo": {
      "deviceType": "DESKTOP",
      "osName": "Linux",
      "osVersion": "6.0",
      "browserName": "curl",
      "browserVersion": "1.0",
      "userAgent": "curl/7.88",
      "deviceId": "test-device-001"
    }
  }'
```

### Search products:
```bash
curl -X POST http://localhost:8080/api/merchandise/search-Product \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{
    "keyword": "laptop",
    "statuses": ["ACTIVE"],
    "createdBy": null,
    "minSoldQuantity": null,
    "maxSoldQuantity": null,
    "minRevenue": null,
    "maxRevenue": null,
    "minOrders": null,
    "maxOrders": null,
    "minView": null,
    "minRating": null,
    "minReviews": null,
    "createdFrom": null,
    "createdTo": null,
    "paging": {
      "page": 1,
      "size": 10,
      "orders": {}
    }
  }'
```

---

## 6. Verification Method

```bash
# Verify ES dependency presence
grep -n "elasticsearch" /home/ddicgegd/Projects/erp_springboot-experiment/pom.xml

# Verify search uses JPA
grep -n "Specification\|ElasticSearch\|elasticsearch" \
  /home/ddicgegd/Projects/erp_springboot-experiment/src/main/java/com/anno/ERP_SpringBoot_Experiment/service/Merchandise/ProductService.java

# Verify controller mapping
grep -n "search-Product\|searchProduct" \
  /home/ddicgegd/Projects/erp_springboot-experiment/src/main/java/com/anno/ERP_SpringBoot_Experiment/web/rest/MerchandiseController.java

# Verify auth login endpoint
grep -n "login\|/api/auth" \
  /home/ddicgegd/Projects/erp_springboot-experiment/src/main/java/com/anno/ERP_SpringBoot_Experiment/web/rest/AuthController.java
```
