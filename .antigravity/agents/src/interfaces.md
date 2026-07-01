# Interfaces Module Knowledge Document

## Overview

The **interfaces** group defines nine service-layer contracts for an ERP Spring Boot application. These interfaces establish the API boundaries for core business domains: user authentication, product management, inventory attributes, categories, orders, shopping carts, product images, and Redis caching. Each interface uses Spring's `Response` wrapper pattern for consistent API responses and leverages DTOs for data transfer.

---

## iAttributes.java

**Purpose:** Contract for product attribute management—create, update, delete, search, and retrieve attributes linked to products or SKUs.

**Key Methods:**
- `create(CreateAttributesRequest)` → `Response<List<AttributesDto>>` — Creates new attributes
- `update(UpdateAttributesRequest)` → `Response<?>` — Updates existing attributes
- `delete(List<String> skus)` → `Response<?>` — Deletes attributes by SKU
- `deleteByProduct(String productId)` → `Response<?>` — Deletes all attributes for a product
- `search(AttributesSearchRequest)` → `Page<AttributesDto>` — Paginated search with filters
- `getAttributesByProductId(String productId)` → `List<AttributesDto>` — Retrieves attributes for a product
- `getAttributesByIds(List<Long> ids)` → `Response<List<AttributesDto>>` — Batch retrieval by ID
- `searchAttributesIds(AttributesSearchRequest)` → `List<Long>` — Returns only attribute IDs matching criteria
- `getAttributesBySkus(List<String> skus)` → `Response<List<AttributesDto>>` — Batch retrieval by SKU

**Dependencies:**
- `AttributesDto` — data transfer object
- `CreateAttributesRequest`, `UpdateAttributesRequest`, `AttributesSearchRequest` — request DTOs
- `Response<T>` — response wrapper from `ResponseConfig`

**Design Patterns:** Service contract with request/response wrapper pattern; separation of concerns (search, create, delete, retrieve operations).

**Public API:** All methods public; exposes CRUD and search operations for attribute management.

---

## iCategory.java

**Purpose:** Contract for product category management—create, update, delete, search categories, and check category existence.

**Key Methods:**
- `create(String name)` → `Response<?>` — Creates category by name
- `update(UpdateCategoryRequest)` → `Response<?>` — Updates category
- `delete(List<String> ids)` → `Response<?>` — Deletes categories by ID
- `search(CategorySearchRequest)` → `Page<CategoryDto>` — Paginated category search
- `isExiting(String name)` → `CategoryExitingResponse` — Checks if category exists
- `getCategoriesByIds(List<Long> ids)` → `Response<List<CategoryDto>>` — Batch retrieval by ID
- `getCategoriesBySkus(List<String> skus)` → `Response<List<CategoryDto>>` — Retrieves categories by SKU

**Dependencies:**
- `CategoryDto` — data transfer object
- `CategorySearchRequest`, `UpdateCategoryRequest` — request DTOs
- `CategoryExitingResponse` — custom response for existence check
- `Response<T>` — response wrapper

**Design Patterns:** Service contract with existence validation; batch retrieval by ID and SKU.

**Public API:** All methods public; standard CRUD + search + existence check operations.

---

## iOrder.java

**Purpose:** Contract for order lifecycle management—creation, retrieval, status transitions (confirm, ship, deliver, return, refund), payment processing, and statistics.

**Key Methods:**

*Core Retrieval:*
- `createOrder(CreateOrderRequest)` → `Response<OrderDto>` — Creates new order
- `getOrderById(String orderId)` → `Response<OrderDto>` — Retrieves single order
- `getOrderByOrderNumber(String orderNumber)` → `Response<OrderDto>` — Retrieves by order number
- `getMyOrders(OrderSearchRequest)` → `Response<PagingResponse<OrderDto>>` — User's orders (paginated)
- `searchOrders(OrderSearchRequest)` → `Response<PagingResponse<OrderDto>>` — Admin search (paginated)
- `getPendingOrders()` → `Response<List<OrderDto>>` — All pending orders
- `getInProgressOrders()` → `Response<List<OrderDto>>` — All in-progress orders
- `getOrderStatistics(String startDate, String endDate)` → `Response<?>` — Statistics within date range

