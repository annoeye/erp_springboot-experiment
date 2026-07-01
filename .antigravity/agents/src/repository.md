# Repository Module Knowledge Document

## Overview

The **repository** group provides Spring Data JPA repository interfaces for the ERP system's core business entities. These repositories handle all database persistence operations across users, products, orders, inventory, payments, and supporting domains. The group implements standard CRUD operations alongside custom query methods for business logic, analytics, and soft-delete patterns. Key architectural patterns include pessimistic locking for concurrent access, transactional outbox for event consistency, and JpaSpecificationExecutor for dynamic queries.

---

## AddressRepository

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/repository/AddressRepository.java`

**Purpose:** Manages Address entity persistence. Provides standard CRUD and address lookup by user.

**Key Methods:**
- `findByUserId(Long userId)` → `List<Address>` — Retrieves all addresses for a specific user

**Dependencies:** 
- Imports `Address` entity from `model.entity`
- Extends `JpaRepository<Address, Long>` for standard CRUD

**Design Pattern:** Basic Spring Data repository with derived query method

**Public API:** Read-only address retrieval by user

---

## AttributesRepository

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/repository/AttributesRepository.java`

**Purpose:** Manages Attributes (product variants) persistence with soft-delete support, filtering, and inventory tracking. Core repository for variant-level operations like stock, pricing, and sales metrics.

**Key Methods:**
- `findAttributesBySku_sku(String skuSku)` → `Optional<Attributes>` — Find variant by SKU (may be deleted)
- `findAttributesBySkuNotDeleted(@Param("sku") String sku)` → `Optional<Attributes>` — Find active variant by SKU
- `findAttributesById(Long id)` → `Optional<Attributes>` — Lookup by ID
- `findAllBySku_skuIn(List<String> skus)` → `List<Attributes>` — Batch lookup by multiple SKUs
- `getQuantityAttributesById(@Param("ids") List<Long> ids)` → `List<Attributes>` — Fetch multiple by IDs
- `findAllByProduct(Product product)` → `List<Attributes>` — Get all variants for a product
- `findAllByProductNotDeleted(@Param("product") Product product)` → `List<Attributes>` — Active variants only
- `findAllByProduct_Id(Long productId)` → `List<Attributes>` — Variants by product ID
- `findAllByProductIdNotDeleted(@Param("productId") Long productId)` → `List<Attributes>` — Active variants by product ID
- `existsBySku_sku(String skuSku)` → `boolean` — Check variant existence
- `countByProduct(Product product)` → `long` — Count all variants
- `countByProductNotDeleted(@Param("product") Product product)` → `long` — Count active variants
- `findByNameContainingNotDeleted(@Param("name") String name)` → `List<Attributes>` — Search by name (case-insensitive, active only)
- `findByPriceBetweenNotDeleted(@Param("minPrice") Double, @Param("maxPrice") Double)` → `List<Attributes>` — Price range filter
- `findAllOnSale()` → `List<Attributes>` — Find variants with active sales
- `deleteAllExpiredAttributes()` — Native SQL hard delete for soft-deleted records past retention
- `updateSalePrice(@Param("sku") String, @Param("salePrice") Double)` — Update sale price by SKU
- `updateSoldQuantity(@Param("id") Long, @Param("quantity") Integer)` — Increment sold quantity
- `updateTotalOrders(@Param("id") Long)` — Increment order count
- `findIdsAndSkusBySkus(@Param("skus") List<String> skus)` → `List<Object[]>` — Batch ID/SKU mapping
- `findIdBySkuNotDeleted(@Param("sku") String sku)` → `Optional<Long>` — Get ID by active SKU

**Dependencies:**
- Imports `Attributes`, `Product` entities
- Extends `JpaRepository<Attributes, Long>`, `JpaSpecificationExecutor<Attributes>`
- Uses `@Transactional`, `@Modifying`, `@Query` annotations

**Design Patterns:** 
- Soft-delete filtering (checks `deletedAt IS NULL`)
- Batch operations for performance
- Native SQL for cleanup jobs
- Specification executor for dynamic filtering

**Public API:** Variant lookup, filtering, pricing updates, sales tracking, batch operations

---

