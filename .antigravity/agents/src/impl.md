# RefreshModuleAgent: impl Group Analysis

## Overview

The **impl** group contains three REST controller implementations that serve as the presentation layer for an ERP Spring Boot system. These controllers implement interface contracts and delegate business logic to service layers, handling authentication/authorization, merchandise management (products, categories, attributes), and order lifecycle operations. All three controllers use Spring's `@RestController` annotation and dependency injection via `@RequiredArgsConstructor`.

---

## File: authControllerImpl.java

**Purpose:** Implements authentication and user profile management endpoints. Provides login, registration, email verification, password reset, token refresh, and profile operations.

### Key Classes/Functions

| Function | Parameters | Return Type | Purpose |
|----------|-----------|------------|---------|
| `login()` | `UserLoginRequest body` | `Response<AuthResponse>` | Authenticate user and return auth token |
| `register()` | `UserRegisterRequest body` | `Response<RegisterResponse>` | Create new user account |
| `verifyEmail()` | `String code` | `Response<String>` | Verify email with verification code |
| `resetPassword()` | `String code`, `AccountVerificationRequest body` | `Response<String>` | Reset password using verification code |
| `refreshToken()` | `RefreshTokenRequest body` | `Response<AuthResponse>` | Refresh expired auth token |
| `sendPasswordResetCode()` | `String email` | `Response<String>` | Send password reset code to email |
| `logout()` | `HttpServletRequest request` | `ResponseEntity<?>` | Invalidate user session |
| `getMyProfile()` | None | `Response<MyProfileResponse>` | Retrieve current user's profile |
| `updateMyProfile()` | `UpdateProfileRequest body` | `Response<MyProfileResponse>` | Update user profile information |
| `uploadAvatar()` | `MultipartFile file` | `Response<MyProfileResponse>` | Upload and set user avatar |

### Dependencies

- `iUser` service interface — delegates all auth/profile business logic
- DTOs: `UserLoginRequest`, `UserRegisterRequest`, `UpdateProfileRequest`, `RefreshTokenRequest`, `AccountVerificationRequest`
- Response wrappers: `AuthResponse`, `RegisterResponse`, `MyProfileResponse`, `ResponseConfig.Response`

### Design Patterns

- **Facade/Delegation:** Controller acts as thin facade, forwarding all logic to `iUser` service
- **Data Transfer Objects:** Request/response DTOs decouple API contracts from internal models

### Public API

- `/login` — POST with credentials → `AuthResponse`
- `/register` — POST with user details → `RegisterResponse`
- `/verify-email` — POST with verification code → confirmation
- `/reset-password` — POST with reset code → confirmation
- `/refresh-token` — POST with refresh token → new `AuthResponse`
- `/send-reset-code` — POST with email → confirmation
- `/logout` — POST → success message
- `/my-profile` — GET → `MyProfileResponse`
- `/my-profile` — PUT with updates → `MyProfileResponse`
- `/upload-avatar` — POST multipart → `MyProfileResponse`

### Configuration

No environment variables or constants defined in this file; relies on service layer configuration.

---

## File: merchandiseControllerImpl.java

**Purpose:** Manages product catalog operations including CRUD for products, categories, attributes, product images, and file uploads. Provides search, retrieval by IDs/SKUs, and existence checks.

### Key Classes/Functions

#### Product Operations

| Function | Parameters | Return Type | Purpose |
|----------|-----------|------------|---------|
| `addProduct()` | `CreateProductRequest request` | `Response<?>` | Create new product |
| `updateProduct()` | `UpdateProductRequest request` | `Response<?>` | Update existing product |
| `deleteProduct()` | `List<String> ids` | `Response<?>` | Delete one or multiple products by IDs |
| `searchProduct()` | `GetProductRequest request` | `Page<ProductDto>` | Search products with pagination |
| `getProductsByIds()` | `List<Long> ids` | `Response<List<ProductDto>>` | Retrieve products by ID list (cache-aware) |
| `getProductsBySkus()` | `List<String> skus` | `Response<List<ProductDto>>` | Retrieve products by SKU list |
| `incrementViewCount()` | `String productId` | `Response<?>` | Increment product view counter |
| `checkProduct()` | `String name` | `ProductIsExiting` | Check if product exists by name |

