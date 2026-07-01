# Merchandise Module Knowledge Document

## Overview

The **Merchandise** group manages product catalog operations including attributes, categories, products, product images, and shopping carts. It provides CRUD operations, search/filtering capabilities, caching strategies, and integrations with external services (MinIO, Redis, Elasticsearch).

---

## File: AttributesService.java

**Purpose:** Manages product attributes (variants/SKUs) including creation, updates, deletion, and retrieval with advanced search and caching.

### Key Classes/Functions

- **`create(CreateAttributesRequest request): Response<List<AttributesDto>>`**
  - Creates multiple attributes for a product
  - Generates unique SKUs, maps variant options, specifications, promotions
  - Parameters: product SKU, name, price, sale price, variant options, keywords, specifications, promotions, status
  - Returns: success message with created count
  - Triggers cache eviction and Redis invalidation

- **`update(UpdateAttributesRequest request): Response<?>`**
  - Updates single attribute with validation for prices (non-negative, sale ≤ price)
  - Supports conditional updates of name, pricing, variants, keywords, specifications, promotions, status
  - Logs update entry with username
  - Returns: success message

- **`delete(List<String> ids): Response<?>`**
  - Soft-deletes attributes (marked for 30-day deletion)
  - Bulk operation with validation
  - Returns: no content response

- **`deleteByProduct(String productId): Response<?>`**
  - Deletes all attributes for a product
  - Soft-delete with audit trail

- **`search(AttributesSearchRequest request): Page<AttributesDto>`**
  - Dynamic search using `SpecificationBuilder` with criteria: keyword, IDs, product IDs, SKUs, statuses, price ranges, sold quantities, cost prices, date ranges
  - Supports pagination
  - Returns: paginated results

- **`getAttributesByIds(List<Long> ids): Response<List<AttributesDto>>`**
  - Retrieves attributes with **two-level caching**: Spring Cache (RAM) + fallback to DB query
  - Uses `CacheUtils.getAll()` for cache-aware batch loading
  - Returns: list of DTOs preserving order

- **`getAttributesBySkus(List<String> skus): Response<List<AttributesDto>>`**
  - Maps SKUs → IDs via DB query, then loads via `getAttributesByIds()`
  - Returns: ordered DTOs matching input SKU list

- **`searchAttributesIds(AttributesSearchRequest request): List<Long>`**
  - Low-level search returning only IDs using Criteria API
  - Applies pagination to result set

- **`getAttributesByProductId(String productId): List<AttributesDto>`**
  - **`@Cacheable` on method** with key `#productId`
  - Cache-first retrieval; logs cache misses
  - Used for lazy-loading product details

### Dependencies

- **Repositories:** `AttributesRepository`, `ProductRepository`
- **Mappers:** `SpecificationMapper`, `PromotionMapper`, `AttributesMapper`
- **Services:** `RedisProducerService`
- **Utilities:** `SecurityUtil`, `CacheUtils`
- **Infrastructure:** `EntityManager`, `CacheManager`, `CacheEvictAfterCommit`

### Data Flow

```
create() → validate product → build Attributes entities → save → 
  → clear cache + send Redis eviction message
  
update() → fetch existing → validate inputs → update fields → 
  → save + audit log → clear cache + Redis message
  
search() → build criteria → execute Criteria Query → getAttributesByIds() → 
  → resolve from cache or DB
  
getAttributesByIds() → CacheUtils.getAll(missingIds callback) → 
  → DB query for missing IDs → merge results
```

### Design Patterns

- **Repository Pattern:** Data access via Spring Data JPA
- **Mapper Pattern:** Entity ↔ DTO conversion
- **Builder Pattern:** `Attributes.builder()`, `SkuInfo.builder()`
- **Strategy Pattern:** `SpecificationBuilder` for dynamic query construction
- **Cache-Aside:** Two-tier caching (Spring Cache + DB fallback)
- **Soft Delete:** Mark-for-deletion with cleanup after 30 days

### Public API

- CRUD: `create()`, `update()`, `delete()`, `deleteByProduct()`
- Search: `search()`, `searchAttributesIds()`
- Retrieval: `getAttributesByIds()`, `getAttributesBySkus()`, `getAttributesByProductId()`

### Configuration

- **Cache Name:** `"attributes"` (CacheConfig.CACHE_ATTRIBUTES)
- **Entity Manager:** JPA Criteria API for dynamic queries
- **Redis:** Async cache invalidation via `RedisProducerService`

---

## File: CategoryService.java

**Purpose:** Manages product categories with CRUD, search, existence checking, and SKU-based retrieval.

### Key Classes/Functions