## CategoryRepository

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/repository/CategoryRepository.java`

**Purpose:** Manages Category entity persistence with soft-delete support and bulk operations.

**Key Methods:**
- `findCategoryById(Long id)` → `Optional<Category>` — Lookup by ID
- `findCategoryBySkuInfo_Sku(String skuInfoSku)` → `Optional<Category>` — Lookup by SKU
- `deleteAllExpiredCategories()` — Hard delete soft-deleted records past retention
- `existsAllByName(String name)` → `boolean` — Check name existence
- `softDeleteAllByIds(@Param("ids") List<Long>, @Param("deletedBy") String, @Param("deletedAt") LocalDateTime)` — Bulk soft delete with audit
- `softDeleteAllByIds(List<Long> ids, String deletedBy)` — Convenience overload setting deletion time to +30 days
- `findCategoryByName(String name)` → `Optional<Category>` — Lookup by name
- `findIdsAndSkusBySkus(@Param("skus") List<String> skus)` → `List<Object[]>` — Batch ID/SKU mapping
- `findIdBySku(@Param("sku") String sku)` → `Optional<Long>` — Get ID by SKU

**Dependencies:**
- Imports `Category` entity
- Extends `JpaRepository<Category, Long>`, `JpaSpecificationExecutor<Category>`
- Uses `@Modifying`, `@Query`, `@Transactional` annotations

**Design Patterns:**
- Soft-delete with audit (tracks `deletedBy`, `deletedAt`)
- Default transactional method with 30-day retention
- Native SQL cleanup for expired records

**Public API:** Category lookup, naming checks, bulk soft-delete, SKU mapping

---

## OrderItemRepository

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/repository/OrderItemRepository.java`

**Purpose:** Manages OrderItem persistence and provides extensive analytics queries for order and product performance metrics.

**Key Methods:**
- `findByOrder(Order order)` → `List<OrderItem>` — Get items in an order
- `findByOrderId(@Param("orderId") Long orderId)` → `List<OrderItem>` — Get items by order ID
- `findByProduct(Product product)` → `List<OrderItem>` — Get all orders containing a product
- `findByProductId(@Param("productId") Long productId)` → `List<OrderItem>` — Get items by product ID
- `findBestSellingProducts()` → `List<Object[]>` — Products ranked by COMPLETED order quantity
- `sumQuantitySoldByProductId(@Param("productId") Long productId)` → `Long` — Total quantity sold (completed only)
- `sumRevenueByProductId(@Param("productId") Long productId)` → `Double` — Total revenue by product
- `countOrdersByProductId(@Param("productId") Long productId)` → `Integer` — Completed orders count
- `sumRevenueByProductIdAndPeriod(@Param("productId"), @Param("startDate"), @Param("endDate"))` → `Double` — Revenue in date range
- `countCancelledOrdersByProductId(@Param("productId") Long productId)` → `Integer` — Cancelled order count
- `countReturnedOrdersByProductId(@Param("productId") Long productId)` → `Integer` — Returned order count
- `sumQuantitySoldByAttributesId(@Param("attributesId") Long attributesId)` → `Integer` — Variant-level sales quantity

**Dependencies:**
- Imports `Order`, `OrderItem`, `Product` entities
- Extends `JpaRepository<OrderItem, Long>`
- Uses `@Query` with complex aggregations

**Design Patterns:**
- Aggregation queries filtering by order status
- Product performance analytics
- Period-based revenue reporting

**Public API:** Order item retrieval, product analytics, sales metrics, variant performance tracking

---

