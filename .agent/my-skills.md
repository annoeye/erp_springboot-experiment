# ERP System - Agent Skills & Workflow

## 🎯 Project Context

**Type**: Enterprise Resource Planning (ERP) System
**Stack**: Spring Boot, JPA/Hibernate, Redis, Kafka, MinIO, PostgreSQL
**Domain**: E-commerce order management with inventory control

---

## 📚 Learning (Domain Knowledge)

### System Architecture
- **Microservices Pattern**: Event-driven architecture với Kafka message broker
- **Data Layer**: JPA/Hibernate với soft-delete pattern, Redis cho caching/session
- **Storage**: MinIO cho file uploads, Thymeleaf cho email templates
- **Security**: JWT-based authentication với role-based authorization

### Core Entities & Relationships
```
User (1) ──→ (N) Order ──→ (N) OrderItem ──→ (1) Product
                                                    ↓
Category (1) ──→ (N) Product ──→ (N) ProductVariant
                                                    
Order (1) ──→ (1) Payment
ShoppingCart (Redis) ──→ CartItem ──→ Product
```

### Critical Business Flows
1. **Authentication Flow**: Registration → Email Verification → JWT Token → Refresh Token
2. **Shopping Flow**: Browse Products → Add to Cart (Redis) → Checkout → Create Order
3. **Order Lifecycle**: PENDING → CONFIRMED → SHIPPED → DELIVERED → COMPLETED
4. **Inventory Management**: Stock tracking → Reservation → Deduction → Restoration (on cancel)

### Known Technical Debt
- ❌ No distributed locking for inventory (race condition risk)
- ❌ Direct Kafka publishing (no transactional outbox)
- ❌ OrderItem references Product entity directly (breaks on soft-delete)
- ❌ No JWT refresh token mechanism
- ❌ Missing payment validation in order state transitions

---

## 🔒 Rules (Constraints & Principles)

### Code Quality Standards
1. **Minimal Code**: Write only what's necessary to solve the problem
2. **Idiomatic Java**: Follow Spring Boot conventions and best practices
3. **Transaction Boundaries**: Always use `@Transactional` with proper isolation levels
4. **Error Handling**: Use custom exceptions with meaningful messages
5. **Logging**: Use SLF4J with structured logging (include correlation IDs)

### Architecture Constraints
1. **No Synchronous Calls Between Services**: Use Kafka events for inter-service communication
2. **Idempotency Required**: All Kafka consumers must handle duplicate messages
3. **Soft Delete Only**: Never hard-delete entities referenced by orders
4. **Snapshot Pattern**: Store denormalized data in OrderItem (product name, price, SKU)
5. **Distributed Locking**: Use Redis (Redisson) for critical sections (inventory, auth codes)

### Security Rules
1. **Authentication**: JWT access token (15min) + refresh token (7 days)
2. **Authorization**: Check roles via `@PreAuthorize` on controller methods
3. **Rate Limiting**: Apply to sensitive endpoints (login, password reset, OTP)
4. **Input Validation**: Use `@Valid` with custom validators for business rules
5. **Sensitive Data**: Never log passwords, tokens, or payment details

### Database Rules
1. **Optimistic Locking**: Use `@Version` for entities with concurrent updates (Inventory)
2. **Pessimistic Locking**: Use for critical reads (User by email during registration)
3. **Native Queries**: Prefer JPQL, use native SQL only when necessary
4. **Indexing**: Add indexes on foreign keys and frequently queried fields
5. **Migration**: Use Flyway/Liquibase for schema changes

---

## 🛠️ Skills (Capabilities)

### Skill 1: Implement Distributed Locking
**When to use**: Adding inventory management, preventing race conditions, concurrent resource access

**Steps**:
1. Add Redisson dependency to `pom.xml`
2. Create `RedisLockService` with `acquireLock(key, timeout)` and `releaseLock(key)`
3. Wrap critical sections: `try { lock.lock(); ... } finally { lock.unlock(); }`
4. Use lock keys like `inventory:lock:{sku}` or `auth:lock:{email}`
5. Set reasonable timeouts (5-10 seconds) to prevent deadlocks

