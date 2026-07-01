# REST Module Knowledge Document

## Overview

The **rest** module provides HTTP API endpoints for an ERP Spring Boot application. It contains three interface controllers that define RESTful contracts for authentication, merchandise management (products, categories, attributes), and order operations. These controllers act as the entry point for client requests, delegating business logic to service layers and returning standardized responses.

---

## AuthController

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/web/rest/AuthController.java`

**Purpose:** Manages user authentication, registration, token refresh, password reset, and profile operations.

### Key Methods

| Method | Endpoint | Parameters | Returns | Purpose |
|--------|----------|-----------|---------|---------|
| `login` | `POST /api/auth/login` | `UserLoginRequest` | `Response<AuthResponse>` | Authenticate user and return tokens |
| `register` | `POST /api/auth/register` | `UserRegisterRequest` | `Response<RegisterResponse>` | Create new user account |
| `verifyEmail` | `GET /api/auth/verify-email` | `token` (query param) | `Response<String>` | Verify email via token |
| `resetPassword` | `POST /api/auth/reset-password` | `code`, `AccountVerificationRequest` | `Response<String>` | Reset password using verification code |
| `refreshToken` | `POST /api/auth/refresh-token` | `RefreshTokenRequest` | `Response<AuthResponse>` | Generate new access token from refresh token |
| `sendPasswordResetCode` | `GET /api/auth/send-reset-code/{email}` | `email` (path var) | `Response<String>` | Send password reset code to email |
| `logout` | `POST /api/auth/logout` | `HttpServletRequest` | `ResponseEntity<?>` | Invalidate user session |
| `getMyProfile` | `GET /api/auth/me` | None | `Response<MyProfileResponse>` | Retrieve authenticated user profile (requires `!hasRole('USER')`) |
| `updateMyProfile` | `PUT /api/auth/me` | `UpdateProfileRequest` | `Response<MyProfileResponse>` | Update user profile data |
| `uploadAvatar` | `POST /api/auth/me/avatar` | `MultipartFile` (multipart form) | `Response<MyProfileResponse>` | Upload user avatar image |

### Dependencies

- **DTOs:** `UserLoginRequest`, `UserRegisterRequest`, `AccountVerificationRequest`, `RefreshTokenRequest`, `UpdateProfileRequest`, `AuthResponse`, `RegisterResponse`, `MyProfileResponse`
- **Response Wrapper:** `ResponseConfig.Response<T>`
- **Security:** Spring Security annotations (`@PreAuthorize`)
- **Validation:** Jakarta validation (`@Valid`)

### Design Patterns

- **Interface-based API Contract:** Uses interface definition for implementation flexibility
- **Standardized Response Wrapper:** All responses wrapped in generic `Response<T>` container
- **DTOs for Encapsulation:** Separates request/response objects from domain models

### Public API

Exposes authentication lifecycle endpoints: login, register, email verification, password reset, token refresh, logout, and profile management. Authorization controlled via `@PreAuthorize` on `getMyProfile`.

### Configuration

- **Base Path:** `/api/auth`
- **HTTP Status Codes:** `200 OK`, `201 CREATED`
- **Content Type:** JSON (default) and multipart form data for avatar upload

---

## MerchandiseController

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/web/rest/MerchandiseController.java`

**Purpose:** Manages product catalog (CRUD, search, images), categories, and product attributes with full CRUD operations and media handling.

### Key Methods

#### Product Operations

| Method | Endpoint | Parameters | Returns | Purpose |
|--------|----------|-----------|---------|---------|
| `addProduct` | `POST /api/merchandise/add-Product` | `CreateProductRequest` | `Response<?>` | Create new product |
| `updateProduct` | `PUT /api/merchandise/update-Product` | `UpdateProductRequest` | `Response<?>` | Update existing product |
| `deleteProduct` | `DELETE /api/merchandise/delete-Product` | `ids` (query param list) | `Response<?>` | Delete products by IDs |
| `searchProduct` | `POST /api/merchandise/search-Product` | `GetProductRequest` | `Page<ProductDto>` | Search products with pagination |
| `getProductsByIds` | `GET /api/merchandise/products` | `ids` (query param list) | `Response<List<ProductDto>>` | Retrieve products by IDs |
| `getProductsBySkus` | `GET /api/merchandise/products/by-skus` | `skus` (query param list) | `Response<List<ProductDto>>` | Retrieve products by SKU codes |
| `checkProduct` | `GET /api/merchandise/checkProduct/{name}` | `name` (path var) | `ProductIsExiting` | Check if product exists |
| `incrementViewCount` | `POST /api/merchandise/view-Product/{productId}` | `productId` (path var) | `Response<?>` | Increment product view counter |