*Status Update Operations:*
- `updateShipping(UpdateShippingRequest)` — Updates shipping info
- `updateDelivery(UpdateDeliveryRequest)` — Updates delivery status
- `updateAdminNotes(UpdateAdminNotesRequest)` — Adds admin notes
- `confirmOrder(ConfirmOrderRequest)` — Confirms order placement
- `cancelOrder(CancelOrderRequest)` — Cancels order
- `completeOrder(CompleteOrderRequest)` — Marks order complete
- `processOrder(ProcessOrderRequest)` — Processes order
- `shipOrder(ShipOrderRequest)` — Ships order
- `markDelayed(DelayOrderRequest)` — Marks order delayed
- `deliverOrder(DeliverOrderRequest)` — Delivers order
- `readyForPickup(ReadyForPickupRequest)` — Marks ready for pickup
- `pickupOrder(PickupOrderRequest)` — Completes pickup
- `returnOrder(ReturnOrderRequest)` — Initiates return
- `confirmReturn(ConfirmReturnRequest)` — Confirms return
- `refundOrder(RefundOrderRequest)` — Processes refund
- `processPayment(PaymentCallbackRequest)` — Handles payment callback

*Other:*
- `setStatus(String orderNumber, OrderStatus status)` → `void` — Direct status setter

**Dependencies:**
- `OrderDto` — data transfer object
- `OrderStatus` enum — status enumeration
- Multiple request DTOs: `CreateOrderRequest`, `OrderSearchRequest`, `UpdateShippingRequest`, `UpdateDeliveryRequest`, `UpdateAdminNotesRequest`, `ConfirmOrderRequest`, `CancelOrderRequest`, `CompleteOrderRequest`, `ProcessOrderRequest`, `ShipOrderRequest`, `DelayOrderRequest`, `DeliverOrderRequest`, `ReadyForPickupRequest`, `PickupOrderRequest`, `ReturnOrderRequest`, `ConfirmReturnRequest`, `RefundOrderRequest`, `PaymentCallbackRequest`
- `PagingResponse<T>` — paginated response wrapper

**Design Patterns:** State machine pattern (order transitions through multiple statuses); command pattern (each status transition is a separate method); separation of user operations (`getMyOrders`) from admin operations (`searchOrders`).

**Public API:** All methods public; exposes complete order lifecycle management.

---

## iProduct.java

**Purpose:** Contract for product management—create, update, delete, search products, track metrics (view count, sales quantity, revenue), and retrieve by ID/SKU.

**Key Methods:**
- `addProduct(CreateProductRequest)` → `Response<?>` — Adds new product
- `updateProduct(UpdateProductRequest)` → `Response<?>` — Updates product
- `deleteProduct(List<Long> ids)` → `Response<?>` — Deletes products by ID
- `searchProducts(GetProductRequest)` → `Page<ProductDto>` — Paginated product search
- `isExiting(String name)` → `ProductIsExiting` — Checks product existence
- `viewCount(String productId)` → `void` — Increments view counter
- `totalSoldQuantity(String productId)` → `void` — Increments sold quantity
- `totalRevenue(String productId, double price)` → `void` — Adds to revenue total
- `getProductsByIds(List<Long> ids)` → `Response<List<ProductDto>>` — Batch retrieval by ID
- `getProductsBySkus(List<String> skus)` → `Response<List<ProductDto>>` — Batch retrieval by SKU

**Dependencies:**
- `ProductDto` — data transfer object
- `CreateProductRequest`, `UpdateProductRequest`, `GetProductRequest` — request DTOs
- `ProductIsExiting` — existence check response
- `Response<T>` — response wrapper

**Design Patterns:** Service contract with analytics methods (viewCount, totalSoldQuantity, totalRevenue); batch retrieval operations.

**Public API:** All methods public; CRUD, search, metrics tracking, and batch retrieval.

---

## iProductCaching.java

**Purpose:** Contract for product caching operations—stores product DTOs in cache (likely Redis-backed).

**Key Methods:**
- `addProduct(List<ProductDto> items)` → `void` — Caches product list

**Dependencies:**
- `ProductDto` — data transfer object

**Design Patterns:** Caching layer abstraction; decouples cache implementation from consumers.

**Public API:** Single method; minimal interface for cache population.

---

## iProductImage.java

**Purpose:** Contract for product image management—upload, delete, replace, and retrieve product images.

**Key Methods:**
- `addProductImages(String productId, List<MultipartFile> images)` → `Response<?>` — Adds images to product
- `deleteProductImage(String productId, String imageKey)` → `Response<?>` — Removes single image
- `replaceProductImages(String productId, List<MultipartFile> images)` → `Response<?>` — Replaces all images
- `getProductImage(String imageName)` → `byte[]` — Retrieves image binary data

**Dependencies:**
- `MultipartFile` — Spring multipart file upload
- `Response<T>` — response wrapper

**Design Patterns:** File storage abstraction; image lifecycle management (add, delete, replace, retrieve).

**Public API:** All methods public; image CRUD operations.

---

## iRedis.java

**Purpose:** Contract for Redis operations—key-value, hash, list, and set operations with expiry management.

**Key Methods:**

