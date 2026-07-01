# Response Group Module - Comprehensive Knowledge Document

## Overview

The **response** group comprises 44 files organizing an ERP Spring Boot application's response DTOs, domain events, event-driven architecture, error handling, search capabilities, and security infrastructure. It facilitates communication between service layers and clients through structured response objects, manages asynchronous event processing via Kafka, and provides centralized error handling with RFC 7807 Problem Details compliance.

---

## Response DTOs

### AuthResponse
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/response/AuthResponse.java`

**Purpose:** Encapsulates authentication response data returned after login/registration.

**Key Fields:**
- `message: String` — Operation status message
- `accessToken: String` — JWT access token
- `refreshToken: String` — JWT refresh token
- `username: String` — Authenticated user's username
- `avatarUrl: String` — User profile avatar URL
- `email: String` — User email address
- `phoneNumber: String` — User phone number
- `gender: Gender` — User gender enum
- `roles: Set<RoleType>` — User assigned roles (default: empty HashSet)

**Design Pattern:** Builder pattern (Lombok `@Builder`), field access control via `@FieldDefaults(AccessLevel.PRIVATE)`.

---

### MyProfileResponse
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/response/MyProfileResponse.java`

**Purpose:** Returns authenticated user's complete profile information.

**Key Fields:**
- `username, fullName, email, phoneNumber, avatarUrl: String`
- `dateOfBirth: Date`
- `gender: Gender`, `rank: UserRank`, `status: ActiveStatus`
- `roles: Set<RoleType>`

**Usage:** Fetched via profile endpoints; includes enums for user state/rank tracking.

---

### DeviceInfoResponse
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/response/DeviceInfoResponse.java`

**Purpose:** Provides device-specific authentication tokens for multi-device sessions.

**Key Fields:**
- `finalRefreshTokenString: String` — Updated refresh token per device
- `accessToken: String` — Device-specific access token
- `message: String` — Operation status

---

### CategoryExitingResponse & ProductIsExiting
**Files:**
- `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/response/CategoryExitingResponse.java`
- `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/response/ProductIsExiting.java`

**Purpose:** Check existence of categories/products in the system.

**Structure:**
- `id: String` — Entity identifier
- `isExiting: boolean` — Existence flag

---

### RegisterResponse
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/response/RegisterResponse.java`

**Purpose:** Simple acknowledgment response for user registration.

**Key Fields:**
- `message: String` — Registration confirmation message

---

### ProductPublicRecord (GraphQL)
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/response/graphql/ProductPublicRecord.java`

**Purpose:** Public-facing product record for customer UI, hiding sensitive internal data.

**Fields:**
- `sku: String` — Public product identifier (replaces internal ID)
- `name: String`
- `mediaItems: List<MediaItemDto>`
- `discountPercent: Double`
- `categoryName: String`
- `status: String`

**Design Pattern:** Immutable record; excludes internal fields like `totalRevenue`, `viewCount`, `totalOrders`.

---

## Domain Events

### VerificationEmailEvent
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/domainevent/VerificationEmailEvent.java`

**Purpose:** Event published when user registration requires email verification.

**Fields:**
- `email: String` — Target email
- `username: String` — User identifier
- `emailVerificationToken: String` — Verification link token
- `purpose: ActiveStatus` — Context (registration, password reset, etc.)

**Listener:** `VerificationEmailListener` (async, `@TransactionalEventListener`)

---

### SendCodeResetPassword
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/domainevent/SendCodeResetPassword.java`

**Purpose:** Event triggered for password reset OTP distribution.

**Fields:**
- `user: User` — Target user entity
- `code: String` — One-time password code

**Listener:** `ResetPasswordListener` (post-commit transaction phase)

---

### SaveDeviceInfo
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/domainevent/SaveDeviceInfo.java`

**Purpose:** Captures device session information for multi-device tracking.

**Fields:**
- `userInfo: User` — Associated user
- `deviceInfo: DeviceInfo` — Device embedded entity
- `purpose: ActiveStatus` — Session purpose (login, logout, device registration)

---

## Event Producers

### ActiveLogProducer
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/event/producer/ActiveLogProducer.java`

**Purpose:** Publishes user activity logs to Kafka `active-log` topic.

**Key Method:**
- `sendLog(ActiveLogDto message): void` — Async send with completion callback logging

**Topic:** `KafkaTopics.ACTIVE_LOG_TOPIC` ("active-log")

**Pattern:** CompletableFuture-based async confirmation.

---

### OrderKafkaProducer
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/event/producer/OrderKafkaProducer.java`