## OrderRepository

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/repository/OrderRepository.java`

**Purpose:** Manages Order persistence with eager loading, pagination, filtering by status/customer/date, and business analytics.

**Key Methods:**
- `findByOrderNumber(String orderNumber)` → `Optional<Order>` — Lookup by order number (eager loads `orderItems`)
- `existsByOrderNumber(String orderNumber)` → `boolean` — Check order number uniqueness
- `findByCustomer(User customer, Pageable pageable)` → `Page<Order>` — Customer's orders paginated
- `findByCustomerId(@Param("customerId") Long customerId, Pageable pageable)` → `Page<Order>` — Paginated by customer ID with eager loading
- `findByStatus(@Param("status") String status, Pageable pageable)` → `Page<Order>` — Filter by status with pagination
- `findByCustomerAndStatus(@Param("customer") User, @Param("status") String, Pageable pageable)` → `Page<Order>` — Combined filter
- `findByCreatedAtBetween(@Param("startDate"), @Param("endDate"), Pageable pageable)` → `Page<Order>` — Date range filter
- `findByCustomerIdAndCreatedAtBetween(@Param("customerId"), @Param("startDate"), @Param("endDate"), Pageable pageable)` → `Page<Order>` — Customer + date range
- `countByStatus(@Param("status") String status)` → `long` — Order count by status
- `countByCustomer(User customer)` → `long` — Orders per customer
- `sumTotalAmountByStatus(@Param("status") String status)` → `Double` — Revenue by status
- `sumTotalAmountByDateRange(@Param("startDate"), @Param("endDate"))` → `Double` — Revenue in period
- `findPendingOrders()` → `List<Order>` — PENDING/CONFIRMED orders (eager loaded, sorted by creation)
- `findInProgressOrders()` → `List<Order>` — PROCESSING/PACKED/SHIPPED orders (eager loaded, sorted)
- `findTopCustomersByTotalAmount(Pageable pageable)` → `Page<Object[]>` — Customers ranked by total COMPLETED order value
- `getOrderStatisticsByDate(@Param("startDate"), @Param("endDate"))` → `List<Object[]>` — Daily order count and revenue

**Dependencies:**
- Imports `Order`, `User`, `OrderStatus` entities/enums
- Extends `JpaRepository<Order, Long>`, `JpaSpecificationExecutor<Order>`
- Uses `@EntityGraph`, `@Query` with JOIN FETCH for N+1 prevention
- Supports `Page<T>` and `Pageable` for pagination

**Design Patterns:**
- `@EntityGraph(attributePaths = "orderItems")` prevents N+1 by eager loading related items
- Complex aggregation queries for analytics
- Status pattern matching with LIKE wildcards
- Paginated result sets for scalability

**Public API:** Order lookup, filtering (customer/status/date), pagination, business analytics, revenue tracking

---

## OutboxEventRepository

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/repository/OutboxEventRepository.java`

**Purpose:** Implements Transactional Outbox pattern for reliable event publishing. Manages pending, failed, and dead events with retry logic.

**Key Methods:**
- `findPendingEvents()` → `List<OutboxEvent>` — PENDING status events ordered by creation (FIFO)
- `findEventsNeedingRetry(@Param("now") LocalDateTime now)` → `List<OutboxEvent>` — FAILED events past retry time
- `findEventsReadyToSend(@Param("now") LocalDateTime now)` → `List<OutboxEvent>` — PENDING + retry-eligible FAILED events
- `findDeadEvents()` → `List<OutboxEvent>` — DEAD events for manual intervention
- `countPendingEvents()` → `long` — Count pending
- `countFailedEvents()` → `long` — Count failed
- `findByAggregateTypeAndAggregateId(String aggregateType, Long aggregateId)` → `List<OutboxEvent>` — Events for specific aggregate (e.g., order)
- `deleteOldSentEvents(@Param("cutoffDate") LocalDateTime cutoffDate)` → `int` — Cleanup sent events older than cutoff (returns deleted count)
- `existsByAggregateTypeAndAggregateIdAndEventType(String aggregateType, Long aggregateId, String eventType)` → `boolean` — Idempotency check

**Dependencies:**
- Imports `OutboxEvent` entity
- Extends `JpaRepository<OutboxEvent, Long>`
- Uses `@Query`, `@Modifying` annotations

**Design Patterns:**
- Transactional Outbox pattern for event reliability
- Retry logic with exponential backoff (`nextRetryAt` field)
- Idempotency checks to prevent duplicate event publishing
- FIFO ordering by `createdAt`
- Dead-letter queue for failed events

**Public API:** Event polling, retry management, aggregate-specific queries, cleanup/archival

**Configuration:** Uses event status enum (PENDING, FAILED, SENT, DEAD) and tracks `nextRetryAt` for retry scheduling

---

## PaymentRepository

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/repository/PaymentRepository.java`

**Purpose:** Manages Payment entity persistence with UUID primary key.

**Key Methods:**
- Standard CRUD only (inherited from `JpaRepository<Payment, UUID>`)

**Dependencies:**
- Imports `Payment` entity
- Extends `JpaRepository<Payment, UUID>`

**Design Pattern:** Minimal repository with standard CRUD operations

**Public API:** Payment CRUD operations

---

## ProductInventoryRepository

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/repository/ProductInventoryRepository.java`

