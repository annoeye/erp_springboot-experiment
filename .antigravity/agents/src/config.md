# Config Module Knowledge Document

## Overview

The **config** module contains 14 Spring Boot configuration classes that establish core infrastructure for the ERP application. These configurations manage security (JWT, authentication), caching (Caffeine in-memory), message streaming (Kafka), object storage (MinIO), distributed locking (Redisson), API documentation (OpenAPI/Swagger), and web framework setup. The module acts as the single point of truth for application-wide behavior initialization.

---

## ApplicationConfig.java

**Purpose**: Provides core Spring Security beans for authentication and password encoding.

**Key Components**:
- `passwordEncoder()` → Returns `BCryptPasswordEncoder` bean for secure password hashing
- `authenticationManager(AuthenticationConfiguration)` → Exposes Spring's `AuthenticationManager` bean from configuration

**Dependencies**: Spring Security (`spring-security-crypto`, `spring-security-config`)

**Public API**: Exposes `PasswordEncoder` and `AuthenticationManager` for injection across application

---

## CacheConfig.java

**Purpose**: Configures Caffeine in-memory caching with TTL and size limits to optimize read-heavy product/category queries.

**Key Components**:
- `CACHE_PRODUCTS` (10 min TTL, 500 max) → Search results cache
- `CACHE_PRODUCT_DETAILS` (10 min TTL, 1000 max) → Product detail cache
- `CACHE_CATEGORY_DETAILS` (30 min TTL, 200 max) → Category detail cache
- `CACHE_ATTRIBUTES` (5 min TTL, 2000 max) → Product attributes cache
- `cacheManager()` → Returns `CaffeineCacheManager` bean with pre-registered caches

**Design Pattern**: Cache stampede prevention via targeted eviction (no `allEntries` clears) and bulk loading with selective ID fetches

**Dependencies**: Caffeine (`com.github.benmanes.caffeine`), Spring Cache (`spring-context`)

**Public API**: Cache constants for `@Cacheable`/`@CacheEvict` annotations; `CacheManager` bean

---

## DataTypeConfig.java

**Purpose**: Configures Jackson serialization for Hibernate lazy-loaded entities in JSON responses.

**Key Components**:
- `hibernate5Module()` → Returns `Hibernate5JakartaModule` with lazy loading disabled (`FORCE_LAZY_LOADING = false`)

**Dependencies**: Jackson Hibernate module (`jackson-datatype-hibernate5-jakarta`)

**Public API**: Jackson module bean for REST response serialization

---

## KafkaAdminConfig.java

**Purpose**: Provides Kafka AdminClient for topic management and cluster operations.

**Key Components**:
- `bootstrapServers` → Injected from `spring.kafka.bootstrap-servers` (default: `localhost:9093`)
- `adminClient()` → Returns configured `AdminClient` with 5-second request timeout

**Dependencies**: Apache Kafka Admin client

**Configuration**: 
- `spring.kafka.bootstrap-servers` environment variable (default: `localhost:9093`)
- `REQUEST_TIMEOUT_MS_CONFIG`: 5000ms

**Public API**: `AdminClient` bean for Kafka cluster operations

---

## KafkaTopicConfig.java

**Purpose**: Auto-creates Kafka topics at application startup.

**Key Components**:
- `activeLogTopic()` → Creates `ACTIVE_LOG_TOPIC` (2 partitions, replication factor 1)
- `orderTopic()` → Creates `ORDER_TOPIC` (2 partitions, replication factor 1)
- `paymentResultTopic()` → Creates `payment-result` topic (3 partitions, replication factor 1)

**Dependencies**: `KafkaTopics` constants from `common.constants`

**Public API**: Three `NewTopic` beans for Spring Kafka auto-configuration

---

## MinioConfiguration.java

**Purpose**: Initializes MinIO S3-compatible object storage client.

**Key Components**:
- `minioClient()` → Returns `MinioClient` configured with endpoint, access key, and secret key from `MinioProperties`

**Dependencies**: `MinioProperties` (injected), MinIO SDK (`io.minio`)

**Public API**: `MinioClient` bean for file upload/download operations

---

## MinioProperties.java

**Purpose**: Binds MinIO configuration properties from `application.yml`/`application.properties`.

**Key Fields**:
- `accessKey` (String) → MinIO access key
- `secretKey` (String) → MinIO secret key
- `url` (String) → MinIO endpoint URL
- `bucket` (String) → Default bucket name

**Configuration Prefix**: `integration.minio`

**Design Pattern**: Spring `@ConfigurationProperties` for type-safe config binding

**Public API**: Property class injected into `MinioConfiguration`

---

## MultipartResolverConfig.java

**Purpose**: Configures file upload handling for multipart form requests.

**Key Components**:
- `multipartResolver()` → Returns `StandardServletMultipartResolver` bean named `customMultipartResolver`

**Dependencies**: Spring Web (`spring-webmvc`)

**Public API**: `MultipartResolver` bean for `@RequestParam MultipartFile` injection

---

## OpenApiConfig.java

**Purpose**: Generates OpenAPI 3.0 specification for Swagger UI documentation with JWT bearer auth.

**Key Components**:
- `openAPI()` → Builds `OpenAPI` object with:
  - Bearer JWT security scheme
  - API info (title: "ERP Experiment API", version 1.0.0)
  - Contact info from properties
  - Server URL from `server.url` property

**Configuration Properties**:
- `spring.contact.name` → Contact name
- `spring.contact.url` → Contact URL
- `spring.contact.email` → Contact email
- `server.url` → Development server URL

**Dependencies**: SpringDoc OpenAPI (`springdoc-openapi-starter-webmvc-ui`)