**Purpose:** Publishes order creation events to order processing topic.

**Key Method:**
- `sendOrderCreatedEvent(OrderEventDto orderEventDto): void` — Sends with order ID as key

**Topic:** `KafkaTopics.ORDER_TOPIC` ("order-topic")

---

### ProductEventProducer
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/event/producer/ProductEventProducer.java`

**Purpose:** Publishes product lifecycle events (create, update, delete) for Elasticsearch synchronization.

**Key Methods:**
- `publishProductCreated(Long productId): void`
- `publishProductUpdated(Long productId): void`
- `publishProductDeleted(Long productId): void`

**Payload Structure:**
```json
{ "productId": <Long>, "eventType": "<PRODUCT_CREATED|UPDATED|DELETED>" }
```

**Topic:** "product-events"

---

## Event Consumers

### PaymentResultConsumer
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/event/consumer/PaymentResultConsumer.java`

**Purpose:** Consumes payment gateway responses and updates order status.

**Key Method:**
- `consume(String msg, long offset, int pt, String key): void` — Transactional listener

**Topic:** `KafkaTopics.PAYMENT_RESULT_TOPIC` ("payment-result")

**Processing Logic:**
1. Parse JSON payload to extract `orderNumber` and `status`
2. Retrieve order from repository
3. Validate current status (must be `WAITING_PAYMENT` or `PENDING`)
4. On `SUCCESS`: transition to `CONFIRMED` → `PROCESSING`, confirm inventory reservation
5. On `FAILED`: transition to `FAILED`, save outbox event
6. Log and persist changes

**Dependencies:** `OrderRepository`, `OrderInventoryService`, `OutboxOrderHelper`, `ObjectMapper`

---

### ElasticsearchSyncListener
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/event/listener/ElasticsearchSyncListener.java`

**Purpose:** Syncs product data changes to Elasticsearch index.

**Topic:** "product-events"

**Key Method:**
- `handleProductEvent(String message): void` — Parses event, syncs or deletes document

**Processing:**
1. Extract `productId` and `eventType` from JSON
2. If `PRODUCT_DELETED`: remove from Elasticsearch
3. Else: fetch full product details, map to `ProductDocument`, save to index

**Mapping Logic:** `mapToDocument(Product)` converts Product entity with nested attributes to ProductDocument with AttributeDocument list.

---

## Event Listeners

### BaseEventListener
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/event/base/BaseEventListener.java`

**Purpose:** Abstract base providing common dependencies for domain event listeners.

**Protected Fields:**
- `emailService: EmailService`
- `jwtService: JwtService`
- `userDetailsService: UserDetailsServiceImpl`
- `serverPort: String` — Injected from `server.port`

---

### VerificationEmailListener
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/event/email/VerificationEmailListener.java`

**Purpose:** Sends email verification link on user registration/email change.

**Key Method:**
- `handleVerificationEmail(VerificationEmailEvent body): void` — Async, post-commit

**Email Construction:**
- URL: `{frontendUrl}/verify-email?token={emailVerificationToken}`
- Dependency: `EmailService.sendVerificationEmail(email, username, verificationUrl)`

**Configuration:** `frontend.url` property required.

---

### ResetPasswordListener
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/event/email/ResetPasswordListener.java`

**Purpose:** Sends OTP email for password reset requests.

**Key Method:**
- `handleSendCodeResetPassword(SendCodeResetPassword body): void` — Post-commit phase, throws `MessagingException` if email fails

**Dependency:** `EmailService.sendPasswordResetOtpEmail(email, username, code)`

---

## Error Handling

### ErrorCode Enum
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/web/rest/error/ErrorCode.java`

**Purpose:** Centralized error code registry with HTTP status mappings.

**Key Codes:**
- `PRODUCT_NOT_FOUND` (404)
- `CATEGORY_NOT_FOUND` (404)
- `INVALID_REQUEST` (400)
- `UNAUTHORIZED` (401)
- `FORBIDDEN` (403)
- `INSUFFICIENT_STOCK`, `ATTRIBUTES_OUT_OF_STOCK` (400)
- `INVALID_STATUS_TRANSITION` (400)
- `INVALID_CREDENTIALS` (401)
- `CATEGORY_ALREADY_EXISTS` (409)

**Methods:**
- `getTitle(): String` — Human-readable message
- `getCode(): String` — Enum name
- `getHttpStatus(): HttpStatus`
- `getType(): URI` — RFC 7807 problem type (default: "about:blank")

---

### BusinessException
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/web/rest/error/BusinessException.java`

