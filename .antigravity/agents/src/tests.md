# ERP SpringBoot Experiment – Tests Module Knowledge Document

## Overview

The **tests** group contains 13 test files (~34,766 tokens) covering unit and functional testing across the ERP application. Tests focus on:

- **Order Management** — state machine transitions, lifecycle validation
- **Caching Strategy** — Caffeine cache configuration, batch loading, synchronization
- **Service Layer** — shopping cart, merchandise operations
- **REST Controllers** — authentication, orders, merchandise endpoints
- **Exception Handling** — global error handling via `GlobalExceptionHandler`

Tests use JUnit 5 with Mockito for mocking, and Spring's `WebMvcTest` for controller testing.

---

## File-by-File Analysis

### 1. `ErpSpringBootExperimentApplicationTests.java`

**Purpose:** Application context smoke test.

**Key Classes/Functions:**
- `contextLoads()` — verifies Spring Boot context loads successfully with test profile

**Dependencies:**
- `@SpringBootTest` — full application context
- `@ActiveProfiles("test")` — test configuration

**Public API:**
- Minimal; ensures application starts without errors

---

### 2. `OrderStatusHandlerTest.java`

**Purpose:** Comprehensive unit tests for order state machine (`OrderStatusHandler`).

**Key Classes/Functions:**

| Name | Purpose | Parameters | Return |
|------|---------|------------|--------|
| `getCurrentStatus(order)` | Returns current status (last element of history) | `Order` | `OrderStatus` |
| `transitionTo(order, targetStatus, reason)` | Validates and executes state transition | `Order`, `OrderStatus`, `String` | `void` (throws `BusinessException` on invalid) |
| `isTerminal(status)` | Checks if status is terminal (no outgoing transitions) | `OrderStatus` | `boolean` |
| `isValidTransition(from, to)` | Validates transition without side effects | `OrderStatus`, `OrderStatus` | `boolean` |

**Data Flow:**
- Orders store status history as `List<OrderStatus>`
- Current status = last element or `PENDING` if empty/null
- `transitionTo()` appends new status; never replaces
- Terminal states: `COMPLETED`, `CANCELLED`, `FAILED`, `REFUNDED`

**Design Patterns:**
- **State Machine** — enforces valid transitions, blocks invalid ones
- **Guard clauses** — validates preconditions before state change

**Valid Transitions (sample):**
- `PENDING` → `CONFIRMED`, `WAITING_PAYMENT`, `CANCELLED`
- `CONFIRMED` → `PROCESSING`, `CANCELLED`
- `PROCESSING` → `SHIPPING`, `READY_FOR_PICKUP`, `CANCELLED`
- `SHIPPING` → `DELIVERED`, `DELAYED`, `RETURNING`
- `DELIVERED` → `COMPLETED`, `RETURNING`
- `RETURNING` → `RETURNED` → `REFUNDED`

**Test Coverage:**
- ✅ Null/empty status list handling (defaults to `PENDING`)
- ✅ Valid transitions via `@ParameterizedTest` (22 scenarios)
- ✅ Invalid transitions rejection (30+ scenarios)
- ✅ Terminal state immutability
- ✅ Self-transition prohibition
- ✅ Full lifecycle paths (COD, Pickup, Return-Refund)
- ✅ Delay recovery, cancellation from non-terminal states

---

### 3. `ShoppingCartService.test.java`

**Purpose:** Commented-out unit tests for shopping cart operations (in progress).

**Key Classes/Functions (when active):**
- `add(items)` — adds/decreases items in cart
- `handleAddItem()`, `handleDecreaseItem()` — delegates to `Helper`
- `recalculateAndUpdateTotals()` — updates cart totals

**Data Flow:**
- `ProductQuantity(attributesId, quantity)` — positive = add, negative = decrease, zero = skip
- Resolves attributes via repository
- Updates cart via helper, recalculates totals, saves

**Dependencies:**
- `ShoppingCartRepository`, `AttributesRepository`, `UserRepository`
- `ShoppingCartMapper`, `Helper`, `SecurityUtil`

**Test Scenarios (inactive):**
- Multiple items with different quantities
- Null/empty list rejection
- Negative quantity handling (decrease)

---

### 4. `AttributesCacheTest.java`

**Purpose:** Functional tests for `AttributesService` caching via `CacheUtils` batch loading.

**Key Classes/Functions:**