**Example**:
```java
@Service
public class InventoryService {
    @Autowired private RedisLockService lockService;
    
    public void reserveStock(String sku, int quantity) {
        RLock lock = lockService.acquireLock("inventory:" + sku);
        try {
            // Check and update inventory
        } finally {
            lock.unlock();
        }
    }
}
```

### Skill 2: Implement Transactional Outbox Pattern
**When to use**: Publishing Kafka events reliably, ensuring DB + message queue consistency

**Steps**:
1. Create `OutboxEvent` entity (id, aggregate_id, event_type, payload, created_at, sent_at)
2. In service layer: Save business entity + outbox event in same transaction
3. Create `@Scheduled` job to poll unsent events → publish to Kafka → mark as sent
4. Add `correlation_id` to all events for tracing
5. Implement idempotency check in consumers (store processed event IDs)

**Example**:
```java
@Transactional
public Order createOrder(OrderDto dto) {
    Order order = orderRepository.save(new Order(dto));
    outboxRepository.save(new OutboxEvent(
        "ORDER_CREATED", order.getId(), toJson(order)
    ));
    return order;
}
```

### Skill 3: Implement Soft Delete with Cascade
**When to use**: Deleting categories/products, maintaining referential integrity, audit trails

**Steps**:
1. Add `deleted_at` and `deleted_by` fields to entity
2. Override repository methods with `@Where(clause = "deleted_at IS NULL")`
3. Add `@PreRemove` listener to cascade soft-delete to children
4. For OrderItem: Snapshot product data (name, price, SKU) instead of FK reference
5. Scheduled job: Delete only records not referenced by active orders

**Example**:
```java
@Entity
@Where(clause = "deleted_at IS NULL")
public class Category {
    @OneToMany(mappedBy = "category")
    private List<Product> products;
    
    @PreRemove
    public void cascadeSoftDelete() {
        products.forEach(p -> p.setDeletedAt(LocalDateTime.now()));
    }
}
```

### Skill 4: Implement JWT Refresh Token
**When to use**: Authentication flow, token expiration handling, session management

**Steps**:
1. Generate two tokens on login: access (15min) + refresh (7 days)
2. Store refresh token in Redis: `refresh_token:{userId}` with TTL
3. Create `/api/auth/refresh` endpoint: Validate refresh token → Issue new access token
4. On logout: Delete refresh token from Redis
5. Add `@PreAuthorize` to protected endpoints checking access token

**Example**:
```java
public AuthResponse login(LoginDto dto) {
    User user = authenticate(dto);
    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);
    redisTemplate.opsForValue().set(
        "refresh:" + user.getId(), refreshToken, 7, TimeUnit.DAYS
    );
    return new AuthResponse(accessToken, refreshToken);
}
```

### Skill 5: Implement Order State Machine
**When to use**: Order status transitions, payment validation, business rule enforcement

**Steps**:
1. Create `OrderStateMachine` service with allowed transitions map
2. Add `transition(Order order, OrderStatus targetStatus)` method
3. Validate business rules before transition (e.g., payment completed for CONFIRMED)
4. Emit Kafka event after successful transition
5. Handle cancellation: Restore inventory + trigger refund

**Example**:
```java
@Service
public class OrderStateMachine {
    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
        PENDING, Set.of(CONFIRMED, CANCELLED),
        CONFIRMED, Set.of(SHIPPED, CANCELLED),
        SHIPPED, Set.of(DELIVERED)
    );
    
    public void transition(Order order, OrderStatus target) {
        if (!TRANSITIONS.get(order.getStatus()).contains(target)) {
            throw new InvalidTransitionException();
        }
        if (target == CONFIRMED && !paymentService.isCompleted(order)) {
            throw new PaymentNotCompletedException();
        }
        order.setStatus(target);
    }
}
```

### Skill 6: Implement Rate Limiting
**When to use**: Protecting sensitive endpoints (login, OTP, password reset), preventing abuse

**Steps**:
1. Create `RateLimitService` using Redis sliding window algorithm
2. Key format: `rate_limit:{endpoint}:{identifier}` (e.g., email or IP)
3. Increment counter on each request, check against threshold
4. Set TTL to window duration (e.g., 1 hour)
5. Return 429 Too Many Requests when limit exceeded

