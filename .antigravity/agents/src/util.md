# Util Module Knowledge Document

## Overview

The **util** group provides cross-cutting utility functions for caching, HTTP request handling, and security context access. These components support cache invalidation strategies, IP extraction from proxied requests, and authenticated user information retrieval.

---

## CacheEvictAfterCommit.java

**Purpose:** Manages cache invalidation after successful transaction commits to prevent stale data from race conditions.

**Key Classes/Functions:**

- **`allEntries(String cacheName)`** — Clears all entries in a named cache after transaction commit. Parameters: cache name (e.g., "attributes", "categoryDetails"). Returns: void. If no active transaction, clears immediately.

- **`key(String cacheName, Object key)`** — Evicts a single key from cache after transaction commit. Parameters: cache name, key object. Returns: void. If no active transaction, evicts immediately.

- **`clearCacheNow(String cacheName)`** — Internal helper that performs synchronous cache.clear(). Logs debug message on completion.

- **`evictKeyNow(String cacheName, Object key)`** — Internal helper that performs synchronous cache.evict(key). Logs debug message on completion.

**Data Flow:**

Public methods register a `TransactionSynchronization` callback with `TransactionSynchronizationManager` that executes `afterCommit()` only if the transaction committed successfully (status == STATUS_COMMITTED). If no transaction is active, operations execute immediately. Private helpers delegate to the `CacheManager` to retrieve and manipulate cache instances.

**Dependencies:**

- `org.springframework.cache.Cache` — Cache interface for eviction/clear operations
- `org.springframework.cache.CacheManager` — Injected dependency to access named caches
- `org.springframework.transaction.support.TransactionSynchronization` — Callback interface for post-commit hooks
- `org.springframework.transaction.support.TransactionSynchronizationManager` — Registry for transaction lifecycle callbacks
- Lombok (`@Slf4j`, `@Component`, `@RequiredArgsConstructor`)

**Design Patterns:**

- **Transaction Synchronization Pattern** — Defers cache invalidation to post-commit phase, preventing race conditions where another thread reads uncommitted data and caches it.
- **Null-safe access** — Checks if cache exists before attempting operations.

**Public API:**

- `allEntries(String cacheName)` — Exported for clearing entire cache regions after transactions
- `key(String cacheName, Object key)` — Exported for targeted cache eviction

**Configuration:**

None. Depends on active `CacheManager` bean and transaction infrastructure already configured in the application.

---

## CacheUtils.java

**Purpose:** Provides bulk cache retrieval with automatic cache-miss handling and database loading.

**Key Classes/Functions:**

- **`getAll(CacheManager cacheManager, String cacheName, Collection<K> keys, Function<Collection<K>, Map<K, V>> dbLoader)`** — Retrieves multiple values from Caffeine cache, loading missing keys from database. Parameters: cache manager, cache name, collection of keys, database loader function. Returns: `Map<K, V>` with all requested keys. Falls back to direct DB call if cache is not Caffeine-backed.

**Data Flow:**

Retrieves the named cache from `CacheManager`. If the underlying native cache is a Caffeine `Cache`, calls `nativeCache.getAll(keys, missingKeys -> ...)` which atomically retrieves cached values and invokes the loader function only for missing keys. The loader function receives missing keys, queries the database, and returns a map that Caffeine automatically inserts into cache. If cache is null or not Caffeine-backed, falls back to calling `dbLoader` with all keys.

**Dependencies:**

- `org.springframework.cache.Cache` — Cache interface
- `org.springframework.cache.CacheManager` — Injected to access caches
- `com.github.benmanes.caffeine.cache.Cache` — Native cache implementation for bulk operations
- `java.util.function.Function` — Functional interface for database loader

**Design Patterns:**

- **Batch Loading Pattern** — Leverages Caffeine's `getAll()` method with compute function to atomically load missing keys in bulk.
- **Fallback Pattern** — Gracefully degrades to direct database call if cache implementation doesn't support bulk operations.

**Public API:**

- `getAll(...)` — Exported static method for batched cache retrieval with automatic miss handling

**Configuration:**

None. Assumes Caffeine cache manager is available; gracefully handles non-Caffeine implementations.

---

## HttpUtils.java

**Purpose:** Extracts client IP address from HTTP requests, handling proxy headers and multiple forwarding scenarios.

**Key Classes/Functions:**