#### Product Image Operations

| Method | Endpoint | Parameters | Returns | Purpose |
|--------|----------|-----------|---------|---------|
| `addProductImages` | `POST /api/merchandise/add-Product-Images/{productId}` | `productId` (path), `images` (multipart) | `Response<?>` | Add images to product |
| `deleteProductImage` | `DELETE /api/merchandise/delete-Product-Image/{productId}` | `productId`, `imageKey` (query) | `Response<?>` | Remove specific image |
| `replaceProductImages` | `PUT /api/merchandise/replace-Product-Images/{productId}` | `productId` (path), `images` (multipart) | `Response<?>` | Replace all product images |
| `viewProductImage` | `GET /api/merchandise/view-image/{imageName}` | `imageName` (path var) | `byte[]` | Retrieve product image (JPEG) |

#### Category Operations

| Method | Endpoint | Parameters | Returns | Purpose |
|--------|----------|-----------|---------|---------|
| `addCategory` | `POST /api/merchandise/add-Category` | `name` (query param) | `Response<?>` | Create category |
| `updateCategory` | `PUT /api/merchandise/update-Category` | `UpdateCategoryRequest` | `Response<?>` | Update category |
| `deleteCategory` | `DELETE /api/merchandise/delete-Category` | `ids` (query param list) | `Response<?>` | Delete categories |
| `searchCategory` | `POST /api/merchandise/search-Category` | `CategorySearchRequest` | `Response<PagingResponse<CategoryDto>>` | Search categories with pagination |
| `getCategoriesByIds` | `GET /api/merchandise/categories` | `ids` (query param list) | `Response<List<CategoryDto>>` | Retrieve categories by IDs |
| `getCategoriesBySkus` | `GET /api/merchandise/categories/by-skus` | `skus` (query param list) | `Response<List<CategoryDto>>` | Retrieve categories by SKUs |
| `check` | `GET /api/merchandise/checkCategory/{name}` | `name` (path var) | `CategoryExitingResponse` | Check if category exists |

#### Attributes Operations

| Method | Endpoint | Parameters | Returns | Purpose |
|--------|----------|-----------|---------|---------|
| `addAttributes` | `POST /api/merchandise/add-Attributes` | `CreateAttributesRequest` | `Response<List<AttributesDto>>` | Create product attributes |
| `updateAttributes` | `PUT /api/merchandise/update-Attributes` | `UpdateAttributesRequest` | `Response<?>` | Update attributes |
| `deleteAttributes` | `DELETE /api/merchandise/delete-Attributes` | `ids` (query param list) | `Response<?>` | Delete attributes |
| `deleteAttributesByProduct` | `DELETE /api/merchandise/delete-Attributes-by-Product/{productId}` | `productId` (path) | `Response<?>` | Delete all attributes for product |
| `searchAttributes` | `POST /api/merchandise/search-Attributes` | `AttributesSearchRequest` | `Response<PagingResponse<AttributesDto>>` | Search attributes with pagination |
| `getAttributesByIds` | `GET /api/merchandise/attributes` | `ids` (query param list) | `Response<List<AttributesDto>>` | Retrieve attributes by IDs |
| `getAttributesBySkus` | `GET /api/merchandise/attributes/by-skus` | `skus` (query param list) | `Response<List<AttributesDto>>` | Retrieve attributes by SKUs |

### Dependencies

