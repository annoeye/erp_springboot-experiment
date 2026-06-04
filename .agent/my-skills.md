# ERP System - Agent Skills & Workflow

## Project Context

**Type**: Enterprise Resource Planning (ERP) System
**Stack**: Spring Boot, JPA/Hibernate, Redis, Kafka, MinIO, PostgreSQL
**Domain**: E-commerce order management with inventory control

---

## Domain Knowledge

### System Architecture
- **Pattern**: Event-driven architecture with Kafka message broker
- **Data Layer**: JPA/Hibernate with soft-delete pattern, Redis for caching/session
- **Storage**: MinIO for file uploads, Thymeleaf for email templates
- **Security**: JWT-based authentication with role-based authorization via `@PreAuthorize`

### Core Entities & Relationships
- `User (1) → (N) Order → (N) OrderItem → (1) Product + (1) Attributes (variant)`
- `Category (1) → (N) Product → (N) Attributes (variants with SKU, price, stock)`
- `Order (1) → (1) Payment`
- `ShoppingCart (1) → (1) User`, items stored as embedded `List<ProductQuantity>` (SKU + quantity)
- `ProductInventory (1) → (1) Product`, tracked by SKU

### Critical Business Flows
1. **Auth Flow**: Registration → Email Verification → JWT Token
2. **Shopping Flow**: Browse → Search → Cart (add/remove/update items) → Checkout → Order
3. **Order Lifecycle**: PENDING → WAITING_PAYMENT → CONFIRMED → PROCESSING → SHIPPING/READY_FOR_PICKUP → DELIVERED → COMPLETED (with CANCELLED, RETURNING, RETURNED, REFUNDED, FAILED, DELAYED as alternative paths)
4. **Inventory**: Stock tracking via `ProductInventory` → Reservation → Deduction → Restoration on cancel

### Known Technical Debt
- No distributed locking for inventory (race condition risk) — Redisson dependency exists but not fully utilized
- Direct Kafka publishing (no transactional outbox) — OutboxEvent entity exists but not connected to all services
- OrderItem references Product entity via FK (breaks on soft-delete) — has both FK and denormalized snapshot fields (productName, productSku)
- No dedicated JWT refresh endpoint — refresh tokens stored in Redis via SaveDeviceInfoListener only
- Missing payment validation in order state transitions

---

## Rules (Constraints & Principles)

### Code Quality
1. Write minimal code — only what's necessary to solve the problem
2. Follow idiomatic Spring Boot conventions and best practices
3. Use `@Transactional` with proper isolation levels
4. Use custom exceptions with meaningful messages
5. Use SLF4J with structured logging (include correlation IDs)

### Architecture
1. No synchronous calls between services — use Kafka events
2. All Kafka consumers must handle duplicate messages (idempotency)
3. Soft delete only — never hard-delete entities referenced by orders
4. Snapshot pattern — store denormalized data in OrderItem (product name, price, SKU) alongside FK reference
5. Use Redis (Redisson) for distributed locking on critical sections (inventory)

### Security
1. JWT authentication with configurable expiration via `JwtService.generateToken(expirationTimeMillis)`
2. Check roles via `@PreAuthorize` on controller methods (hasRole('ADMIN'), hasAnyRole('CUSTOMER', 'ADMIN'))
3. Use `@Valid` with custom validators for business rules
4. Never log passwords, tokens, or payment details

### Database
1. Use `@Version` for optimistic locking on entities with concurrent updates (`ProductInventory`)
2. Use pessimistic locking for critical reads (user by email during registration)
3. Prefer JPQL, use native SQL only when necessary
4. Add indexes on foreign keys and frequently queried fields
5. No migration tool in use — schema managed manually or via JPA auto-DDL

---

## Skills (Capabilities)

### Skill 1: Implement Distributed Locking
**When to use**: Adding inventory management, preventing race conditions, concurrent resource access.

**Steps**:
1. Use existing Redisson dependency (already in project)
2. Inject `RedissonClient` and get lock via `redissonClient.getLock("prefix:" + key)`
3. Wrap critical sections: `try { lock.lock(timeout, unit); ... } finally { lock.unlock(); }`
4. Use lock keys like `inventory:lock:{sku}`
5. Set reasonable timeouts (5-10 seconds) to prevent deadlocks

**Reference files**: `OrderInventoryService.java`, `InventoryService.java`

### Skill 2: Implement Transactional Outbox Pattern
**When to use**: Publishing Kafka events reliably, ensuring DB + message queue consistency.

**Steps**:
1. Use existing `OutboxEvent` entity (id, aggregate_id, event_type, payload, status, created_at, sent_at, next_retry_at)
2. In service layer: save business entity + outbox event in same `@Transactional` method
3. `OutboxEventPublisher` runs `@Scheduled(fixedDelay = 5000)` to poll PENDING events → publish to Kafka → mark as SENT
4. Add `correlation_id` to all events for tracing
5. Implement idempotency check in consumers (store processed event IDs)

**Reference files**: `OutboxEvent.java`, `OutboxEventRepository.java`, `OutboxEventPublisher.java`

### Skill 3: Implement Soft Delete with Cascade
**When to use**: Deleting categories/products, maintaining referential integrity, audit trails.