#### Product Image Operations

| Function | Parameters | Return Type | Purpose |
|----------|-----------|------------|---------|
| `addProductImages()` | `String productId`, `List<MultipartFile> images` | `Response<?>` | Upload images for product |
| `deleteProductImage()` | `String productId`, `String imageKey` | `Response<?>` | Delete specific product image |
| `replaceProductImages()` | `String productId`, `List<MultipartFile> images` | `Response<?>` | Replace all product images |
| `viewProductImage()` | `String imageName` | `byte[]` | Retrieve image by filename |

#### Category Operations

| Function | Parameters | Return Type | Purpose |
|----------|-----------|------------|---------|
| `addCategory()` | `String name` | `Response<?>` | Create new category |
| `updateCategory()` | `UpdateCategoryRequest categoryDto` | `Response<?>` | Update category |
| `deleteCategory()` | `List<String> ids` | `Response<?>` | Delete categories by IDs |
| `searchCategory()` | `CategorySearchRequest request` | `Response<PagingResponse<CategoryDto>>` | Search categories with pagination |
| `getCategoriesByIds()` | `List<Long> ids` | `Response<List<CategoryDto>>` | Retrieve categories by ID list |
| `getCategoriesBySkus()` | `List<String> skus` | `Response<List<CategoryDto>>` | Retrieve categories by SKU list |
| `check()` | `String name` | `CategoryExitingResponse` | Check if category exists |

#### Attributes Operations

| Function | Parameters | Return Type | Purpose |
|----------|-----------|------------|---------|
| `addAttributes()` | `CreateAttributesRequest request` | `Response<List<AttributesDto>>` | Create product attributes/variants |
| `updateAttributes()` | `UpdateAttributesRequest request` | `Response<?>` | Update attributes |
| `deleteAttributes()` | `List<String> ids` | `Response<?>` | Delete attributes by IDs |
| `deleteAttributesByProduct()` | `String productId` | `Response<?>` | Delete all attributes for product |
| `searchAttributes()` | `AttributesSearchRequest request` | `Response<PagingResponse<AttributesDto>>` | Search attributes with pagination |
| `getAttributesByIds()` | `List<Long> ids` | `Response<List<AttributesDto>>` | Retrieve attributes by ID list |
| `getAttributesBySkus()` | `List<String> skus` | `Response<List<AttributesDto>>` | Retrieve attributes by SKU list |

#### File Management

| Function | Parameters | Return Type | Purpose |
|----------|-----------|------------|---------|
| `upload()` | `MultipartFile file` | `ResponseEntity<String>` | Upload file to MinIO storage |

### Dependencies

- Services: `ProductService`, `CategoryService`, `AttributesService`, `iProductImage`, `MinioService`
- `SecurityUtil` — utility for security operations
- DTOs: Request types (`CreateProductRequest`, `UpdateProductRequest`, `GetProductRequest`, etc.) and response types (`ProductDto`, `CategoryDto`, `AttributesDto`, `ProductIsExiting`, `CategoryExitingResponse`)
- Spring: `Page`, `MultipartFile`, `@PathVariable`, `@RequestParam`, `@RequestBody`, validation annotations

### Design Patterns

- **Delegation:** Controller routes requests to specialized services (ProductService, CategoryService, AttributesService)
- **Pagination:** Uses Spring's `Page<T>` and custom `PagingResponse<T>` wrapper with `PageableData.from(categories)` helper
- **ID Conversion:** Converts string IDs to `Long` with error handling in `deleteProduct()` (lines 60–67)
- **Multi-resource Operations:** Supports batch operations (delete multiple, retrieve multiple by IDs/SKUs)

### Public API

**Products:**
- `/products` — POST → create
- `/products` — PUT → update
- `/products?ids=...` — DELETE → batch delete
- `/products/search` — POST → search with pagination
- `/products/by-ids?ids=...` — GET → retrieve by IDs
- `/products/by-skus?skus=...` — GET → retrieve by SKUs
- `/products/{productId}/increment-view` — POST → increment view count
- `/products/check?name=...` — GET → check existence