- **DTOs:** `ProductDto`, `CategoryDto`, `AttributesDto`, `CreateProductRequest`, `UpdateProductRequest`, `GetProductRequest`, `UpdateCategoryRequest`, `CategorySearchRequest`, `CreateAttributesRequest`, `UpdateAttributesRequest`, `AttributesSearchRequest`
- **Response Types:** `Response<T>`, `PagingResponse<T>`
- **Media:** `MultipartFile` for image uploads

### Design Patterns

- **CRUD Convention:** Standard create, read, update, delete operations
- **Bulk Operations:** Delete multiple items via list parameters
- **Search/Filter Pattern:** Dedicated search endpoints accepting request objects with pagination
- **Multi-lookup:** Support retrieval by IDs and SKUs
- **Existence Check:** Separate endpoints to verify entity existence before operations

### Public API

Exposes catalog management: products (CRUD, search, images), categories (CRUD, search), and attributes (CRUD, search). All operations follow RESTful conventions with consistent response wrapping.

### Configuration

- **Base Path:** `/api/merchandise`
- **Media Types:** `MULTIPART_FORM_DATA_VALUE` for uploads, `IMAGE_JPEG_VALUE` for image retrieval
- **HTTP Status:** `201 CREATED` (add), `200 OK` (read/update), `204 NO_CONTENT` (delete)

---