**Purpose:** Manages ProductInventory with pessimistic locking for concurrent stock updates.

**Key Methods:**
- `findBySkuWithLock(@Param("sku") String sku)` → `Optional<ProductInventory>` — Find inventory with PESSIMISTIC_WRITE lock for safe updates
- `findBySku(String sku)` → `Optional<ProductInventory>` — Find inventory without lock

**Dependencies:**
- Imports `ProductInventory` entity
- Extends `JpaRepository<ProductInventory, Long>`
- Uses `@Lock(LockModeType.PESSIMISTIC_WRITE)` for concurrent access control

**Design Patterns:**
- Pessimistic locking prevents race conditions during stock deductions
- Dual queries: locked vs. unlocked for flexibility

**Public API:** Inventory lookup with optional locking

---

## ProductRepository

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/repository/ProductRepository.java`

**Purpose:** Manages Product persistence with soft-delete, analytics metrics (views, sales, revenue), and eager loading.

**Key Methods:**
- `deleteAllExpiredProducts()` — Hard delete soft-deleted records past retention
- `softDeleteAllByIds(@Param("ids") List<Long>, @Param("deletedBy") String, @Param("deletedAt") LocalDateTime)` — Bulk soft delete with audit
- `softDeleteAllByIds(List<Long> ids, String deletedBy)` — Convenience overload with +30 day retention
- `updateViewCount(@Param("id") Long id)` — Increment product view counter
- `updateTotalSoldQuantity(@Param("id") Long id, @Param("quantity") Integer quantity)` — Add to total sold
- `updateTotalOrders(@Param("id") Long id)` — Increment order count
- `updateTotalRevenue(@Param("id") Long id, @Param("price") BigDecimal price)` — Add to revenue total
- `findProductByName(String name)` → `Optional<Product>` — Lookup by name
- `findProductBySkuInfo_Sku(String skuInfoSku)` → `Optional<Product>` — Lookup by SKU
- `findIdsAndSkusBySkus(@Param("skus") List<String> skus)` → `List<Object[]>` — Batch ID/SKU mapping
- `findIdByName(@Param("name") String name)` → `Optional<Long>` — Get ID by name
- `findIdBySku(@Param("sku") String sku)` → `Optional<Long>` — Get ID by SKU
- `findByIdWithDetails(@Param("id") Long id)` → `Optional<Product>` — Eager load product with category (N+1 prevention)

**Dependencies:**
- Imports `Product` entity, `BigDecimal`
- Extends `JpaRepository<Product, Long>`, `JpaSpecificationExecutor<Product>`
- Uses `@Modifying`, `@Transactional`, `@Query` annotations

**Design Patterns:**
- Soft-delete with audit trail
- Denormalized analytics fields (viewCount, totalSoldQuantity, totalOrders, totalRevenue) for reporting
- Batch operations for bulk updates
- Eager loading with LEFT JOIN FETCH to prevent N+1 queries

**Public API:** Product lookup, metrics tracking, analytics denormalization, bulk soft-delete

---

## ShoppingCartRepository

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/repository/ShoppingCartRepository.java`

**Purpose:** Manages ShoppingCart persistence with expiration policies based on user rank.

**Key Methods:**
- `findByUser(User user)` → `Optional<ShoppingCart>` — Get cart for a user
- `findExpiredCartsByRank(@Param("rank") UserRank rank, @Param("cutoff") LocalDateTime cutoff)` → `List<ShoppingCart>` — Find carts inactive before cutoff for a specific rank
- `deleteExpiredCartsByRank(@Param("rank") UserRank rank, @Param("cutoff") LocalDateTime cutoff)` → `int` — Delete expired carts (returns count)

**Dependencies:**
- Imports `ShoppingCart`, `User`, `UserRank` entities/enums
- Extends `JpaRepository<ShoppingCart, Long>`
- Uses `@Query`, `@Modifying` annotations

**Design Patterns:**
- Rank-based retention policies
- Scheduled job cleanup (find + delete for purging stale carts)

