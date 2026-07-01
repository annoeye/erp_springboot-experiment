# Service Layer Knowledge Document

## Group Overview

The **service** group provides business logic and infrastructure services for an ERP system built on Spring Boot. It handles cache synchronization, inventory management with distributed locking, email notifications, JWT authentication, file storage via MinIO, order processing with transactional outbox pattern, Redis caching, and token lifecycle management. Services are organized around domain concerns (orders, inventory, products) and cross-cutting infrastructure (email, storage, authentication).

---

## CacheSyncService

**Purpose:** Manages background synchronization of product cache from database to Redis. Implements a dirty-flag pattern with rate limiting to prevent resource spikes.

**Key Classes/Functions:**
- `markProductDirty(Long id)` — Marks a product ID for cache refresh. Called by business services after create/update/delete operations.
- `syncDirtyProductCaches()` — Scheduled task (cron: every 5 minutes) that retrieves dirty product IDs, queries DB with JOIN FETCH, and updates cache entries with 100ms delays between operations.

**Data Flow:** Business service → `markProductDirty()` → Set `dirtyProductIds` → Scheduled task polls set → `productRepository.findByIdWithDetails()` → `ProductMapper.toDto()` → Cache update or eviction.

**Dependencies:**
- `ProductRepository` — Provides `findByIdWithDetails(id)` for eager-loaded product data
- `ProductMapper` — Converts entities to DTOs
- `CacheManager` (Spring) — Manages cache named "productDetails"
- Lombok (`@Slf4j`, `@RequiredArgsConstructor`)

**Design Patterns:** Dirty-flag cache synchronization with scheduled batch processing and rate limiting.

**Public API:** `markProductDirty(Long id)` — Called by other services to trigger cache refresh.

**Configuration:** 
- Scheduled cron: `0 */5 * * * *` (every 5 minutes)
- Cache name: "productDetails"
- Rate limit: 100ms sleep between DB reads to limit CPU/RAM spike to ~10-15%

---

## DeviceInfoService

**Purpose:** Generates unique device identifiers from device metadata for tracking multi-device user sessions.

**Key Classes/Functions:**
- `createDeviceId(DeviceInfo deviceInfo)` — Returns formatted string combining OS name and device type (e.g., "windows:desktop", "unknown_device" if null input).

**Data Flow:** `DeviceInfo` embedded object → String concatenation → Device ID used as Redis hash field key.

**Dependencies:**
- `DeviceInfo` model (embedded)

**Design Patterns:** Simple factory method for device ID generation.

**Public API:** `createDeviceId(DeviceInfo)` — Returns `String`.

---

## EmailService

**Purpose:** Sends HTML-templated emails asynchronously via SMTP using Thymeleaf template engine.

**Key Classes/Functions:**
- `sendVerificationEmail(String to, String username, String verificationUrl)` — Async method that processes "verification-email.html" template and sends account verification email.
- `sendPasswordResetOtpEmail(String to, String username, String otp)` — Async method that processes "password-reset-otp-email.html" template and sends OTP email.
- `processHtmlTemplate(String templateName, Map<String, Object> variable)` — Renders Thymeleaf template with variables from `/mail/` directory.

**Data Flow:** Method call → Template processing → MIME message construction → Async send via `JavaMailSender`.

**Dependencies:**
- `JavaMailSender` (Spring Mail) — SMTP sender
- `SpringTemplateEngine` (Thymeleaf) — Template rendering
- `@Value("${spring.mail.username}")` — Sender email from config

**Design Patterns:** Template method pattern with async processing (`@Async`).

**Public API:** 
- `sendVerificationEmail(to, username, verificationUrl)`
- `sendPasswordResetOtpEmail(to, username, otp)`

**Configuration:**
- Mail templates location: `/mail/*.html`
- From email: `${spring.mail.username}`
- Encoding: UTF-8

---

## InventoryService

**Purpose:** Manages product stock with distributed Redis locking to prevent overselling in concurrent scenarios.

**Key Classes/Functions:**
- `reserveStock(String sku, int quantity)` — Acquires distributed lock on SKU, checks availability, decrements available, increments reserved. Returns `boolean`.
- `confirmReservation(String sku, int quantity)` — Decrements reserved qty after payment success.
- `releaseReservation(String sku, int quantity)` — Returns reserved qty to available (order cancellation).
- `checkAvailability(String sku, int quantity)` — Non-transactional check if stock exists.

**Data Flow:** SKU → Redis lock → DB query → Inventory update → DB save → Lock release.