## OrderController

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/web/rest/OrderController.java`

**Purpose:** Manages order lifecycle from creation through completion, supporting customer operations, admin state transitions, shipping/delivery management, and delivery PIN generation.

### Key Methods

#### Customer Order Operations

| Method | Endpoint | Parameters | Returns | Purpose |
|--------|----------|-----------|---------|---------|
| `createOrder` | `POST /api/orders` | `CreateOrderRequest` | `Response<OrderDto>` | Create new order |
| `getOrderById` | `GET /api/orders/{orderId}` | `orderId` (path) | `Response<OrderDto>` | Retrieve order by ID |
| `getOrderByOrderNumber` | `GET /api/orders/number/{orderNumber}` | `orderNumber` (path) | `Response<OrderDto>` | Retrieve order by order number |
| `getMyOrders` | `POST /api/orders/my-orders` | `OrderSearchRequest` | `Response<PagingResponse<OrderDto>>` | Get current user's orders with pagination |
| `cancelOrder` | `POST /api/orders/cancel` | `CancelOrderRequest` | `Response<OrderDto>` | Cancel pending order |

#### Admin Order Operations

| Method | Endpoint | Parameters | Returns | Purpose |
|--------|----------|-----------|---------|---------|
| `searchOrders` | `POST /api/orders/search` | `OrderSearchRequest` | `Response<PagingResponse<OrderDto>>` | Search all orders with filters/pagination |
| `updateShipping` | `PUT /api/orders/shipping` | `UpdateShippingRequest` | `Response<OrderDto>` | Update shipping information |
| `updateDelivery` | `PUT /api/orders/delivery` | `UpdateDeliveryRequest` | `Response<OrderDto>` | Update delivery information |
| `updateAdminNotes` | `PUT /api/orders/admin-notes` | `UpdateAdminNotesRequest` | `Response<OrderDto>` | Add/update admin notes |
| `confirmOrder` | `POST /api/orders/confirm` | `ConfirmOrderRequest` | `Response<OrderDto>` | Confirm order after payment |
| `completeOrder` | `POST /api/orders/complete` | `CompleteOrderRequest` | `Response<OrderDto>` | Mark order as completed |
| `getPendingOrders` | `GET /api/orders/pending` | None | `Response<List<OrderDto>>` | Retrieve all pending orders |
| `getInProgressOrders` | `GET /api/orders/in-progress` | None | `Response<List<OrderDto>>` | Retrieve all in-progress orders |
| `getOrderStatistics` | `GET /api/orders/statistics` | `startDate`, `endDate` (query params) | `Response<?>` | Get order statistics within date range |

#### Order State Transitions (Dashboard)

| Method | Endpoint | Parameters | Returns | Purpose |
|--------|----------|-----------|---------|---------|
| `transitionOrder` | `POST /api/orders/transition` | `TransitionOrderRequest` | `Response<OrderDto>` | Change order status (PROCESSING, DELIVERED, READY_FOR_PICKUP, RETURNING, RETURNED, COMPLETED) |
| `shipOrder` | `POST /api/orders/ship` | `TransitionOrderRequest` | `Response<?>` | Transition to SHIPPED status with driver info; generates delivery token + PIN stored in Redis |
| `getDeliveryPin` | `GET /api/orders/delivery-pin/{orderNumber}` | `orderNumber` (path) | `Response<?>` | Retrieve current delivery PIN for order |
| `clearDeliveryPin` | `DELETE /api/orders/delivery-pin/{orderNumber}` | `orderNumber` (path) | `Response<?>` | Clear delivery PIN; forces driver to generate new PIN on next access |

### Dependencies

- **DTOs:** `OrderDto`, `CreateOrderRequest`, `OrderSearchRequest`, `CancelOrderRequest`, `UpdateShippingRequest`, `UpdateDeliveryRequest`, `UpdateAdminNotesRequest`, `ConfirmOrderRequest`, `CompleteOrderRequest`, `TransitionOrderRequest`
- **Response Types:** `Response<T>`, `PagingResponse<T>`
- **External:** Redis (implied for PIN storage in `shipOrder`)

### Design Patterns

- **Workflow State Machine:** Order transitions follow defined state paths via `transitionOrder` and `shipOrder`
- **Role-based Operations:** Customer operations (create, cancel, view own) vs. admin operations (search, confirm, complete, ship)
- **Pin-based Authentication:** Delivery PIN generation and management for driver verification
- **Temporal Queries:** Statistics endpoint accepts date range for time-series analysis
- **Separation of Concerns:** Distinct endpoints for shipping vs. delivery vs. status transitions

### Public API

Exposes order management: customer order CRUD, state transitions, admin oversight (search, statistics), and delivery PIN management for driver authentication.

### Configuration

- **Base Path:** `/api/orders`
- **HTTP Status:** `201 CREATED` (create), `200 OK` (read/update), `204 NO_CONTENT` (implied for deletes)
- **External State:** Delivery PINs stored in Redis (referenced in `shipOrder` and retrieval methods)
- **Order States:** PROCESSING, SHIPPED, DELIVERED, READY_FOR_PICKUP, RETURNING, RETURNED, COMPLETED, PENDING

---

## Data Flow

**Authentication Flow:**
1. Client calls `login` → receives `AuthResponse` with tokens
2. Token refresh: `refreshToken` accepts expired access token, returns new `AuthResponse`
3. Profile access: `getMyProfile` retrieves user data; `updateMyProfile` modifies it
4. Avatar upload: `uploadAvatar` handles multipart image, returns updated profile

**Merchandise Flow:**
1. Products: CRUD via `addProduct`/`updateProduct`/`deleteProduct`; search via `searchProduct`
2. Images: Uploaded via `addProductImages` (multipart), retrieved via `viewProductImage` (returns JPEG bytes)
3. Categories/Attributes: Parallel structure—CRUD and search operations mirror products
4. Lookup: By ID or SKU for flexible access patterns

**Order Flow:**
1. Customer creates order: `createOrder` → `OrderDto` returned
2. Order confirmation: Admin calls `confirmOrder` after payment
3. State transitions: `transitionOrder` for standard status changes; `shipOrder` for SHIPPED (generates PIN)
4. Delivery: Driver authenticates via PIN (`getDeliveryPin`); admin can clear PIN (`clearDeliveryPin`)
5. Completion: `completeOrder` finalizes order
6. Admin oversight: `searchOrders`, `getPendingOrders`, `getInProgressOrders`, `getOrderStatistics`

---

## Cross-Module Integration

All controllers return standardized response wrappers (`Response<T>`, `PagingResponse<T>`) from shared `ResponseConfig`, enabling consistent client-side handling. Request DTOs include validation annotations (`@Valid`), delegating validation to underlying service layer. Multipart handling in both `AuthController` (avatar) and `MerchandiseController` (product images) suggests shared file storage infrastructure.