- **`create(String name): Response<?>`**
  - Validates uniqueness (throws if exists)
  - Generates category SKU with prefix "ctgr-"
  - Records creation timestamp and username
  - Returns: success message

- **`update(UpdateCategoryRequest request): Response<?>`**
  - Updates category name
  - Records update entry with username
  - Triggers cache eviction on transaction commit
  - Returns: success message

- **`delete(List<String> ids): Response<?>`**
  - Soft-deletes categories by IDs
  - Returns: no content

- **`search(CategorySearchRequest request): Page<CategoryDto>`**
  - Filters: names, SKUs, IDs, keyword, creator, date ranges
  - Runs `deleteAllExpiredCategories()` before search (30-day cleanup)
  - Uses `SpecificationBuilder` with `SearchCriteria`
  - Returns: paginated DTO results

- **`getCategoriesByIds(List<Long> ids): Response<List<CategoryDto>>`**
  - Cache-aware batch retrieval using `CacheUtils.getAll()`
  - Returns: ordered DTOs

- **`isExiting(String name): CategoryExitingResponse`**
  - Checks if category exists by name
  - Returns: `{id, isExiting}` response object

- **`getCategoriesBySkus(List<String> skus): Response<List<CategoryDto>>`**
  - Maps SKUs → IDs via DB query
  - Loads via `getCategoriesByIds()`
  - Returns: ordered DTOs matching input SKU order

### Dependencies

- **Repositories:** `CategoryRepository`
- **Mappers:** `CategoryMapper`
- **Utilities:** `SecurityUtil`
- **Infrastructure:** `CacheManager`, `CacheEvictAfterCommit`

### Data Flow

```
create() → check existence → save with SKU → cache evict
update() → fetch → modify → save → cache evict on commit
search() → expire old soft-deletes → build criteria → find → map to DTO
getCategoriesByIds() → resolve from cache or DB
```

### Design Patterns

- **Repository Pattern**
- **Mapper Pattern**
- **Cache-Aside**
- **Soft Delete with TTL:** 30-day cleanup via `deleteAllExpiredCategories()`

### Public API

- CRUD: `create()`, `update()`, `delete()`
- Search: `search()`
- Retrieval: `getCategoriesByIds()`, `getCategoriesBySkus()`, `isExiting()`

### Configuration

- **Cache Names:** `"categoryDetails"` (CacheConfig.CACHE_CATEGORY_DETAILS)
- **Soft Delete TTL:** 30 days

---

## File: Helper.java

**Purpose:** Shared utilities for merchandise operations including cart management, UUID conversion, and data transformation.

### Key Classes/Functions

- **`handleAddItem(ShoppingCart cart, String sku, int quantity, Attributes attributes): void`**
  - Delegates to `cart.addItem(sku, quantity)`
  - Logs operation

- **`handleDecreaseItem(ShoppingCart cart, String sku, int quantityToDecrease): void`**
  - Finds item by SKU
  - If new quantity ≤ 0: removes item
  - Otherwise: updates quantity
  - Throws if SKU not in cart

- **`recalculateAndUpdateTotals(ShoppingCart cart): void`**
  - Queries DB for all SKUs in cart via `attributesRepository.findAllBySku_skuIn(skus)`
  - Calculates: total items, total price (price × qty), total discount (price - salePrice × qty)
  - Updates cart via `updateTotals()`

- **`createNewCart(User user): ShoppingCart`**
  - Initializes new cart with user, audit info, creation timestamp/username

- **`toDto(ShoppingCart cart): ShoppingCartDto`**
  - Transforms entity → DTO, mapping cart items and totals
  - Handles null cart

- **`convertStringToUUID(String id): UUID`**
  - Accepts 36-char UUID format (with hyphens) or 32-char hex string
  - Validates format and reconstructs as standard UUID
  - Throws on invalid input

- **`generateKey(): String`**
  - Generates random 5-character alphanumeric string for media items

- **`filterBlank(List<String> list): List<String>`**
  - Filters null and blank strings from list

### Dependencies

- **Repositories:** `AttributesRepository`
- **Entities:** `Attributes`, `CartItem`, `ShoppingCart`, `User`
- **DTOs:** `ShoppingCartDto`

### Design Patterns

- **Helper/Utility Component**
- **Facade:** Simplifies cart operations for calling services

### Public API

- Cart operations: `handleAddItem()`, `handleDecreaseItem()`, `recalculateAndUpdateTotals()`, `createNewCart()`, `toDto()`
- Conversion: `convertStringToUUID()`, `generateKey()`, `filterBlank()`

---

## File: ProductImageService.java