**Public API**: `OpenAPI` bean auto-picked by SpringDoc for `/swagger-ui.html` and `/v3/api-docs`

---

## RedisConfiguration.java

**Purpose**: Configures Spring `RedisTemplate` with JSON serialization for cache and session storage.

**Key Components**:
- `redisTemplate(RedisConnectionFactory)` → Returns `RedisTemplate<String, Object>` with:
  - String key/hash-key serialization
  - JSON value/hash-value serialization via `GenericJackson2JsonRedisSerializer`

**Design Pattern**: Template pattern for Redis operations

**Dependencies**: Spring Data Redis (`spring-boot-starter-data-redis`)

**Public API**: `RedisTemplate` bean for Redis operations (cache writes, session storage)

---

## RedissonConfig.java

**Purpose**: Manually configures `RedissonClient` for distributed locking without conflicting with Spring Data Redis.

**Key Components**:
- `redissonClient()` → Builds single-node `RedissonClient` from Redis properties with:
  - Connection pool: min 2, max 4
  - Retry: 3 attempts, 1500ms interval
  - Timeout: 5 seconds (both connect and operation)
  - Optional password/username for Redis 6+ ACL

**Configuration Properties**:
- `spring.data.redis.host` (default: `localhost`)
- `spring.data.redis.port` (default: `6379`)
- `spring.data.redis.password` (optional)
- `spring.data.redis.username` (optional, Redis 6+ ACL)

**Design Pattern**: Manual bean construction to avoid Spring Data Redis conflicts

**Dependencies**: Redisson (`org.redisson`)

**Public API**: `RedissonClient` bean for `InventoryService`, `OrderInventoryService` distributed locks

---

## SecurityConfiguration.java

**Purpose**: Defines Spring Security filter chains for API and MCP endpoints with JWT token validation and CORS.

**Key Components**:
- `mcpFilterChain(HttpSecurity)` → Stateless chain for `/mcp/**` (all requests permitted)
- `apiFilterChain(HttpSecurity)` → Stateless chain for `/api/**` with:
  - Swagger endpoints permitted
  - Auth endpoints (`/auth/register`, `/auth/login`, etc.) permitted
  - Product/image/payment/delivery endpoints permitted
  - `/api/auth/search` requires `ADMIN` role
  - `/api/address/**`, `/api/orders/**` require authentication
  - `/api/auth/get-user/**` requires `USER` role
  - JWT filter injected before `UsernamePasswordAuthenticationFilter`
- `corsConfigurationSource()` → CORS policy for `localhost:3000`, `localhost:3245` with all HTTP methods

**Dependencies**: `JwtAuthenticationFilter` (injected from `component` package), Spring Security

**Whitelist Constants**:
- `SWAGGER_WHITELIST`: `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`
- `REQUEST_PERMIT_ALL`: Auth, product, image, payment, delivery endpoints
- `MCP_PERMIT_ALL`: `/mcp`, `/mcp/**`

**Design Pattern**: Multiple filter chains for different endpoint groups

**Public API**: Two `SecurityFilterChain` beans and `CorsConfigurationSource` bean

---

## WebClientConfig.java

**Purpose**: Empty placeholder for future HTTP client configuration (currently unused).

**Status**: No implementation

---

## WebMvcConfiguration.java

**Purpose**: Registers request interceptor for API endpoints excluding auth.

**Key Components**:
- `addInterceptors(InterceptorRegistry)` → Registers `SmartRequestInterceptor` for `/api/**` excluding `/api/auth/**`

**Dependencies**: `SmartRequestInterceptor` (injected from `common.interceptor`)

**Design Pattern**: Spring MVC interceptor pattern for cross-cutting request handling

**Public API**: Interceptor registration via `WebMvcConfigurer` interface

---

## Data Flow & Dependencies

**Initialization Order**:
1. `ApplicationConfig` → Security beans (password encoder, auth manager)
2. `DataTypeConfig` → Jackson serialization
3. `RedisConfiguration` + `RedissonConfig` → Redis connectivity
4. `CacheConfig` → Caffeine cache manager
5. `KafkaAdminConfig` + `KafkaTopicConfig` → Kafka topics
6. `MinioConfiguration` + `MinioProperties` → Object storage
7. `SecurityConfiguration` → Filter chains + CORS
8. `WebMvcConfiguration` → Request interceptors
9. `OpenApiConfig` → Swagger documentation

**Internal Dependencies**:
- `SecurityConfiguration` → `JwtAuthenticationFilter` (component)
- `WebMvcConfiguration` → `SmartRequestInterceptor` (common.interceptor)
- `MinioConfiguration` → `MinioProperties`
- `KafkaTopicConfig` → `KafkaTopics` (common.constants)

---

## Configuration Summary

| Component | Source | Default | Purpose |
|-----------|--------|---------|---------|
| Redis Host | `spring.data.redis.host` | `localhost` | Redisson connection |
| Redis Port | `spring.data.redis.port` | `6379` | Redisson connection |
| Kafka Bootstrap | `spring.kafka.bootstrap-servers` | `localhost:9093` | Kafka cluster |
| MinIO URL | `integration.minio.url` | N/A (required) | Object storage endpoint |
| MinIO Access Key | `integration.minio.accessKey` | N/A (required) | S3 credentials |
| MinIO Secret Key | `integration.minio.secretKey` | N/A (required) | S3 credentials |
| Contact Name | `spring.contact.name` | N/A | Swagger metadata |
| Server URL | `server.url` | N/A | Swagger server |
| CORS Origins | Hardcoded | `localhost:3000`, `localhost:3245` | Frontend domains |