**Dependencies:**
- `ProductInventoryRepository` — Data access for `ProductInventory` entities
- `RedissonClient` — Distributed locking via RLock
- Spring `@Transactional`

**Design Patterns:** Distributed lock pattern with try-finally for release guarantee.

**Public API:**
- `reserveStock(sku, quantity) → boolean`
- `confirmReservation(sku, quantity)`
- `releaseReservation(sku, quantity)`
- `checkAvailability(sku, quantity) → boolean`

**Configuration:**
- Lock key prefix: "inventory:lock:"
- Lock wait time: 5 seconds
- Lock lease time: 10 seconds

---

## JwtService

**Purpose:** Generates, validates, and extracts claims from JWT tokens using JJWT library.

**Key Classes/Functions:**
- `extractUsername(String token)` — Returns JWT subject claim.
- `extractClaim(String token, Function<Claims, T> claimsResolver)` — Generic claim extractor.
- `generateToken(UserDetails userDetails, long expirationTimeMillis, Map<String, Object>... extraClaims)` — Creates signed JWT with custom claims, expiration, and subject.
- `isTokenValid(String token, UserDetails userDetails)` — Validates username match and expiration.
- `extractAllClaims(String token)` — Parses and verifies JWT signature.

**Data Flow:** Token string → Base64 decode secret → HMAC-SHA verification → Claims extraction or generation.

**Dependencies:**
- `io.jsonwebtoken` (JJWT) — JWT parsing/building
- `UserDetails` (Spring Security) — User principal
- `@Value("${application.security.jwt.secret-key}")` — Base64-encoded secret key

**Design Patterns:** Facade over JJWT library with generic claim resolution.

**Public API:**
- `generateToken(userDetails, expirationMs, ...extraClaims) → String`
- `isTokenValid(token, userDetails) → boolean`
- `extractUsername(token) → String`

**Configuration:**
- Secret key: `${application.security.jwt.secret-key}` (Base64-encoded)

---

## MinioService

**Purpose:** Manages file upload/download to MinIO object storage with presigned URL generation.

**Key Classes/Functions:**
- `uploadFile(MultipartFile file)` — Generates UUID filename, uploads to MinIO, returns object name.
- `getFile(String fileName)` — Returns InputStream for file retrieval.
- `getPresignedUrl(String fileName, int expiryInSeconds)` — Generates time-limited download URL (default: 7 days).
- `deleteFile(String fileName)` — Removes object from MinIO.
- `initBucket()` — PostConstruct: ensures "images" bucket exists.

**Data Flow:** MultipartFile → UUID rename → S3-compatible upload → Object name stored in DB. Later: fetch via name → InputStream or presigned URL.

**Dependencies:**
- `MinioClient` — S3-compatible object storage client
- Spring `@PostConstruct`

**Design Patterns:** Resource wrapper with lazy initialization via PostConstruct.

**Public API:**
- `uploadFile(file) → String (objectName)`
- `getFile(fileName) → InputStream`
- `getPresignedUrl(fileName) → String`
- `getPresignedUrl(fileName, expirySeconds) → String`
- `deleteFile(fileName)`

**Configuration:**
- Bucket name: "images"
- Presigned URL default expiry: 604800 seconds (7 days)

---

## OrderInventoryService

**Purpose:** Orchestrates inventory reservation/release for order lifecycle with distributed locking to prevent overselling.

**Key Classes/Functions:**
- `reserveInventory(List<OrderItem> items)` — Acquires sorted locks on all SKUs, validates availability, reserves stock. Returns `Map<String, Integer>` of SKU → qty.
- `releaseInventory(List<OrderItem> items)` — Calls `inventoryService.releaseReservation()` for each item (on order cancellation).
- `confirmReservation(List<OrderItem> items)` — Transitions reserved → sold (on payment success).
- `validateCartItems(Map<String, Integer> skusWithQuantities)` — Returns `ValidationResult` with valid/invalid items list.

**Data Flow:** Order items → Extract SKUs → Sort (deadlock prevention) → Acquire locks → Validate each item → Reserve or throw `BusinessException` → Always release locks in finally block.

**Dependencies:**
- `ProductInventoryRepository`
- `AttributesRepository`
- `RedissonClient`
- `InventoryService` — Delegates actual stock manipulation
- Custom `BusinessException` with `ErrorCode` enum

**Design Patterns:** Orchestration with distributed multi-resource locking (sorted to prevent deadlock), exception aggregation.

