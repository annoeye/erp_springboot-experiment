# Component Group Knowledge Document

## Overview

The **component** group provides essential infrastructure and cross-cutting concerns for the ERP Spring Boot application. These components handle authentication, caching, data initialization, UI field filtering, and event-driven cache invalidation. Together, they form the backbone of request processing, security, performance optimization, and data consistency.

---

## CacheWarmupListener

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/component/CacheWarmupListener.java`

**Purpose:** Automatically pre-loads frequently accessed data into RAM cache when the application starts successfully, reducing cold-start latency and improving initial user experience.

**Key Classes/Functions:**

| Name | Purpose | Parameters | Return |
|------|---------|-----------|--------|
| `warmupCaches()` | Entry point triggered on `ApplicationReadyEvent`; orchestrates category and product warm-up | None | void |
| `warmupCategories()` | Loads all categories (max 200 records) into cache via `CategoryService.search()` | None | void |
| `warmupProducts()` | Pre-loads first 500 products into cache via `ProductService.searchProducts()` | None | void |

**Data Flow:**
- Listens for Spring's `ApplicationReadyEvent` after application startup completes
- Executes `warmupCategories()` → calls `CategoryService.search()` with page=1, size=200
- Executes `warmupProducts()` → calls `ProductService.searchProducts()` with page=1, size=500
- Both methods log success count or catch exceptions without failing startup

**Dependencies:**
- `CategoryService` — retrieves all categories for RAM cache
- `ProductService` — retrieves popular products for RAM cache
- `CategorySearchRequest`, `GetProductRequest`, `PagingRequest` — DTO request objects
- Spring Framework: `ApplicationReadyEvent`, `@EventListener`, `@Component`

**Design Patterns:**
- **Event-Driven Initialization** — uses Spring event listeners to trigger warm-up at the optimal lifecycle moment
- **Lazy vs Eager Loading Strategy** — categories and popular products loaded eagerly; attributes use lazy loading with 5-minute TTL

**Public API:**
- No public methods exposed; operates internally via Spring event system

**Configuration:**
- Hard-coded page sizes: categories=200, products=500
- Strategy documented in class comments: attributes excluded from warm-up due to data volume

---

## JwtAuthenticationFilter

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/component/JwtAuthenticationFilter.java`

**Purpose:** Intercepts HTTP requests to extract, validate, and apply JWT authentication tokens, establishing the authenticated user context for each request.

**Key Classes/Functions:**

| Name | Purpose | Parameters | Return |
|------|---------|-----------|--------|
| `doFilterInternal()` | Core filter method processing each request | `HttpServletRequest`, `HttpServletResponse`, `FilterChain` | void |
| `handleJwtException()` | Sends standardized JSON error response for JWT failures | `HttpServletResponse`, int status, `String` message | void |
| `mapJwtExceptionToMessage()` | Converts JWT exceptions to user-friendly Vietnamese error messages | `Exception` e | `String` |

**Data Flow:**
1. Extracts "Bearer {token}" from `Authorization` header
2. Calls `JwtService.extractUsername(jwt)` to parse username
3. Loads `UserDetails` via `UserDetailsService.loadUserByUsername()`
4. Validates token with `JwtService.isTokenValid(token, userDetails)`
5. On success: creates `UsernamePasswordAuthenticationToken` and sets in `SecurityContextHolder`
6. On failure: calls `handleJwtException()` to return 401/400 JSON response
7. Special handling for logout endpoint — allows expired tokens to proceed

**Dependencies:**
- `JwtService` — extracts username and validates token validity
- `UserDetailsService` — loads user authorities and credentials
- Spring Security: `SecurityContextHolder`, `UsernamePasswordAuthenticationToken`, `WebAuthenticationDetailsSource`
- Spring Framework: `HttpServletRequest/Response`, `FilterChain`
- JWT exceptions: `ExpiredJwtException`, `MalformedJwtException`, `SignatureException`, `UnsupportedJwtException`

**Design Patterns:**
- **Filter Chain Pattern** — extends `OncePerRequestFilter` to guarantee single execution per request
- **Exception Mapping** — converts low-level JWT exceptions to business-friendly messages (Vietnamese)
- **Graceful Degradation** — logout requests bypass token expiration checks for revocation workflow

**Public API:**
- Configured as a Spring Security filter bean; no direct method calls from other components