| Name | Purpose | Cache Key | Caching Strategy |
|------|---------|-----------|------------------|
| `getAttributesByIds(idList)` | Batch load attributes, cache misses → DB | Long (attribute ID) | CacheUtils.getAll() |
| `getAttributesBySkus(skuList)` | Resolve SKUs to IDs, delegate to `getAttributesByIds()` | Long | Via getAttributesByIds |
| `getAttributesByProductId(productId)` | Load all attributes for product | Long | @Cacheable (not active in Mockito) |

**Caching Strategy:**
- **Cache Region:** `attributes` (Caffeine)
- **Batch Loading:** `CacheUtils.getAll()` checks cache for each ID; loads missing from DB
- **Entry Format:** `Long` → `AttributesDto`

**Data Flow:**
1. `getAttributesByIds([1L, 2L])` calls `CacheUtils.getAll()`
2. CacheUtils checks cache for both IDs
3. If ID 1 cached, skipped; ID 2 missing → DB load via `attributesRepository.getQuantityAttributesById([2L])`
4. Results combined and returned; DB-loaded values stored in cache

**Test Coverage:**
- ✅ Mixed cache hit/miss scenarios
- ✅ Empty/null ID handling
- ✅ Cache miss → DB load → cache hit on second call
- ✅ Different IDs cached independently
- ✅ SKU resolution with caching
- ✅ Direct cache put/get/evict operations
- ✅ Cache persistence across calls

---

### 5. `CacheConfigTest.java`

**Purpose:** Validates Caffeine cache configuration and regions.

**Key Classes/Functions:**

| Constant | Value | Purpose |
|----------|-------|---------|
| `CACHE_PRODUCT_DETAILS` | `"productDetails"` | Cache region for products |
| `CACHE_CATEGORY_DETAILS` | `"categoryDetails"` | Cache region for categories |
| `CACHE_ATTRIBUTES` | `"attributes"` | Cache region for attributes |

**Configuration:**
- **Cache Manager:** `CaffeineCacheManager`
- **Stats:** Enabled (`recordStats=true`)
- **Eviction:** Automatic via Caffeine TTL/size limits