**Public API:** Cart lookup by user, rank-based expiration queries

**Configuration:** Uses `UserRank` enum and `lastActivityAt` timestamp for cleanup scheduling

---

## UserRepository

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/repository/UserRepository.java`

**Purpose:** Manages User entity persistence with pessimistic locking for concurrent authentication/email updates.

**Key Methods:**
- `findByAuthCode(@Param("code") String code)` → `Optional<User>` — Lookup by auth code
- `findByEmail(String email)` → `Optional<User>` — Lookup by email with PESSIMISTIC_WRITE lock for safe concurrent updates
- `findByName(@Param("name") String name)` → `Optional<User>` — Lookup by name
- `findByNameOrEmail(@Param("value") String value)` → `Optional<User>` — Flexible lookup by name or email
- `findByNameAndEmail(@Param("name") String name, @Param("email") String email)` → `Optional<User>` — Dual-field lookup
- `count()` → `long` — Total user count

**Dependencies:**
- Imports `User` entity
- Extends `JpaRepository<User, Long>`, `JpaSpecificationExecutor<User>`
- Uses `@Lock(LockModeType.PESSIMISTIC_WRITE)`, `@Query` annotations

**Design Patterns:**
- Pessimistic locking on `findByEmail()` prevents race conditions during authentication
- Multiple lookup strategies (auth code, email, name, combination)
- Specification executor for dynamic filtering

**Public API:** User lookup by various identifiers, authentication support

---

## Data Flow & Integration

**Cross-Repository Dependencies:**

- **OrderRepository ↔ OrderItemRepository:** Orders eagerly load items; OrderItems reference Products
- **OrderItemRepository ↔ AttributesRepository:** Items track sold quantities and updates on variant metrics
- **ProductRepository ↔ AttributesRepository:** Products have multiple variants; soft-deletes cascade
- **ShoppingCartRepository ↔ UserRepository:** Carts belong to users; rank-based expiration
- **OrderRepository ↔ PaymentRepository:** Orders reference payments (via Order entity)
- **OutboxEventRepository:** Standalone; publishes events from domain operations (Order creation, etc.)
- **ProductInventoryRepository:** Locked lookups during order processing to prevent overselling
- **AddressRepository ↔ UserRepository:** Addresses belong to users

**Analytics Flow:**
- Product views → `ProductRepository.updateViewCount()`
- Order completion → `OrderItemRepository` aggregates, `ProductRepository` denormalizes (sold qty, revenue, orders)
- Attributes metrics → `AttributesRepository.updateSoldQuantity()`, `updateTotalOrders()`

**Event Flow:**
- Business operations create `OutboxEvent` records
- `OutboxEventRepository.findEventsReadyToSend()` polls periodically
- Events transition: PENDING → SENT → cleanup, or PENDING → FAILED → retry → SENT/DEAD

---

## Key Design Patterns Summary

| Pattern | Repositories | Purpose |
|---------|--------------|---------|
| **Soft Delete** | Product, Category, Attributes | Compliance/audit trail with `deletedAt`, `deletedBy` |
| **Pessimistic Locking** | User (email), ProductInventory (sku) | Prevent concurrent update race conditions |
| **Transactional Outbox** | OutboxEventRepository | Reliable event publishing with retry logic |
| **N+1 Prevention** | Order (`@EntityGraph`), Product (`LEFT JOIN FETCH`) | Eager load related entities in single query |
| **Batch Operations** | Attributes, Product, Category | Bulk updates for performance |
| **Denormalized Analytics** | Product (viewCount, totalSoldQuantity, totalRevenue) | Fast reporting without aggregations |
| **Specification Executor** | Product, Category, Attributes, Order, User | Dynamic filtering via `JpaSpecificationExecutor` |

---

## Public API Surface

**Entity Lookups:** By ID, natural keys (name, SKU, email, order number), relationships (user→address, product→attributes)

**Filtering:** By status, date range, price range, user rank, product performance metrics

**Analytics:** Revenue/quantity aggregations, best-sellers, customer rankings, daily statistics

**Mutations:** Soft deletes, metric increments (views, sales, orders), price updates, inventory locks

**Event Publishing:** Outbox polling and status transitions for reliable async operations

**Cleanup:** Expired soft-deletes, old outbox events, stale shopping carts