**Purpose:** Application-level exception wrapping `ErrorCode` with contextual properties.

**Constructors:**
- `BusinessException(ErrorCode)` — Uses default error message
- `BusinessException(ErrorCode, String detail)` — Custom detail message
- `BusinessException(ErrorCode, String detail, Throwable cause)`

**Fluent API:**
- `with(String key, Object value): BusinessException` — Add contextual properties

**Example:**
```java
throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "Stock unavailable")
    .with("availableStock", 5)
    .with("requestedQuantity", 10);
```

**Public API:**
- `getCode(), getTitle(), getHttpStatus(), getType(): ...`
- `hasProperties(): boolean`
- `getProperties(): Map<String, Object>`

---

### GlobalExceptionHandler
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/web/rest/error/GlobalExceptionHandler.java`

**Purpose:** REST controller advice implementing RFC 7807 Problem Details for all exceptions.

**Handlers:**

1. **`@ExceptionHandler(BusinessException.class)`**
   - Converts to `ProblemDetail` with status, title, detail, errorCode
   - Includes optional `details` map if properties present

2. **`@Override handleMethodArgumentNotValid(...)`**
   - Field validation errors from `@Valid`
   - Returns `VALIDATION_FAILED` with field error map

3. **`@ExceptionHandler(ConstraintViolationException.class)`**
   - Path variable / request param validation
   - Returns violations map

4. **`@ExceptionHandler(AccessDeniedException.class)`**
   - Spring Security access denied
   - Returns 403 with `ACCESS_DENIED` code

5. **`@ExceptionHandler(Exception.class)`** — Fallback
   - Unhandled exceptions logged at ERROR level
   - Returns generic 500 response

**Response Structure:**
```json
{
  "type": "about:blank",
  "title": "...",
  "status": 400,
  "detail": "...",
  "errorCode": "...",
  "details": { /* optional */ },
  "fieldErrors": { /* optional */ }
}
```

---

## Search Service

### ProductElasticSearchService
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/search/ProductElasticSearchService.java`

**Purpose:** Elasticsearch queries for product discovery with multi-faceted filtering.

**Key Methods:**

1. **`searchProductIds(GetProductRequest request): List<Long>`**
   - Builds Bool query with:
     - Multi-match on keyword: `name`, `attributes.name`, `attributes.keywords`, `sku`, `attributes.sku`
     - Filter by category ID (term query)
     - Filter by statuses (terms query)
   - Returns paginated product IDs
   - **Dependencies:** `ElasticsearchOperations`, `GetProductRequest.paging()`

2. **`countProducts(GetProductRequest request): long`**
   - Same filtering, returns count without pagination

**Query Builder:** Native Elasticsearch Java API client (co.elastic.clients)

---

## Security & User Details

### CustomUserDetails
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/UserDetails/CustomUserDetails.java`

**Purpose:** Spring Security `UserDetails` implementation wrapping User entity.

**Fields:**
- `id, username, email, password: String`
- `authorities: Collection<? extends GrantedAuthority>`
- `enabled: boolean` (always true)

**Construction:** `CustomUserDetails(User user, Collection<GrantedAuthority> authorities)`

**All account status checks return `true`:** non-expired, non-locked, credentials valid.

---

### UserDetailsServiceImpl
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/UserDetails/UserDetailsServiceImpl.java`

**Purpose:** Spring Security's `UserDetailsService` for authentication.

**Key Method:**
- `loadUserByUsername(String username): UserDetails` — Transactional

**Logic:**
1. Check if input is email (contains "@"), query by email; else query by username
2. Throw `UsernameNotFoundException` if not found
3. Convert roles to authorities: `ROLE_{RoleType.name()}`
4. Return `CustomUserDetails`

**Dependencies:** `UserRepository`

---

## Supporting Classes

