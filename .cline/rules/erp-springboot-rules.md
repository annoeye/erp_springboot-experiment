# Cline Project Rules — ERP SpringBoot Experiment
## Version: 1.0

---

## 🏛️ ARCHITECTURAL RULES

### Package Structure
```
com.anno.ERP_SpringBoot_Experiment/
├── model/entity/           # JPA Entities
├── model/embedded/         # JPA Embeddable
├── model/enums/            # Enums
├── model/base/             # BaseEntity, IdentityOnly
├── repository/             # JPA Repositories
├── service/interfaces/     # iOrder, iProduct...
├── service/dto/request/    # Request DTOs
├── service/dto/response/   # Response DTOs
├── web/rest/impl/          # Controller implementations
├── web/rest/error/         # BusinessException, ErrorCode
├── event/producer/         # Kafka producers
├── event/consumer/         # Kafka consumers
├── mapper/                 # MapStruct mappers
├── config/                 # Configuration classes
└── common/constants/       # KafkaTopics, constants
```

### Entity Rules
- All entities extend `BaseEntity<Long>` (has `isDeleted` flag)
- Use `@Embedded AuditInfo auditInfo` for auditing fields
- Soft delete: set `isDeleted = true`, never physical delete

### Response Pattern
- Always wrap responses in `Response<T>`: `Response.ok(data)` or `Response.ok(pagingResponse)`
- Use `BusinessException(ErrorCode, message)` for errors
- Add `@Transactional` for all write operations

### Kafka Events
- Use `KafkaTopics` constants for topic names
- Include `correlationId` (UUID) for tracing

---

## 🎯 CODE GENERATION

### Entity Template
```java
@Entity
@Table(name = "table_name")
@Getter @Setter @SuperBuilder
@AllArgsConstructor @NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Xxx extends BaseEntity<Long> {
    @Column(name = "field_name")
    String fieldName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fk_field")
    @ToString.Exclude
    OtherEntity otherEntity;
}
```

### Service Template
```java
@Slf4j
@Service
@RequiredArgsConstructor
public class XxxService implements iXxx {
    private final XxxRepository xxxRepository;

    @Override
    @Transactional
    public Response<XxxDto> createXxx(CreateXxxRequest request) {
        // Validate → Build → Save → Return
    }
}
```

### Controller Template
```java
@Slf4j
@RestController
@RequiredArgsConstructor
public class XxxControllerImpl implements iXxx {
    private final iXxx xxxService;

    @Override
    @PostMapping("/api/xxx")
    public Response<XxxDto> createXxx(@Valid @RequestBody CreateXxxRequest request) {
        return xxxService.createXxx(request);
    }
}
```

### Repository Template
```java
public interface XxxRepository extends JpaRepository<Xxx, Long> {
    Optional<Xxx> findByFieldName(String fieldName);
    List<Xxx> findByIsDeletedFalse();
}
```

---

## 🔄 WORKFLOW

### Implement Task
1. **Analyze**: Read requirements, identify affected modules, list files
2. **Plan**: Present file list + changes → wait for user approval
3. **Execute**: Create/modify files in order: Entity → Repository → DTO → Mapper → Service → Controller
4. **Verify**: Run `mvn compile`, check imports
5. **Report**: Summary of created/modified files

### Fix Bug
1. Read error log / stack trace
2. Locate layer (Controller/Service/Repository)
3. Find source file
4. Fix & verify with compile
5. Report root cause + fix

---

## ⚠️ CRITICAL PATTERNS

### Outbox Pattern (already implemented)
```java
// Save order + outbox event in 1 transaction
OutboxEvent outbox = OutboxEvent.builder()
    .aggregateId(order.getId())
    .eventType("ORDER_CREATED")
    .payload(objectMapper.writeValueAsString(event))
    .build();
outboxRepository.save(outbox);
orderRepository.save(order);
```

### Order State Machine
```java
Map<OrderStatus, Set<OrderStatus>> VALID_TRANSITIONS = Map.of(
    OrderStatus.PENDING, Set.of(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
    OrderStatus.CONFIRMED, Set.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
    ...
);
```

---

## 📁 FILE REFERENCE
| Pattern | Location |
|---------|----------|
| Entity | `model/entity/Xxx.java` |
| Repository | `repository/XxxRepository.java` |
| Service interface | `service/interfaces/iXxx.java` |
| Service impl | `service/XxxManagement/XxxService.java` |
| Controller | `web/rest/XxxController.java` |
| Controller Impl | `web/rest/impl/xxxControllerImpl.java` |
| Mapper | `mapper/XxxMapper.java` |