**Purpose:** Manages product media (images) with upload, delete, and replace operations via MinIO storage.

### Key Classes/Functions

- **`uploadImages(List<MultipartFile> images): List<MediaItem>`**
  - Uploads each file to MinIO via `minioService.uploadFile()`
  - Generates 5-char key per image via `helper.generateKey()`
  - On exception: rolls back uploaded files
  - Returns: list of `MediaItem` objects (key + URL)

- **`addProductImages(String productId, List<MultipartFile> images): Response<?>`**
  - Fetches product by ID
  - Validates non-empty image list
  - Calls `uploadImages()` and appends to product's media items
  - Records audit entry and saves
  - Returns: updated product DTO

- **`deleteProductImage(String productId, String imageKey): Response<?>`**
  - Finds product and media item by key
  - Deletes from MinIO (swallows errors)
  - Removes from product's media list
  - Records audit and saves
  - Returns: updated product DTO

- **`replaceProductImages(String productId, List<MultipartFile> images): Response<?>`**
  - Fetches product
  - Deletes all existing media from MinIO
  - Clears media list
  - Uploads new images
  - Records audit
  - Returns: updated product DTO

- **`getProductImage(String imageName): byte[]`**
  - Retrieves file bytes from MinIO
  - Throws BusinessException on error

### Dependencies

- **Repositories:** `ProductRepository`
- **Services:** `MinioService`, `Helper`
- **Mappers:** `ProductMapper`
- **Utilities:** `SecurityUtil`
- **Models:** `MediaItem`

### Data Flow

```
addProductImages() → uploadImages() → MinIO + generate keys → 
  → append to product → save → record audit

deleteProductImage() → find item → MinIO delete → remove from list → save

replaceProductImages() → MinIO delete all → clear list → uploadImages() → save
```

### Design Patterns

- **Repository Pattern**
- **Service Delegation:** Delegates file ops to `MinioService`
- **Transactional Safety:** Transaction wraps file operations

### Public API

- Add: `addProductImages()`
- Delete: `deleteProductImage()`
- Replace: `replaceProductImages()`
- Retrieve: `getProductImage()`

### Configuration

- **Storage Backend:** MinIO (object storage)
- **Media Key Generation:** 5-char random alphanumeric

---

## File: ProductService.java

**Purpose:** Core product lifecycle management with CRUD, search, analytics updates, and cache synchronization.

### Key Classes/Functions

- **`addProduct(CreateProductRequest request): Response<?>`**
  - Validates category by SKU
  - Creates product with name, category, generated SKU, status
  - Publishes `ProductCreated` event
  - Returns: success message

- **`updateProduct(UpdateProductRequest request): Response<?>`**
  - Validates product existence
  - Optionally updates category
  - Maps request fields via `ProductMapper.updateFromRequest()`
  - Records audit entry
  - Triggers: Redis cache eviction, Elasticsearch update, cache sync marking
  - Returns: success message

- **`deleteProduct(List<Long> ids): Response<?>`**
  - Soft-deletes products
  - Triggers: Redis eviction per product, `ProductDeleted` events
  - Returns: no content

- **`searchProducts(GetProductRequest request): Page<ProductDto>`**
  - Uses Elasticsearch (`productElasticSearchService.searchProductIds()`) to find matching IDs
  - Loads DTOs via `getProductsByIds()`
  - Returns: paginated results

- **`searchProductIds(GetProductRequest request): List<Long>`**
  - Builds `SearchCriteria` from request (keyword, status, category, price ranges, dates, ratings, reviews, etc.)
  - Executes via Criteria API with `CriteriaBuilder`
  - Applies pagination
  - Returns: ID list only

- **`isExiting(String name): ProductIsExiting`**
  - Checks if product exists by name

- **`getProductById(Long id): ProductDto`**
  - **`@Cacheable` with key `#id`**
  - Lazy-loads via `findByIdWithDetails()` (JOIN FETCH)
  - Logs cache misses
  - Returns: DTO

- **`viewCount(String productId): void`**
  - Increments view count via repository update

- **`totalSoldQuantity(String productId): void`**
  - Increments sold quantity by 1

- **`totalRevenue(String productId, double price): void`**
  - Adds price to total revenue

- **`getProductsByIds(List<Long> ids): Response<List<ProductDto>>`**
  - Dual-layer cache (Caffeine + fallback):
    - Tries `cacheManager.getCache("productDetails")` → Caffeine batch get
    - Falls back to direct DB query if cache unavailable
  - Returns: DTOs preserving input order