**Product Images:**
- `/products/{productId}/images` — POST → upload images
- `/products/{productId}/images?imageKey=...` — DELETE → delete image
- `/products/{productId}/images/replace` — POST → replace all images
- `/products/images/{imageName}` — GET → view image

**Categories:**
- `/categories?name=...` — POST → create
- `/categories` — PUT → update
- `/categories?ids=...` — DELETE → batch delete
- `/categories/search` — POST → search with pagination
- `/categories/by-ids?ids=...` — GET → retrieve by IDs
- `/categories/by-skus?skus=...` — GET → retrieve by SKUs
- `/categories/check?name=...` — GET → check existence

**Attributes:**
- `/attributes` — POST → create
- `/attributes` — PUT → update
- `/attributes?ids=...` — DELETE → delete by IDs
- `/attributes/{productId}/delete-all` — DELETE → delete all for product
- `/attributes/search` — POST → search with pagination
- `/attributes/by-ids?ids=...` — GET → retrieve by IDs
- `/attributes/by-skus?skus=...` — GET → retrieve by SKUs

**File Upload:**
- `/upload` — POST multipart → upload to MinIO

### Configuration

- **Security:** Injects `SecurityUtil` for access control
- **Storage:** Uses MinIO service for file operations
- **Pagination:** Custom `PageableData.from()` converts Spring `Page` metadata

---

## File: orderControllerImpl.java

**Purpose:** Manages order lifecycle from creation through delivery. Provides customer endpoints for order management and admin endpoints for order fulfillment, status transitions, and statistics.

### Key Classes/Functions

#### Customer Endpoints

| Function | Parameters | Return Type | Purpose |
|----------|-----------|------------|---------|
| `createOrder()` | `CreateOrderRequest request` | `Response<OrderDto>` | Customer creates new order |
| `getOrderById()` | `String orderId` | `Response<OrderDto>` | Retrieve order by ID |
| `getOrderByOrderNumber()` | `String orderNumber` | `Response<OrderDto>` | Retrieve order by order number |
| `getMyOrders()` | `OrderSearchRequest request` | `Response<PagingResponse<OrderDto>>` | Customer views their orders |
| `cancelOrder()` | `CancelOrderRequest request` | `Response<OrderDto>` | Customer/Admin cancels order |

#### Admin Endpoints

| Function | Parameters | Return Type | Purpose |
|----------|-----------|------------|---------|
| `searchOrders()` | `OrderSearchRequest request` | `Response<PagingResponse<OrderDto>>` | Admin searches orders with filters |
| `updateShipping()` | `UpdateShippingRequest request` | `Response<OrderDto>` | Admin updates shipping info |
| `updateDelivery()` | `UpdateDeliveryRequest request` | `Response<OrderDto>` | Admin updates delivery date |
| `updateAdminNotes()` | `UpdateAdminNotesRequest request` | `Response<OrderDto>` | Admin adds/updates notes |
| `confirmOrder()` | `ConfirmOrderRequest request` | `Response<OrderDto>` | Admin confirms order |
| `completeOrder()` | `CompleteOrderRequest request` | `Response<OrderDto>` | Admin marks order complete |
| `getPendingOrders()` | None | `Response<List<OrderDto>>` | Admin views pending orders |
| `getInProgressOrders()` | None | `Response<List<OrderDto>>` | Admin views in-progress orders |
| `getOrderStatistics()` | `String startDate`, `String endDate` | `Response<?>` | Admin gets order statistics |

#### Status Transitions

| Function | Parameters | Return Type | Purpose |
|----------|-----------|------------|---------|
| `transitionOrder()` | `TransitionOrderRequest request` | `Response<OrderDto>` | Generic status transition (throws error — not implemented) |
| `shipOrder()` | `TransitionOrderRequest request` | `Response<?>` | Assign order to shipper, generate delivery token/PIN |
| `getDeliveryPin()` | `String orderNumber` | `Response<?>` | Admin views shipper PIN (placeholder) |
| `clearDeliveryPin()` | `String orderNumber` | `Response<?>` | Admin revokes shipper PIN |