*Key Operations:*
- `hasKey(String key)` → `boolean` — Checks key existence
- `delete(String... keys)` → `void` — Deletes one or more keys
- `getExpire(String key, TimeUnit timeUnit)` → `void` — Retrieves expiry
- `expire(String key, long timeout, TimeUnit timeUnit)` → `void` — Sets expiry

*String Operations:*
- `setValue(String key, Object value)` → `void` — Sets value
- `setValueWithExpiry(String key, Object value, long time, TimeUnit timeUnit)` → `void` — Sets with expiry
- `getValue(String key)` → `Object` — Retrieves value

*Hash Operations:*
- `hSet(String key, String field, Object value)` → `void` — Sets hash field
- `hGet(String key, String field)` → `Object` — Gets hash field
- `hGetAll(String key)` → `Map<Object, Object>` — Gets all hash fields
- `hDelete(String key, Object... fields)` → `void` — Deletes hash fields

*List Operations:*
- `lPush(String key, Object value)` → `void` — Pushes to list
- `lPop(String key)` → `Object` — Pops from list
- `lRange(String key, long start, long end)` → `List<Object>` — Gets list range

*Set Operations:*
- `sAdd(String key, Object... values)` → `void` — Adds to set
- `sMembers(String key)` → `Set<Object>` — Gets all set members
- `sRemove(String key, Object... values)` → `void` — Removes from set

**Dependencies:** `java.util.*` (Map, List, Set); `java.util.concurrent.TimeUnit` — time unit enumeration.

**Design Patterns:** Adapter pattern (abstracts Redis client); supports multiple data structures (strings, hashes, lists, sets).

**Public API:** All methods public; complete Redis API abstraction layer.

---

## iShoppingCart.java

**Purpose:** Contract for shopping cart operations—add and remove items.

**Key Methods:**
- `add(List<CartItemRequest> items)` → `Response<ShoppingCartDto>` — Adds items to cart
- `remove(List<String> skus)` → `Response<ShoppingCartDto>` — Removes items by SKU

**Dependencies:**
- `ShoppingCartDto` — data transfer object
- `CartItemRequest` — request DTO
- `Response<T>` — response wrapper

**Design Patterns:** Service contract for cart operations; SKU-based item identification.

**Public API:** Two methods; minimal cart interface (add/remove only).

---

## iUser.java

**Purpose:** Contract for user authentication, registration, profile management, and account verification—handles registration, login, token refresh, email verification, password reset, and profile updates.

**Key Methods:**

*Authentication:*
- `createUser(UserRegisterRequest)` → `Response<RegisterResponse>` — User registration
- `loginUser(UserLoginRequest)` → `Response<AuthResponse>` — User login with token generation
- `refreshToken(RefreshTokenRequest)` → `Response<AuthResponse>` — Refreshes auth token
- `logoutUser(HttpServletRequest)` → `void` — Logs out user

*Verification & Password:*
- `verifyEmail(String code)` → `Response<String>` — Confirms email via code
- `sendCodeResetPassword(String email)` → `Response<String>` — Sends password reset code
- `resetPassword(String code, AccountVerificationRequest)` → `Response<String>` — Resets password with code

*Profile Management:*
- `getMyProfile()` → `Response<MyProfileResponse>` — Retrieves current user profile
- `updateMyProfile(UpdateProfileRequest)` → `Response<MyProfileResponse>` — Updates profile
- `uploadAvatar(MultipartFile file)` → `Response<MyProfileResponse>` — Uploads avatar image

**Dependencies:**
- `UserRegisterRequest`, `UserLoginRequest`, `RefreshTokenRequest`, `AccountVerificationRequest`, `UpdateProfileRequest` — request DTOs
- `RegisterResponse`, `AuthResponse`, `MyProfileResponse` — response DTOs
- `Response<T>` — response wrapper
- `HttpServletRequest` — servlet request (for logout context)
- `MultipartFile` — file upload

**Design Patterns:** Authentication flow pattern (register → verify email → login → refresh token); session management abstraction.

**Public API:** All methods public; complete user lifecycle and authentication operations.

---

## Cross-Interface Data Flow

- **iProduct** → **iAttributes**, **iCategory**: Products reference attributes and categories
- **iOrder** → **iProduct**, **iShoppingCart**: Orders contain products; shopping cart feeds into orders
- **iProductImage** → **iProduct**: Images attached to products
- **iProductCaching** ← **iProduct**: Product caching layer
- **iRedis**: Underlying cache provider for **iProductCaching** and general session/cache storage
- **iUser**: Authentication context for all operations (implicit user context)

---

## Response Wrapper Pattern

All interfaces (except **iRedis** and **iProductCaching**) use `Response<T>` wrapper from `ResponseConfig` for standardized API responses. Search operations use `Page<T>` (Spring Data pagination) or `PagingResponse<T>` for order listing.