- **`getIpAddress(HttpServletRequest request)`** — Extracts real client IP from request, checking multiple proxy headers in order. Parameters: `HttpServletRequest`. Returns: IP address string. Checks headers: `X-Forwarded-For`, `Proxy-Client-IP`, `WL-Proxy-Client-IP`, `HTTP_CLIENT_IP`, `HTTP_X_FORWARDED_FOR`, then falls back to `request.getRemoteAddr()`.

**Data Flow:**

Iterates through proxy headers in precedence order, returning the first non-null, non-empty, non-"unknown" value. If the header contains multiple IPs (comma-separated), extracts the first one (the original client). Falls back to `getRemoteAddr()` if no proxy headers are present.

**Dependencies:**

- `jakarta.servlet.http.HttpServletRequest` — Request object from servlet context
- Spring stereotype (`@Component`)

**Design Patterns:**

- **Fallback Chain Pattern** — Tries multiple headers in precedence order before using direct remote address.
- **Stateless utility** — No instance state; can be injected as singleton.

**Public API:**

- `getIpAddress(HttpServletRequest request)` — Exported for extracting client IP from servlet requests

**Configuration:**

None. Operates on standard HTTP request object.

---

## SecurityUtil.java

**Purpose:** Provides authentication context queries to retrieve current user information, verify roles, and get IP address.

**Key Classes/Functions:**

- **`getCurrentUsername()`** — Returns username from SecurityContext. Returns: username string or "anonymous". Returns "anonymous" if unauthenticated or not an instance of UserDetails.

- **`getCurrentUserDetails()`** — Returns UserDetails object from SecurityContext. Returns: UserDetails or null if unauthenticated.

- **`getCurrentUserId()`** — Returns user ID from CustomUserDetails principal. Returns: ID string or null. Requires principal to be CustomUserDetails instance.

- **`getCurrentUser()`** — Returns User entity by looking up current username in database. Returns: `Optional<User>`. Queries `UserRepository.findByName(username)`.

- **`hasRole(String role)`** — Checks if current user has a specific role. Parameters: role name (without "ROLE_" prefix). Returns: boolean. Iterates granted authorities and checks for "ROLE_" + role match.

- **`getIpAddress()`** — Extracts client IP from current request context. Returns: IP string or "0.0.0.0" if no request context. Uses same header precedence chain as HttpUtils, with IPv6 localhost ("0:0:0:0:0:0:0:1") normalized to "127.0.0.1".

**Data Flow:**

All methods retrieve `Authentication` from `SecurityContextHolder.getContext()`. Username and role checks operate on the principal object. `getCurrentUser()` uses the username to load the User entity from database. IP extraction retrieves the current HTTP request from `RequestContextHolder` and checks proxy headers.

**Dependencies:**

- `org.springframework.security.core.Authentication` — Authentication interface from SecurityContext
- `org.springframework.security.core.context.SecurityContextHolder` — Global holder for authentication
- `org.springframework.security.core.GrantedAuthority` — Role/authority interface
- `org.springframework.security.authentication.AnonymousAuthenticationToken` — Marker for unauthenticated requests
- `com.anno.ERP_SpringBoot_Experiment.model.entity.User` — User domain entity
- `com.anno.ERP_SpringBoot_Experiment.repository.UserRepository` — Injected for user lookup
- `com.anno.ERP_SpringBoot_Experiment.service.UserDetails.CustomUserDetails` — Custom principal implementation
- `jakarta.servlet.http.HttpServletRequest` — For IP extraction
- `org.springframework.web.context.request.RequestContextHolder` — Thread-local request context
- Lombok (`@RequiredArgsConstructor`, `@Component`)

**Design Patterns:**

- **Security Context Facade Pattern** — Provides simplified access to Spring Security context without exposing low-level APIs.
- **Null-safe delegation** — Checks authentication state before accessing principals.
- **Type checking pattern** — Uses `instanceof` to safely cast principals to expected types.

**Public API:**

- `getCurrentUsername()` — Returns current authenticated username
- `getCurrentUserDetails()` — Returns Spring UserDetails object
- `getCurrentUserId()` — Returns user ID (application-specific identifier)
- `getCurrentUser()` — Returns full User entity with Optional wrapper
- `hasRole(String role)` — Checks role membership
- `getIpAddress()` — Returns client IP from current request

**Configuration:**

None. Operates on standard Spring Security configuration; requires SecurityContextHolder and UserRepository to be available.