**Public API:**
- `reserveInventory(items) → Map<String, Integer>`
- `releaseInventory(items)`
- `confirmReservation(items)`
- `validateCartItems(skusWithQuantities) → ValidationResult`

**Inner Classes:**
- `ValidationResult` — Builder pattern with `List<ValidItem>` and `List<InvalidItem>`, `isValid()` method
- `ValidItem` — SKU + available qty
- `InvalidItem` — SKU + reason string

**Configuration:**
- Lock prefix: "inventory:lock:"
- Lock wait time: 10 seconds
- Lock lease time: 30 seconds

---

## OutboxEventPublisher

**Purpose:** Implements transactional outbox pattern: polls database for pending events and publishes to Kafka asynchronously, with retry and dead-letter handling.

**Key Classes/Functions:**
- `publishPendingEvents()` — Scheduled task (every 30s) querying DB, publishing each event via Kafka, marking sent/failed.
- `publishEvent(OutboxEvent event)` — Sends event to Kafka with correlation ID headers, waits 10s for ack, marks as sent.
- `handlePublishFailure(OutboxEvent event, String errorMessage)` — Marks event failed, logs dead-letter alert if retry count exceeded.
- `cleanupOldEvents()` — Scheduled cron (daily 3 AM): deletes sent events older than 30 days.
- `alertPendingEvents()` — Scheduled task (every 10m): warns if backlog >1000 or failures >100.

**Data Flow:** DB poll → Event list → For each: serialize → Kafka send with headers → Mark sent → Store. On error: increment retry, mark failed. Cleanup: age-based deletion.

**Dependencies:**
- `OutboxEventRepository` — Queries pending events, updates status
- `KafkaTemplate<String, Object>` — Sends to Kafka
- `ObjectMapper` — Serialization (unused in shown methods)

**Design Patterns:** Scheduled batch processing with transactional outbox, exponential backoff via retry count.

**Public API:** None (internal scheduled jobs).

**Configuration:**
- Poll interval: 30 seconds
- Kafka send timeout: 10 seconds
- Batch size: 100 events
- Max retry: 3 (implicit via dead-letter check)
- Cleanup cron: `0 0 3 * * ?` (daily 3 AM)
- Alert schedule: every 600000ms (10 minutes)

---

## OutboxOrderHelper

**Purpose:** Serializes order lifecycle events (creation, status change, cancellation) into OutboxEvent entities for Kafka publication.

**Key Classes/Functions:**
- `saveOrderCreatedEvent(Order order, String paymentMethod, String bankCode)` — Creates ORDER_CREATED event with payment details, UUID correlation ID, serializes to JSON, saves to outbox.
- `saveOrderCreatedEvent(Order order, CreateOrderRequest request)` — Overload extracting payment details from request.
- `saveOrderStatusChangedEvent(Order order, OrderStatus prev, OrderStatus next, String note)` — Creates ORDER_STATUS_CHANGED event with before/after status, timestamp, changer username.
- `saveOrderStatusChangedEvent(Order order, OrderStatus prev, OrderStatus next, String note, String role)` — Overload with role parameter.
- `saveOrderCancelledEvent(Order order, String reason, boolean refundRequired)` — Creates ORDER_CANCELLED event with reason and refund flag.

**Data Flow:** Domain event (order created/status changed) → Build LinkedHashMap payload → Serialize to JSON → Create OutboxEvent (PENDING status) → Save to DB. Later: OutboxEventPublisher publishes to Kafka.

**Dependencies:**
- `OutboxEventRepository` — Saves OutboxEvent
- `ObjectMapper` — JSON serialization
- `SecurityUtil` — Gets current username and IP address
- `KafkaTopics` constant — Topic name (ORDER_TOPIC)

**Design Patterns:** Event builder with LinkedHashMap to preserve insertion order, correlation ID for tracing.

**Public API:**
- `saveOrderCreatedEvent(order, paymentMethod, bankCode)`
- `saveOrderCreatedEvent(order, request)`
- `saveOrderStatusChangedEvent(order, prev, next, note)`
- `saveOrderStatusChangedEvent(order, prev, next, note, role)`
- `saveOrderCancelledEvent(order, reason, refundRequired)`

**Configuration:**
- Kafka topic: `KafkaTopics.ORDER_TOPIC`
- Event status: "PENDING" (processed by OutboxEventPublisher)

---

## RedisProducerService

**Purpose:** Sends product eviction messages to Redis Stream for async processing, with deduplication via distributed lock.