### SmartRequestInterceptor
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/common/interceptor/SmartRequestInterceptor.java`

**Purpose:** HTTP interceptor enriching request with user context attributes.

**preHandle Logic:**
- Extract current user ID and role flags via `SecurityUtil`
- Set request attributes: `X-User-Id`, `X-Is-Admin`, `X-Is-Staff`
- Return true (allow continuation)

**Used for:** Audit logging, role-based request routing.

---

### NormalizedId Annotation & Validator
**Files:**
- `src/main/java/com/anno/ERP_SpringBoot_Experiment/common/annotation/NormalizedId.java`
- `src/main/java/com/anno/ERP_SpringBoot_Experiment/common/annotation/NormalizedIdValidator.java`

**Purpose:** Bean validation constraint for ID fields (alphanumeric, dash, underscore).

**Regex:** `^[a-zA-Z0-9_-]+$`

**Target:** Methods, fields, parameters at runtime.

---

### KafkaTopics Constants
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/common/constants/KafkaTopics.java`

**Topic Registry:**
- `ACTIVE_LOG_TOPIC` = "active-log"
- `ORDER_TOPIC` = "order-topic"
- `ORDER_RESPONSE_TOPIC` = "order-response-topic"
- `PAYMENT_RESULT_TOPIC` = "payment-result"

---

## Service Layer DTOs

### UserDto
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/UserDto.java`

**Purpose:** Transfer object for user data in service layer operations.

**Fields:** `id, username, fullName, email, numberPhone, avatarUrl, dateOfBirth, gender, active, roles`

---

### VariantOptionDto & VariantValueInput
**Files:**
- `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/VariantOptionDto.java`
- `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/request/VariantValueInput.java`

**Purpose:** Product attribute variant handling.

**Structure:**
- `VariantOptionDto`: `name, values: List<String>`
- `VariantValueInput`: Single `value: String`

---

### OrderEventDto & Related
**Files:**
- `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/kafkaDtos/OrderEventDto.java`
- `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/kafkaDtos/CustomerInfo.java`
- `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/kafkaDtos/PaymentOptions.java`

**Purpose:** Payment gateway integration payloads.

**OrderEventDto:**
- `paymentProvider: String` (VNPAY, MOMO, ZALOPAY, PAYPAL)
- `amount: double, currency: String` (VND, USD)
- `orderId, orderDescription: String`
- `customerInfo: CustomerInfo, paymentOptions: PaymentOptions`

**CustomerInfo:** `ipAddress, appUserId, language`

**PaymentOptions:** `paymentMethod, bankCode, extraData`

---

## Base Model Classes

### IdentityOnly
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/model/base/IdentityOnly.java`

**Purpose:** Base mapped superclass providing only ID identity.

**Fields:**
- `id: T extends Serializable` — Auto-generated primary key

**Overrides:**
- `equals()` — ID-based comparison, Hibernate proxy safe
- `hashCode()` — Class-based for uninitialized entities

---

### BaseEntity
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/model/base/BaseEntity.java`

**Purpose:** Extends `IdentityOnly` with audit fields and soft delete support.

**Audit Fields:**
- `createdBy, updatedBy, deletedBy: String`
- `createdAt, updatedAt, deletedAt: LocalDateTime`

**Soft Delete:**
- `isDeleted: Boolean` (default false)
- `updateHistory: List<AuditEntry>` (CLOB-persisted via `AuditEntryListConverter`)

**Helper Methods:**
- `addUpdateEntry(String action, String updatedBy)` — Log state change
- `markDeletedAfter30Days(String deletedByUser)` — Schedule soft delete
- `markDeletedNow(String deletedByUser)` — Immediate soft delete
- `restore()` — Undo soft delete
- `isSoftDeleted(): boolean`

---

## Elasticsearch Documents

### ProductDocument
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/model/document/ProductDocument.java`

**Purpose:** Elasticsearch index document for product search.

**Index:** "products"

**Fields:**
- `id: String` — ES document ID
- `productId: Long` — DB foreign key
- `name: String` (text, standard analyzer)
- `sku, categoryName: String` (keyword)
- `categoryId: Long`
- `status: ActiveStatus` (keyword)
- `attributes: List<AttributeDocument>` (nested)
- `discountPercent: Double, totalSoldQuantity: Integer, averageRating: Double`

---

### AttributeDocument
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/model/document/AttributeDocument.java`

**Purpose:** Nested product attribute for Elasticsearch.

**Fields:**
- `attributeId: Long, name: String` (text)
- `sku: String` (keyword)
- `price, salePrice: Double`
- `statusProduct: StockStatus` (keyword)
- `keywords: Set<String>` (keyword)

---

### ProductSearchRepository
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/repository/search/ProductSearchRepository.java`

**Purpose:** Spring Data Elasticsearch repository for ProductDocument.

**Key Method:**
- `findByProductId(Long productId): Optional<ProductDocument>`

---

## Application Bootstrap