**Example**:
```java
@Aspect
@Component
public class RateLimitAspect {
    @Around("@annotation(rateLimit)")
    public Object checkRateLimit(ProceedingJoinPoint pjp, RateLimit rateLimit) {
        String key = "rate:" + rateLimit.endpoint() + ":" + getIdentifier();
        Long count = redisTemplate.opsForValue().increment(key);
        if (count == 1) {
            redisTemplate.expire(key, rateLimit.duration(), TimeUnit.SECONDS);
        }
        if (count > rateLimit.limit()) {
            throw new RateLimitExceededException();
        }
        return pjp.proceed();
    }
}
```

## 🔄 Workflow (Task Execution Process)

### Phase 1: Analysis & Planning
1. **Read Audit Report**: Understand the problem, affected components, and business impact
2. **Identify Dependencies**: Check which entities, services, and APIs are involved
3. **Review Existing Code**: Read relevant files to understand current implementation
4. **Create Task Breakdown**: Split into subtasks with clear acceptance criteria
5. **Estimate Complexity**: Identify risks and potential blockers

### Phase 2: Implementation
1. **Start with Data Layer**: Create/modify entities, add fields, indexes
2. **Update Repository Layer**: Add custom queries, locking strategies
3. **Implement Service Logic**: Business rules, transaction boundaries, error handling
4. **Add Controller Endpoints**: REST APIs with validation and authorization
5. **Integrate External Systems**: Kafka producers/consumers, Redis operations

### Phase 3: Testing & Verification
1. **Unit Tests**: Test service methods with mocked dependencies
2. **Integration Tests**: Test with real DB, Redis, Kafka (testcontainers)
3. **Manual Testing**: Use Postman/curl to verify happy path and edge cases
4. **Performance Testing**: Check for N+1 queries, slow transactions
5. **Security Testing**: Verify authentication, authorization, input validation

### Phase 4: Documentation & Handoff
1. **Update API Docs**: Document new endpoints, request/response formats
2. **Add Code Comments**: Explain complex logic, business rules, gotchas
3. **Update README**: Add setup instructions for new dependencies
4. **Create Migration Guide**: Document breaking changes, data migrations
5. **Write Runbook**: Operational procedures, monitoring, troubleshooting

---

## 📋 Task Priority Matrix

### P0 (Critical - Do First)
- **Task 1**: Distributed Locking for Inventory (prevents overselling)
- **Task 2**: Transactional Outbox for Kafka (prevents data loss)

### P1 (High - Do Next)
- **Task 3**: Cascade Soft Delete & Snapshot OrderItems (data integrity)
- **Task 4**: JWT Refresh Token & Rate Limiting (security)

### P2 (Medium - Do After)
- **Task 5**: Order State Machine with Payment Validation (business logic)

---

## 🎯 Success Criteria

### Technical Metrics
- ✅ Zero race conditions in inventory management (load test with 100 concurrent users)
- ✅ 100% Kafka event delivery (no lost messages even on DB rollback)
- ✅ Soft-deleted products don't break order history
- ✅ JWT tokens expire correctly, refresh works without re-login
- ✅ Order state transitions follow business rules (no invalid states)

### Code Quality Metrics
- ✅ Test coverage > 80% for critical paths
- ✅ No N+1 query problems (check with Hibernate statistics)
- ✅ All endpoints have rate limiting on sensitive operations
- ✅ Proper transaction boundaries (no lazy loading exceptions)
- ✅ Structured logging with correlation IDs for tracing

---

## 🚀 Quick Start Commands

```bash
# Build project
./mvnw clean install

# Run with Docker Compose (Redis, Kafka, MinIO, PostgreSQL)
docker-compose up -d

# Run application
./mvnw spring-boot:run

# Run tests
./mvnw test

# Check code quality
./mvnw checkstyle:check

# Generate API docs
./mvnw springdoc-openapi:generate
```

---

## 📞 When to Ask for Help

1. **Unclear Requirements**: Business rules not specified in audit report
2. **Missing Context**: Need to understand existing code behavior
3. **Architecture Decisions**: Trade-offs between different approaches
4. **Breaking Changes**: Changes that affect existing APIs or data schema
5. **Performance Concerns**: Optimization strategies for high-load scenarios

---

*Last Updated: 2026-05-24*
*Version: 1.0*