### Dependencies

- `iOrder` service interface — delegates order business logic
- `OrderMapper` — maps between entities and DTOs
- `OrderStatusHandler` — handles order state transitions
- DTOs: Request types (`CreateOrderRequest`, `CancelOrderRequest`, `UpdateShippingRequest`, `UpdateDeliveryRequest`, `UpdateAdminNotesRequest`, `ConfirmOrderRequest`, `CompleteOrderRequest`, `TransitionOrderRequest`, `OrderSearchRequest`) and response type (`OrderDto`)
- Spring Security: `@PreAuthorize` for role-based access control
- Swagger/OpenAPI: `@Operation`, `@SecurityRequirement`, `@Tag` for documentation
- Utilities: `UUID` for token generation, `LinkedHashMap` for ordered response maps

### Design Patterns

- **Role-Based Access Control:** `@PreAuthorize` restricts endpoints to `CUSTOMER`, `ADMIN`, `SUPER_ADMIN` roles
- **Delegation:** Routes all business logic to `iOrder` service; `OrderStatusHandler` manages state machine
- **Logging:** `@Slf4j` logs all endpoint calls with order/shipper context (lines 48, 53, 58, etc.)
- **Response Templates:** Uses `LinkedHashMap` for predictable JSON key ordering in `shipOrder()` response
- **Stub Implementation:** `transitionOrder()` intentionally throws `BusinessException` (line 158) — status transitions handled via specialized endpoints

### Public API

**Customer Endpoints (CUSTOMER/ADMIN/SUPER_ADMIN):**
- `/orders` — POST → create order
- `/orders/{orderId}` — GET → retrieve by ID
- `/orders/by-number/{orderNumber}` — GET → retrieve by order number
- `/orders/my-orders` — POST → paginated list of user's orders
- `/orders/cancel` — POST → cancel order

**Admin Endpoints (ADMIN/SUPER_ADMIN):**
- `/orders/search` — POST → search with pagination and filters
- `/orders/shipping` — PUT → update shipping info
- `/orders/delivery` — PUT → update delivery date
- `/orders/admin-notes` — PUT → update admin notes
- `/orders/confirm` — POST → confirm order
- `/orders/complete` — POST → mark complete
- `/orders/pending` — GET → list pending orders
- `/orders/in-progress` — GET → list in-progress orders
- `/orders/statistics?startDate=...&endDate=...` — GET → order stats

**Order Status/Delivery:**
- `/orders/transition` — POST → generic transition (not implemented)
- `/orders/ship` — POST → assign to shipper → returns `{orderId, deliveryToken, deliveryUrl}`
- `/orders/{orderNumber}/delivery-pin` — GET → view shipper PIN
- `/orders/{orderNumber}/delivery-pin` — DELETE → clear shipper PIN

### Configuration

- **Security:** Bearer token authentication (`@SecurityRequirement(name = "bearerAuth")`)
- **Role-Based Access:** Granular `@PreAuthorize` rules per endpoint
- **Delivery Tokens:** Generated as `UUID.randomUUID()` (line 159); format `/api/delivery/{token}`
- **Logging Level:** Info level (`log.info()`) for all REST calls

---

## Cross-File Dependencies & Data Flow

```
REST Client
    ↓
authControllerImpl ─→ iUser service ─→ UserDto, AuthResponse
merchandiseControllerImpl ─→ {ProductService, CategoryService, AttributesService, iProductImage, MinioService} ─→ ProductDto, CategoryDto, AttributesDto
orderControllerImpl ─→ iOrder service + OrderStatusHandler + OrderMapper ─→ OrderDto
```

**Data Conversions:**
- Controllers receive request DTOs (JSON)
- Delegate to services which perform business logic
- Services return response DTOs wrapped in `Response<T>` or `ResponseEntity<T>`
- Controllers return responses; Spring serializes to JSON

**Security Flow:**
- `authControllerImpl`: Generates tokens; other controllers validate via `@PreAuthorize` and `HttpServletRequest`
- `orderControllerImpl`: Role-based access via `@PreAuthorize`
- `merchandiseControllerImpl`: Uses `SecurityUtil` for user context in operations