### ErpSpringBootExperimentApplication
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/ErpSpringBootExperimentApplication.java`

**Purpose:** Spring Boot application entry point.

**Annotations:**
- `@SpringBootApplication`
- `@EnableCaching` — Activates Spring cache abstraction
- `@EnableScheduling` — Enables `@Scheduled` tasks
- `@EnableConfigurationProperties({ SecurityProperties.class, MinioProperties.class, CacheProperties.class })`

**Main Logic:**
- Runs Spring Application
- Logs startup info: protocol (http/https), port, context path, host, Swagger UI path, active profiles

**Configuration Injection:** `server.ssl.key-store`, `server.port`, `server.servlet.context-path`, `springdoc.swagger-ui.path`

---

## Additional Services

### ActiveLogService
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/KafkaService/ActiveLogService.java`

**Purpose:** Orchestrates active log production to Kafka.

**Key Method:**
- `sendMessage(ActiveLogDto activeLogDto): void` — Enriches DTO with timestamp, forwards to producer

**Dependencies:** `ActiveLogProducer`

---

### ProductCachingService
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/Recommendation/ProductCachingService.java`

**Purpose:** Caches product recommendation lists in Redis.

**Key Method:**
- `addProduct(List<ProductDto> items): void` — Generates UUID key, validates products exist, stores `ProductCachingDto` in Redis

**Redis Key Format:** `rec:{recommendationId}`

**Dependencies:** `RedisService`, `ProductRepository`, `ProductMapper`

---

## FilterRequest Utility
**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/request/utils/FilterRequest.java`

**Purpose:** Abstract base for paginated search/filter requests.

**Fields:**
- `paging: PagingRequest` (default initialized)

**Abstract Method:**
- `specification(): Specification<T>` — Subclasses define JPA Specification for dynamic queries

---

## Data Flow Summary

### Authentication Flow
1. User credentials → `UserDetailsServiceImpl.loadUserByUsername()`
2. User + roles → `CustomUserDetails` → Spring Security authentication
3. JWT tokens generated → `AuthResponse` returned

### Event-Driven Order Processing
1. Order created → `OrderKafkaProducer` sends to "order-topic"
2. Payment gateway response → `PaymentResultConsumer` updates order status
3. Status change triggers `OutboxOrderHelper.saveOrderStatusChangedEvent()`
4. If payment succeeds: inventory reserved, order transitioned to PROCESSING

### Product Lifecycle & Search
1. Product CRUD → `ProductEventProducer` publishes event
2. `ElasticsearchSyncListener` consumes event → syncs/deletes from "products" index
3. Search requests → `ProductElasticSearchService.searchProductIds()` queries Elasticsearch
4. Results mapped back to database entities

### Email Notifications
1. User registration → `VerificationEmailEvent` published (post-transaction)
2. `VerificationEmailListener` (async) → `EmailService.sendVerificationEmail()`
3. Password reset → `SendCodeResetPassword` → `ResetPasswordListener` → OTP email

### User Tracking
1. HTTP request → `SmartRequestInterceptor` extracts user context
2. Request attributes set: `X-User-Id`, `X-Is-Admin`, `X-Is-Staff`
3. `ActiveLogService` publishes audit log to Kafka
4. Activity logged for compliance/analytics

---

## Public API Summary

| Component | Exposes |
|-----------|---------|
| **AuthResponse** | User auth tokens, roles, profile data |
| **MyProfileResponse** | Complete user profile with rank/status |
| **ProductPublicRecord** | Sanitized product details (SKU-based, no internals) |
| **ErrorCode/BusinessException** | Typed errors with RFC 7807 Problem Details |
| **ProductElasticSearchService** | Product ID search with filtering/pagination |
| **CustomUserDetails** | Spring Security UserDetails |
| **Event Producers** | Kafka topic integration (order, product, logs) |
| **Email Listeners** | Async verification/password reset emails |

---

## Configuration Dependencies

- **`server.port`** — HTTP server port (consumed by app bootstrap and interceptor)
- **`server.ssl.key-store`** — SSL configuration (determines http vs https)
- **`server.servlet.context-path`** — Application context path
- **`springdoc.swagger-ui.path`** — Swagger UI endpoint
- **`frontend.url`** — Frontend base URL (for email verification links)
- **Kafka brokers** — Message broker configuration
- **Elasticsearch** — Search index configuration
- **Redis** — Caching backend
- **Email service** — SMTP configuration (implicit, used by EmailService)