**Configuration:**
- `BEARER_PREFIX = "Bearer "` — token format
- Logout endpoint exempt path: `/api/auth/logout`

---

## OraclePrivilegeInitializer

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/component/OraclePrivilegeInitializer.java`

**Purpose:** Automatically grants necessary Oracle database privileges (CREATE TABLE, CREATE VIEW, CREATE SEQUENCE) to the application's database user during startup, enabling Hibernate's `ddl-auto:update` to function.

**Key Classes/Functions:**

| Name | Purpose | Parameters | Return |
|------|---------|-----------|--------|
| `init()` | Executed post-construction; connects as admin and grants privileges | None | void |

**Data Flow:**
1. Reads admin credentials and target user from application properties
2. If admin datasource not configured, logs info message and returns
3. Establishes connection as admin user via `DriverManager.getConnection()`
4. Executes three GRANT statements sequentially for target user
5. Logs success or catches exceptions with guidance message

**Dependencies:**
- `java.sql.Connection`, `DriverManager`, `Statement` — direct JDBC for admin-level operations
- Spring Framework: `@Value`, `@PostConstruct`, `@Component`, `@Lazy(false)`

**Design Patterns:**
- **Eager Initialization** — `@Lazy(false)` ensures this runs before Hibernate DDL operations
- **Fail-Safe Configuration** — checks for admin credentials; skips gracefully if not configured

**Public API:**
- No public methods; operates via lifecycle callback

**Configuration:**
- Environment variables (application.yml):
  - `app.datasource.admin.url` — admin connection URL (required)
  - `app.datasource.admin.username` — admin user (required)
  - `app.datasource.admin.password` — admin password
  - `spring.datasource.username` — target application user (default: "Spring_app")

---

## RedisConsumer

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/component/RedisConsumer.java`

**Purpose:** Listens to Redis Stream messages for cache invalidation events and orchestrates intelligent cache eviction with retry logic and backpressure handling when the target system is overloaded.

**Key Classes/Functions:**

| Name | Purpose | Parameters | Return |
|------|---------|-----------|--------|
| `onMessage()` | Called per stream message; initiates retry loop with lock-based synchronization | `MapRecord<String, String, String>` message | void |
| `processMessageWithRetry()` | Retries cache eviction up to 30 times (2.5 minutes) with exponential backoff | `MapRecord`, `String` lockKey | void |
| `evictCache()` | Removes cached product data from three separate caches: productDetails, products, attributes | `MapRecord<String, String, String>` msg | void |
| `checkTargetSystem()` | Placeholder method to determine if system is ready for cache operations | None | boolean |

**Data Flow:**
1. Receives message from Redis Stream containing product ID to evict
2. Generates lock key: `"lock:" + productId`
3. Calls `processMessageWithRetry()` to handle with backpressure:
   - Calls `checkTargetSystem()` — if ready, proceeds to eviction
   - If not ready: extends lock TTL to 10 minutes, sleeps 5 seconds, retries
   - After 30 failed attempts: logs error, deletes lock, abandons message
4. On success: `evictCache()` removes product from three caches:
   - `productDetails` cache — specific product entry by ID (Long)
   - `products` cache — entire products list (clear all)
   - `attributes` cache — product attributes by ID (String)
5. Deletes lock key and stream message record
6. Stops stream container on completion or system unavailability

**Dependencies:**
- `StringRedisTemplate` — Redis key/value and Stream operations
- `CacheManager` — access Spring Cache abstraction for cache eviction
- `StreamMessageListenerContainer` — manages stream consumption lifecycle (qualified as "RedisContainer")
- Spring Data Redis: Stream, `MapRecord`, `StreamListener`

**Design Patterns:**
- **Observer/Listener Pattern** — implements `StreamListener` for event-driven cache invalidation
- **Retry with Exponential Backoff** — 30 retries × 5 seconds = up to 2.5 minutes
- **Distributed Lock** — uses Redis key with TTL to prevent concurrent processing
- **Backpressure Handling** — stops container when system overloaded to prevent queue buildup

**Public API:**
- Implements `StreamListener` interface; Spring manages subscription internally

**Configuration:**
- `maxRetries = 30` — retry attempts for system readiness
- Sleep interval: 5 seconds between retries
- Lock TTL extension: 10 minutes per retry
- Stream name: `"redis-stream"`
- Cache names: `"productDetails"`, `"products"`, `"attributes"`