**Test Coverage:**
- ✅ CacheManager bean creation
- ✅ All three cache regions exist
- ✅ Store/retrieve values
- ✅ Eviction works correctly
- ✅ Cache isolation (different regions don't interfere)
- ✅ Stats tracking (hit/miss counts)

---

### 6. `CacheSyncServiceTest.java`

**Purpose:** Tests background cache synchronization for product updates.

**Key Classes/Functions:**

| Name | Purpose | Parameters | Behavior |
|------|---------|------------|----------|
| `markProductDirty(productId)` | Marks product for cache refresh | `Long` | Adds to internal dirty set |
| `syncDirtyProductCaches()` | Processes dirty set, updates/evicts cache | — | Runs periodically (5 min), clears dirty set |

**Data Flow:**
1. **Mark Phase:** `markProductDirty(1L)` adds ID to dirty set (deduplicated)
2. **Sync Phase (scheduled):** `syncDirtyProductCaches()` processes each dirty ID
   - If product exists in DB → fetch → map to DTO → cache update
   - If product deleted → evict from cache
3. **Cleanup:** Dirty set cleared after processing

**Design Patterns:**
- **Dirty Tracking** — eventual consistency model
- **Cache Invalidation** — proactive refresh vs. evict

**Test Coverage:**
- ✅ Marking flow with deduplication
- ✅ Cache update for existing products
- ✅ Cache eviction for deleted products
- ✅ Empty dirty set (no-op)
- ✅ Dirty set cleared after sync
- ✅ Duplicate marks sync only once
- ✅ Updated cache usable by services

---

### 7. `CacheUtilsTest.java`

**Purpose:** Unit tests for `CacheUtils.getAll()` — batch cache loading helper.

**Key Classes/Functions:**

```java
CacheUtils.getAll(
    CacheManager cacheManager,
    String cacheName,
    Collection<K> keys,
    Function<Collection<K>, Map<K, V>> dbLoader
) → Map<K, V>
```

**Logic:**
- Uses Caffeine's native `getAll(keys, function)` method
- Caffeine checks cache first; calls `dbLoader` for missing keys
- Returns combined map (cached + loaded)

**Test Coverage:**
- ✅ All keys cached → returns all from cache
- ✅ Mixed cache hit/miss → combines results
- ✅ All keys missing → loads all from dbLoader
- ✅ Empty keys list → returns empty map
- ✅ Non-existent cache name → fallback to dbLoader
- ✅ Loaded values stored in cache for subsequent calls
- ✅ Null value handling (Caffeine doesn't allow nulls)
- ✅ Works with Collection types (List, Set)

---

### 8. `CategoryCacheTest.java`

**Purpose:** Functional tests for `CategoryService` caching via `CacheUtils`.

**Key Classes/Functions:**

| Name | Purpose | Cache Strategy |
|------|---------|-----------------|
| `getCategoriesByIds(idList)` | Batch load categories | CacheUtils.getAll() |
| `getCategoriesBySkus(skuList)` | Resolve SKUs to IDs, delegate to `getCategoriesByIds()` | Via getCategoriesByIds |

**Caching Strategy:**
- **Cache Region:** `categoryDetails` (Caffeine)
- **Entry Format:** `Long` → `CategoryDto`

**Data Flow:**
- Same as `AttributesCacheTest` but for categories
- `getCategoriesBySkus()` resolves SKUs via `findIdsAndSkusBySkus()` first

**Test Coverage:**
- ✅ Cache miss → DB load → cache hit
- ✅ Mixed cache hit/miss batch loading
- ✅ Null/empty IDs return empty without DB call
- ✅ SKU resolution with caching
- ✅ Single ID miss triggers DB load once
- ✅ Cache stores DTOs for reuse

---

### 9. `ProductCacheTest.java`

**Purpose:** Functional tests for `ProductService` caching via `CacheUtils` and `CacheSyncService`.

**Key Classes/Functions:**

| Name | Purpose | Cache Strategy | Note |
|------|---------|-----------------|------|
| `getProductById(id)` | Get single product | @Cacheable (inactive in Mockito) | No AOP proxy in tests |
| `getProductsByIds(idList)` | Batch load products | CacheUtils.getAll() | Main caching method |
| `getProductsBySkus(skuList)` | Resolve SKUs, delegate | Via getProductsByIds | SKU resolution first |
| `updateProduct(request)` | Update product, mark dirty | Calls CacheSyncService | Triggers async cache refresh |

**Caching Strategy:**
- **Cache Region:** `productDetails` (Caffeine)
- **Entry Format:** `Long` → `ProductDto`
- **Dirty Tracking:** `CacheSyncService.markProductDirty()` on update

**Data Flow:**
1. `getProductsByIds()` → `CacheUtils.getAll()` → mixed hit/miss
2. `updateProduct()` → `CacheSyncService.markProductDirty(productId)`
3. Background sync → cache refresh/eviction

**Test Coverage:**
- ✅ Direct getProductById loads from DB (no AOP proxy active)
- ✅ Mixed cache hit/miss batch loading
- ✅ Empty/null IDs return empty
- ✅ Cache miss → DB load → cache hit
- ✅ Different IDs cached independently
- ✅ Cache consistency between methods
- ✅ Update marks dirty for background sync
- ✅ SKU resolution with caching
- ✅ Direct cache operations work
- ✅ Loaded data persists in cache

---

### 10. `GlobalExceptionHandlerTest.java`

**Purpose:** Tests `GlobalExceptionHandler` exception conversion to REST responses.

**Key Classes/Functions:**

| Exception | HTTP Status | Response Format |
|-----------|-------------|-----------------|
| `BusinessException` | 400 Bad Request | RFC 7807 `ProblemDetail` |
| `RuntimeException` (unhandled) | 500 Internal Server Error | Standard error response |

**Response Structure (BusinessException):**
```json
{
  "title": "ErrorCode message (Vietnamese)",
  "detail": "Exception message",
  "errorCode": "ERROR_CODE_ENUM",
  "details": { "key": "value" }  // Optional context
}
```

**Design Patterns:**
- **Global Exception Handler** — `@RestControllerAdvice` centralizes error handling
- **ProblemDetail** — RFC 7807 standard for HTTP error responses

**Test Coverage:**
- ✅ BusinessException converts to 400 with ProblemDetail
- ✅ RuntimeException converts to 500
- ✅ Error code and details preserved

---

### 11. `AuthControllerImplTest.java`

**Purpose:** REST unit tests for `authControllerImpl` endpoints.

**Endpoints Tested:**

| Method | Path | Purpose | Request | Response |
|--------|------|---------|---------|----------|
| GET | `/api/auth/me` | Get current user profile | — | `MyProfileResponse` |
| PUT | `/api/auth/me` | Update profile | `UpdateProfileRequest` | `MyProfileResponse` |
| POST | `/api/auth/me/avatar` | Upload avatar | Multipart file | `MyProfileResponse` |

**Request/Response DTOs:**

**MyProfileResponse:**
- `username`, `fullName`, `email`, `phoneNumber`, `avatarUrl`
- `dateOfBirth`, `gender`, `rank`, `status`, `roles`

**UpdateProfileRequest:**
- `fullName`, `phoneNumber`, `gender`

**Validations:**
- Phone number: length validation (rejects "1234")
- Full name: no special characters (rejects "Nguyen @ B")

**Test Coverage:**
- ✅ Get profile returns 200 with user data
- ✅ Update profile with valid data
- ✅ Invalid phone rejects with 400
- ✅ Invalid name characters reject with 400
- ✅ Upload avatar with valid image file
- ✅ Avatar URL updated in response

---

### 12. `MerchandiseControllerImplTest.java`

**Purpose:** Commented-out REST unit tests for merchandise endpoints (in progress).

**Endpoints (when active):**

**Products:**
- POST `/api/merchandise/add-Product` — add product
- PUT `/api/merchandise/update-Product` — update product
- DELETE `/api/merchandise/delete-Product` — delete by IDs
- POST `/api/merchandise/search-Product` — search with pagination

**Product Images:**
- POST `/api/merchandise/add-Product-Images/{productId}` — add images
- DELETE `/api/merchandise/delete-Product-Image/{productId}` — delete single image
- PUT `/api/merchandise/replace-Product-Images/{productId}` — replace images

**Categories:**
- POST `/api/merchandise/add-Category` — create category
- PUT `/api/merchandise/update-Category` — update category
- DELETE `/api/merchandise/delete-Category` — delete by IDs
- POST `/api/merchandise/search-Category` — search with pagination

**Attributes:**
- POST `/api/merchandise/add-Attributes` — create attributes
- PUT `/api/merchandise/update-Attributes` — update attributes
- DELETE `/api/merchandise/delete-Attributes` — delete by SKUs
- DELETE `/api/merchandise/delete-Attributes-by-Product/{productId}` — delete all for product
- GET `/api/merchandise/get-Attributes-by-Product/{productId}` — get all for product
- GET `/api/merchandise/get-Attributes-by-Sku/{sku}` — get by SKU

**Test Coverage (when active):**
- ✅ CRUD operations for products, categories, attributes
- ✅ Image management (add, delete, replace)
- ✅ Pagination and search
- ✅ Multipart file handling

---

### 13. `orderControllerImplTest.java`

**Purpose:** REST unit tests for `orderControllerImpl` endpoints.

**Customer Endpoints:**

| Method | Path | Purpose | Request | Response |
|--------|------|---------|---------|----------|
| POST | `/api/orders` | Create order | `CreateOrderRequest` | `OrderDto` |
| GET | `/api/orders/{orderId}` | Get by ID | — | `OrderDto` |
| GET | `/api/orders/number/{orderNumber}` | Get by order number | — | `OrderDto` |
| POST | `/api/orders/my-orders` | Get my orders (paginated) | `OrderSearchRequest` | `PagingResponse<OrderDto>` |
| POST | `/api/orders/cancel` | Cancel order | `CancelOrderRequest` | `OrderDto` |

**Admin Endpoints:**

| Method | Path | Purpose | Request | Response |
|--------|------|---------|---------|----------|
| POST | `/api/orders/search` | Search orders (paginated) | `OrderSearchRequest` | `PagingResponse<OrderDto>` |
| PUT | `/api/orders/shipping` | Update shipping info | `UpdateShippingRequest` | `OrderDto` |
| PUT | `/api/orders/delivery` | Update delivery dates | `UpdateDeliveryRequest` | `OrderDto` |
| PUT | `/api/orders/admin-notes` | Update admin notes | `UpdateAdminNotesRequest` | `OrderDto` |
| POST | `/api/orders/confirm` | Confirm order | `ConfirmOrderRequest` | `OrderDto` |
| POST | `/api/orders/complete` | Complete order | `CompleteOrderRequest` | `OrderDto` |
| GET | `/api/orders/pending` | Get pending orders | — | `List<OrderDto>` |
| GET | `/api/orders/in-progress` | Get in-progress orders | — | `List<OrderDto>` |
| GET | `/api/orders/statistics` | Get statistics | Query: `startDate`, `endDate` | `Map<String, Object>` |

**Dashboard Endpoints:**

| Method | Path | Purpose | Behavior |
|--------|------|---------|----------|
| POST | `/api/orders/transition` | Generic state transition | Throws error for unsupported transitions |
| POST | `/api/orders/ship` | Ship order | Returns delivery token/URL |
| GET | `/api/orders/delivery-pin/{orderNumber}` | Get delivery PIN info | Returns PIN details |
| DELETE | `/api/orders/delivery-pin/{orderNumber}` | Clear PIN | Deletes PIN, confirms deletion |

**Request DTOs:**

- `CreateOrderRequest` — items, address, discount, shipping method, notes
- `CancelOrderRequest` — orderId, cancellation reason
- `ConfirmOrderRequest` — orderId, confirmation info
- `CompleteOrderRequest` — orderId
- `OrderSearchRequest` — orderStatus, page, size, sortBy, sortDirection
- `UpdateShippingRequest`, `UpdateDeliveryRequest`, `UpdateAdminNotesRequest`, etc.

**Response DTOs:**

**OrderDto:**
- `id`, `orderNumber`, `status` (history), `currentStatus`, `currentStatusDescription`
- `customerId`, `customerName`, `customerEmail`, `customerPhone`
- `orderItems`, `subtotal`, `discountAmount`, `taxAmount`, `shippingFee`, `totalAmount`
- `customerNotes`, `confirmedAt`, `confirmedBy`, `cancelledAt`, `cancelledBy`, etc.

**Test Coverage:**
- ✅ Create order returns 201 Created
- ✅ Get by ID/order number
- ✅ Get paginated my orders
- ✅ Cancel order with reason
- ✅ Search orders with pagination
- ✅ Update shipping/delivery/admin notes
- ✅ Confirm/complete order
- ✅ Get pending/in-progress orders
- ✅ Get statistics by date range
- ✅ State transition error handling
- ✅ Ship order returns delivery token
- ✅ Delivery PIN operations

---

## Cross-File Dependencies & Data Flow

### Cache Ecosystem
```
AttributesService → CacheUtils.getAll() → CacheConfig.cacheManager() → Caffeine Cache
CategoryService  → (same path)
ProductService   → (same path) + CacheSyncService.markProductDirty()
  ↓
CacheSyncService → scheduled syncDirtyProductCaches() → cache update/evict
```

### Order Lifecycle
```
OrderStatusHandler (state machine) ← orderControllerImpl (REST)
  ↓
Valid transitions enforced:
PENDING → CONFIRMED → PROCESSING → SHIPPING → DELIVERED → COMPLETED
         ↓ (alternative path)
         READY_FOR_PICKUP → DELIVERED
  ↓ (returns)
RETURNING → RETURNED → REFUNDED
  ↓ (cancellation)
CANCELLED (terminal)
```

### Exception Handling
```
Controller endpoints → throw BusinessException / RuntimeException
  ↓
GlobalExceptionHandler (@RestControllerAdvice)
  ↓
Convert to ProblemDetail (RFC 7807) + HTTP status
  ↓
REST response to client
```

---

## Configuration & Constants

**Cache Configuration (CacheConfig):**
- Three Caffeine cache regions: `productDetails`, `categoryDetails`, `attributes`
- Stats enabled for monitoring
- TTL/size limits managed by Caffeine

**Order Statuses (OrderStatus enum):**
- Non-terminal: `PENDING`, `WAITING_PAYMENT`, `CONFIRMED`, `PROCESSING`, `SHIPPING`, `DELAYED`, `READY_FOR_PICKUP`, `DELIVERED`, `RETURNING`, `RETURNED`
- Terminal: `COMPLETED`, `CANCELLED`, `FAILED`, `REFUNDED`

**Security & Auth:**
- `SecurityUtil` — current user context
- `JwtService` — token operations
- `UserDetailsService` — Spring Security integration

---

## Summary

The **tests** module provides comprehensive coverage across order management, caching, REST APIs, and exception handling. Key testing strategies:

- **Unit Tests:** OrderStatusHandler state machine (comprehensive parameterized tests)
- **Functional Tests:** Caching behavior with mock repositories
- **Integration Tests:** REST controllers via `WebMvcTest`
- **Mocking:** Mockito for dependencies, MockMvc for HTTP requests

All tests use JUnit 5 with clear `@DisplayName` annotations and nested `@Nested` test classes for organization.