**Key Classes/Functions:**
- `sendEvictMessage(String id)` — Sets lock key with 10-min TTL (preventing duplicates), sends `{id}` to stream "redis-stream", starts container if stopped.

**Data Flow:** ID → Check lock:ID exists → If new, add to stream → Start stream listener container.

**Dependencies:**
- `StringRedisTemplate` — Redis operations
- `StreamMessageListenerContainer<String, MapRecord<String, String, String>>` — Stream consumer container

**Design Patterns:** Distributed deduplication via SET-IF-ABSENT, stream producer.

**Public API:** `sendEvictMessage(String id)`

**Configuration:**
- Stream name: "redis-stream"
- Lock key prefix: "lock:"
- Lock TTL: 10 minutes

---

## RedisService

**Purpose:** Generic Redis wrapper implementing `iRedis` interface with operations for strings, hashes, lists, and sets.

**Key Classes/Functions:**
- `hasKey(String key)` — Checks key existence
- `delete(String... keys)` — Deletes multiple keys
- `setValue(String key, Object value)` / `setValueWithExpiry(...)` — String operations
- `getValue(String key)` → `Object`
- `hSet(key, field, value)` / `hGet(...)` / `hGetAll(...)` / `hDelete(...)` — Hash operations
- `lPush(...)` / `lPop(...)` / `lRange(...)` — List operations
- `sAdd(...)` / `sMembers(...)` / `sRemove(...)` — Set operations
- `expire(key, timeout, timeUnit)` / `getExpire(...)` — TTL management

**Data Flow:** Method call → `RedisTemplate` operation → Redis store.

**Dependencies:**
- `RedisTemplate<String, Object>` — Spring Redis template
- `TimeUnit` — Expiry units

**Design Patterns:** Facade over RedisTemplate.

**Public API:** All methods in `iRedis` interface (listed above).

---

## RefreshTokenService

**Purpose:** Manages JWT access/refresh token lifecycle across devices with Redis storage, supporting token rotation and multi-device sessions.

**Key Classes/Functions:**
- `handleLoginTokens(User user, UserDetails userDetails, DeviceInfo deviceInfo, String deviceId)` — On login: checks existing tokens, reuses if valid or generates new pair, stores in Redis (access: 60min, refresh: 30 days), returns `DeviceInfoResponse`.
- `refreshSessionTokens(User user, UserDetails userDetails, DeviceInfo deviceInfo, String newDeviceId, String oldDeviceId)` — Generates new token pair, revokes old device's token if device changed, stores in Redis.
- `revokeAllUserTokens(Long userId)` — Logout: deletes all session data and refresh tokens for user.

**Data Flow:** Login → Redis check `user:{id}:profile` (access token) and `user:refresh_tokens:{id}:{deviceId}` (refresh token) → Validate via `JwtService` → Generate or reuse → Update Redis with TTL.

**Dependencies:**
- `RedisService` — Hash/key operations
- `JwtService` — Token validation/generation
- `ObjectMapper` — Token data serialization
- Constants: `REFRESH_TOKEN_EXPIRATION_DAYS` (30), `ACCESS_TOKEN_EXPIRATION_MINUTES` (60)

**Design Patterns:** Token rotation with device tracking, multi-device session isolation.

**Public API:**
- `handleLoginTokens(user, userDetails, deviceInfo, deviceId) → DeviceInfoResponse`
- `refreshSessionTokens(user, userDetails, deviceInfo, newDeviceId, oldDeviceId) → DeviceInfoResponse`
- `revokeAllUserTokens(userId)`

**Redis Storage:**
- Profile key: `user:{userId}:profile` (hash with accessToken field, 60-min TTL)
- Refresh tokens key: `user:refresh_tokens:{userId}` (hash with deviceId→{token, deviceInfo}, 30-day TTL)

---

## Cross-Service Dependencies

**Inventory Coordination:** `OrderInventoryService` orchestrates `InventoryService` calls for multi-item orders with distributed locking.

**Event Publishing:** `OutboxOrderHelper` creates events; `OutboxEventPublisher` publishes them; order service calls helper.

**Cache Invalidation:** Business services call `CacheSyncService.markProductDirty()` after mutations.

**Token Management:** `RefreshTokenService` uses `JwtService` for validation and generation; `DeviceInfoService` generates device IDs for session tracking.

**Redis Layer:** `RefreshTokenService` uses `RedisService`; `InventoryService` and `OrderInventoryService` use `RedissonClient` for locking.