---

## UIFieldFilterComponent

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/component/UIFieldFilterComponent.java`

**Purpose:** Transforms internal `ProductDto` (containing all fields including sensitive data) into a public `ProductPublicRecord` (containing only customer-facing fields) for secure exposure via GraphQL and Customer APIs.

**Key Classes/Functions:**

| Name | Purpose | Parameters | Return |
|------|---------|-----------|--------|
| `toPublicRecord()` | Maps ProductDto to ProductPublicRecord, filtering sensitive fields | `ProductDto` dto | `ProductPublicRecord` |

**Data Flow:**
1. Receives fully-populated `ProductDto` from cache
2. Extracts only public fields: SKU, name, media items, discount percent, category name, status
3. Hides sensitive fields: id, totalRevenue, viewCount, cost, internal flags
4. Returns immutable `ProductPublicRecord` for API response

**Dependencies:**
- `ProductDto` — internal DTO with complete product information
- `ProductPublicRecord` — public response record (defined in `dto.response.graphql` package)

**Design Patterns:**
- **Data Transfer Object (DTO) / Projection Pattern** — separates internal data model from public API contract
- **Security Through Composition** — explicitly whitelists safe fields rather than blacklisting

**Public API:**
- `toPublicRecord(ProductDto dto)` — single public method injected into Customer API and GraphQL controllers

**Configuration:**
- No environment variables or configuration
- Field mapping is hard-coded based on `ProductPublicRecord` constructor signature

---

## UserSeeder

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/component/UserSeeder.java`

**Purpose:** Automatically creates a default ADMIN user during application startup if no users exist in the database, ensuring the application is immediately usable without manual user provisioning.

**Key Classes/Functions:**

| Name | Purpose | Parameters | Return |
|------|---------|-----------|--------|
| `run()` | Called on application startup via `CommandLineRunner`; seeds ADMIN user if table empty | `String[]` args | void |

**Data Flow:**
1. Checks if `userRepository.count() > 0` — if users exist, returns immediately
2. If no users: creates new `User` entity with:
   - Full name: "Ngô Ngọc Định"
   - Username: "ADMIN"
   - Email: "ADMIN@gmail.com"
   - Password: hashed via `PasswordEncoder.encode("admin")`
   - Status: `ACTIVE`
   - Roles: `{ADMIN, USER, SUPER_ADMIN}` (set-based)
3. Persists via `userRepository.save()`
4. Logs completion

**Dependencies:**
- `User` — entity model for database persistence
- `UserRepository` — Spring Data JPA repository
- `PasswordEncoder` — Spring Security password encoding
- `ActiveStatus`, `RoleType` — enums for user state and roles
- Spring Framework: `CommandLineRunner`, `@Component`, `@Profile` (ready for profile-specific activation)

**Design Patterns:**
- **Initialization Pattern** — implements `CommandLineRunner` for post-startup data population
- **Idempotent Design** — safe to run multiple times; early exit if data already exists

**Public API:**
- No public methods; operates via Spring lifecycle

**Configuration:**
- Hard-coded credentials:
  - Username: `ADMIN`
  - Default password: `admin`
  - Email: `ADMIN@gmail.com`
  - Full name: `Ngô Ngọc Định`
- Roles assigned: `ADMIN`, `USER`, `SUPER_ADMIN`
- Status: `ACTIVE`

---

## Cross-Component Data Flow

```
Request → JwtAuthenticationFilter → [Token validation + SecurityContext]
         ↓
      Controller (GraphQL/REST)
         ↓
      UIFieldFilterComponent (if public API)
         ↓
      ProductDto from Cache (warmed by CacheWarmupListener)
         ↓
      ProductPublicRecord → Response
         ↓
      Cache Invalidation Event → RedisConsumer → Evict (productDetails, products, attributes)
```

---

## Initialization Sequence

1. **OraclePrivilegeInitializer.init()** — `@PostConstruct`, `@Lazy(false)` — runs first to grant DB privileges
2. **Hibernate DDL** — `ddl-auto:update` uses granted privileges to create/update schema
3. **UserSeeder.run()** — `CommandLineRunner` — creates ADMIN user if empty
4. **CacheWarmupListener.warmupCaches()** — `ApplicationReadyEvent` — pre-loads categories and products
5. **JwtAuthenticationFilter** — registered in security chain, active for all requests