**Steps**:
1. `auditInfo` embedded field contains `deletedAt` (LocalDateTime) and `deletedBy` (String)
2. In repository: add query methods with `AuditInfo_DeletedAtIsNull` condition or JPQL `WHERE e.auditInfo.deletedAt IS NULL`
3. Use `softDeleteAllByIds(List<Long> ids, String deletedBy)` batch JPQL update pattern (already in CategoryRepository)
4. For OrderItem: snapshot product data (productName, productSku, attributesSku, unitPrice) — already implemented
5. Do NOT use `@Where` or `@PreRemove` — project explicitly uses query-level filtering

**Reference files**: `CategoryRepository.java`, `AttributesRepository.java`, `OrderItem.java`

### Skill 4: Implement JWT Refresh Token
**When to use**: Authentication flow, token expiration handling, session management.

**Steps**:
1. `JwtService.generateToken(userDetails, expirationTimeMillis)` already supports custom expiration
2. Store refresh tokens in Redis — use pattern `user:refresh_tokens:{userId}` with device tracking (see `SaveDeviceInfoListener`)
3. Create `/api/auth/refresh` endpoint — validate refresh token, issue new access token (currently only handles via device info listener)
4. On logout or device removal: delete refresh token entries from Redis
5. Add correlation between access and refresh tokens via device ID

**Reference files**: `JwtService.java`, `SaveDeviceInfoListener.java`, `UserService.java`

### Skill 5: Implement Order Status Handler
**When to use**: Order status transitions, payment validation, business rule enforcement.

**Steps**:
1. Use existing `OrderStatusHandler` class — it has the allowed transitions map
2. Call `transitionTo(order, targetStatus, note)` — validates transition is allowed
3. Call `isValidTransition(current, target)` to check before transitioning
4. Call `getCurrentStatus(order)` — reads the last status from `order.getStatus()` list
5. Call `isTerminal(status)` — checks if status is COMPLETED, CANCELLED, FAILED, or REFUNDED

**Allowed transitions (full map)**:
- PENDING → CONFIRMED, WAITING_PAYMENT, CANCELLED
- WAITING_PAYMENT → CONFIRMED, CANCELLED, FAILED
- CONFIRMED → PROCESSING, CANCELLED
- PROCESSING → SHIPPING, READY_FOR_PICKUP, CANCELLED
- SHIPPING → DELIVERED, DELAYED, RETURNING
- DELAYED → SHIPPING, RETURNING
- READY_FOR_PICKUP → DELIVERED, RETURNING
- DELIVERED → COMPLETED, RETURNING
- COMPLETED → (terminal)
- FAILED → (terminal)
- CANCELLED → (terminal)
- RETURNING → RETURNED
- RETURNED → REFUNDED
- REFUNDED → (terminal)

**Reference files**: `OrderStatusHandler.java`, `OrderService.java`

---

## Workflow (Task Execution Process)

### Phase 1: Analysis & Planning
1. Read audit report — understand problem, affected components, business impact
2. Identify dependencies — entities, services, APIs involved
3. Review existing code — read relevant files to understand current implementation
4. Create task breakdown — split into subtasks with clear acceptance criteria
5. Estimate complexity — identify risks and potential blockers

### Phase 2: Implementation
1. Start with data layer — create/modify entities, add fields, indexes
2. Update repository layer — add custom queries, locking strategies
3. Implement service logic — business rules, transaction boundaries, error handling
4. Add controller endpoints — REST APIs with validation and authorization
5. Integrate external systems — Kafka producers/consumers, Redis operations

### Phase 3: Testing & Verification
1. Unit tests — test service methods with mocked dependencies
2. Integration tests — test with real DB, Redis, Kafka (testcontainers)
3. Manual testing — use Postman/curl to verify happy path and edge cases
4. Performance testing — check for N+1 queries, slow transactions
5. Security testing — verify authentication, authorization, input validation

### Phase 4: Documentation & Handoff
1. Update API docs — document new endpoints, request/response formats
2. Add code comments — explain complex logic, business rules, gotchas
3. Update README — add setup instructions for new dependencies
4. Create migration guide — document breaking changes, data migrations
5. Write runbook — operational procedures, monitoring, troubleshooting

---

## Task Priority Matrix

### P0 (Critical — Do First)
- Distributed Locking for Inventory (prevents overselling)
- Transactional Outbox for Kafka (prevents data loss)

### P1 (High — Do Next)
- Cascade Soft Delete & Snapshot OrderItems (data integrity)
- JWT Refresh Token endpoint (security)

### P2 (Medium — Do After)
- Order Status Handler with Payment Validation (business logic)

---

## Quick Start Commands

```bash
./mvnw clean install
docker-compose up -d
./mvnw spring-boot:run
./mvnw test
./mvnw checkstyle:check
```

---

## When to Ask for Help

1. Unclear requirements — business rules not specified in audit report
2. Missing context — need to understand existing code behavior
3. Architecture decisions — trade-offs between different approaches
4. Breaking changes — changes that affect existing APIs or data schema
5. Performance concerns — optimization strategies for high-load scenarios