- **`getProductsBySkus(List<String> skus): Response<List<ProductDto>>`**
  - Maps SKUs → IDs via DB query
  - Loads via `getProductsByIds()`
  - Returns: ordered DTOs

### Dependencies

- **Repositories:** `ProductRepository`, `CategoryRepository`
- **Mappers:** `ProductMapper`
- **Services:** `MinioService`, `CacheSyncService`, `RedisProducerService`, `ProductElasticSearchService`
- **Event Producer:** `ProductEventProducer`
- **Utilities:** `SecurityUtil`
- **Infrastructure:** `CacheManager`, `EntityManager`

### Data Flow

```
addProduct() → validate category → create with SKU → 
  → save → publish ProductCreated event

updateProduct() → fetch → map fields → save → 
  → Redis evict + Elasticsearch update + mark cache dirty

searchProducts() → Elasticsearch search (IDs) → getProductsByIds() → 
  → resolve from cache or DB → paginate

getProductsByIds() → Caffeine cache batch get + DB fallback

getProductById() → @Cacheable (first miss → JOIN FETCH from DB)
```

### Design Patterns

- **Repository Pattern**
- **Mapper Pattern**
- **Event-Driven:** Publishes events for async processing
- **Cache-Aside:** Caffeine cache with DB fallback
- **Method-level Caching:** `@Cacheable` for lazy-load pattern
- **Search Service Delegation:** Elasticsearch for complex queries

### Public API

- CRUD: `addProduct()`, `updateProduct()`, `deleteProduct()`
- Search: `searchProducts()`, `searchProductIds()`
- Retrieval: `getProductById()`, `getProductsByIds()`, `getProductsBySkus()`, `isExiting()`
- Analytics: `viewCount()`, `totalSoldQuantity()`, `totalRevenue()`

### Configuration

- **Cache Names:** `"productDetails"` (Caffeine, key = product ID)
- **Search Backend:** Elasticsearch
- **Events:** Product creation/update/deletion
- **Cache Sync:** `CacheSyncService` marks dirty entries for async refresh

---

## File: ShoppingCartService.java

**Purpose:** Manages user shopping carts with add/remove operations, stock validation, and total recalculation.

### Key Classes/Functions

- **`add(List<CartItemRequest> items): Response<ShoppingCartDto>`**
  - Validates non-empty item list
  - Fetches current user and existing cart (or creates new via `helper.createNewCart()`)
  - Batch-queries attributes for all SKUs
  - For each item:
    - quantity = 0 → remove item
    - quantity > 0 → add/update via `helper.handleAddItem()`
    - quantity < 0 → decrease via `helper.handleDecreaseItem()`
  - Recalculates totals
  - Records audit entry
  - Returns: updated cart DTO

- **`remove(List<String> skus): Response<ShoppingCartDto>`**
  - Validates non-empty SKU list
  - Fetches current user and cart
  - Removes matching items
  - Throws if no items found
  - Recalculates totals
  - Records audit entry
  - Returns: updated cart DTO

### Dependencies

- **Repositories:** `ShoppingCartRepository`, `AttributesRepository`, `UserRepository`
- **Utilities:** `SecurityUtil`, `Helper`

### Data Flow

```
add() → get user → find/create cart → 
  → batch query attributes → process items (add/remove/decrease) → 
  → recalculate totals → save → record audit

remove() → get user → find cart → 
  → remove by SKU → recalculate totals → save
```

### Design Patterns

- **Repository Pattern**
- **Delegation:** Helpers for item operations and recalculation
- **Batch Query:** Single DB call for all required attributes

### Public API

- Modify: `add()`, `remove()`

### Configuration

- **No explicit cache configuration** (cart is session-bound)
- **Audit tracking:** Via `auditInfo.addUpdateEntry()`

---

## Cross-Module Integration

### Cache Architecture

| Component | Cache Name | Strategy | TTL |
|-----------|-----------|----------|-----|
| Attributes | `"attributes"` | Spring Cache + DB | Explicit evict |
| Categories | `"categoryDetails"` | Spring Cache + DB | Explicit evict |
| Products | `"productDetails"` | Caffeine (native) | Explicit evict |

### External Services

- **MinIO:** Image/media storage (ProductImageService)
- **Redis:** Async cache invalidation (via RedisProducerService)
- **Elasticsearch:** Product search indexing (ProductService)
- **Event Producer:** Product lifecycle events (ProductEventProducer)

### Audit Trail

All services record modifications via `auditInfo.addUpdateEntry(username)` and soft-delete via `markDeletedAfter30Days(username)`.

### Security

- Current user retrieved via `SecurityUtil.getCurrentUsername()`
- Applied to: creation, updates, deletions, cart operations