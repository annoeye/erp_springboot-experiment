# Mapper Group Knowledge Document

## Overview

The **mapper** group provides data transfer object (DTO) mapping infrastructure for the ERP Spring Boot application. It uses MapStruct to enable compile-time bean mapping between JPA entities and service DTOs, reducing boilerplate and improving type safety. The group consists of 12 interfaces: one configuration template, one generic base interface, and 10 entity-specific mappers covering core business domains (products, orders, payments, users, etc.).

---

## Base Infrastructure

### EntityMapper.java
**Purpose:** Generic base interface defining the contract for all entity-to-DTO bidirectional mappings.

**Key Methods:**
- `E toEntity(D dto)` — Converts DTO to entity with null-value checking (NullValueCheckStrategy.ALWAYS)
- `D toDto(E entity)` — Converts entity to DTO, ignoring unmapped properties
- `List<D> toDto(List<E> entityList)` — Batch DTO conversion
- `List<E> toEntity(List<D> dtoList)` — Batch entity conversion

**Configuration:**
- Uses `@BeanMapping` annotations to control null handling and unmapped property policies
- All implementations extend this interface to inherit consistent mapping behavior

---

### DefaultConfigMapper.java
**Purpose:** Centralized MapStruct configuration template applied across mappers via `@MapperConfig`.

**Settings:**
- `componentModel = SPRING` — Enables Spring dependency injection for generated mappers
- `unmappedTargetPolicy = IGNORE` — Silently ignores properties without explicit mappings

**Usage:** Referenced by `UserMapper` via `config = DefaultConfigMapper.class`; other mappers apply equivalent settings inline.

---

## Entity-Specific Mappers

### AttributesMapper.java
**Purpose:** Maps between `Attributes` entity and `AttributesDto`.

**Key Methods:**
- `Attributes partialUpdate(AttributesDto xAttributesDto, @MappingTarget Attributes attributes)` — Updates entity fields, skipping null values in DTO

**Configuration:**
- `builder = @Builder(disableBuilder = true)` — Disables MapStruct builder generation
- `nullValuePropertyMappingStrategy = IGNORE` — Preserves existing entity values when DTO properties are null

---

### CategoryMapper.java
**Purpose:** Maps between `Category` entity and `CategoryDto`.

**Key Methods:**
- `Category partialUpdate(CategoryDto categoryDto, @MappingTarget Category category)` — Partial update with null-value skipping

**Design:** Mirrors `AttributesMapper` structure; used as dependency by `ProductMapper`.

---

### ProductMapper.java
**Purpose:** Maps between `Product` entity and `ProductDto`; also supports `UpdateProductRequest` for API updates.

**Key Methods:**
- `Product partialUpdate(ProductDto productDto, @MappingTarget Product product)` — DTO-based partial update
- `void updateFromRequest(UpdateProductRequest request, @MappingTarget Product product)` — Request-based update with selective field mapping

**Dependencies:** `uses = {CategoryMapper.class}` — Composes category mappings

**Mapping Rules:**
- Ignores `id`, `skuInfo`, and `category` in request-based updates (prevents unauthorized modification)
- Null values in requests are skipped

---

### OrderMapper.java
**Purpose:** Maps between `Order` entity and `OrderDto`; handles complex status extraction from order history.

**Key Methods:**
- `@Named("toDto") OrderDto toDto(Order order)` — Custom DTO conversion extracting the latest order status
- `List<OrderDto> toDto(List<Order> entityList)` — Batch conversion using the named mapping
- `Order partialUpdate(OrderDto orderDto, @MappingTarget Order order)` — Partial update with null-skipping

**Dependencies:** `uses = {OrderItemMapper.class}` — Composes order-item mappings

**Mapping Logic:**
- `currentStatus` — Extracts last status from `order.getStatus()` list (line 24)
- `currentStatusDescription` — Extracts description from latest status (line 25)
- Custom expressions handle null-safe navigation for empty status lists

---

### OrderItemMapper.java
**Purpose:** Maps between `OrderItem` entity and `OrderItemDto`.

**Key Methods:**
- `OrderItem partialUpdate(OrderItemDto orderItemDto, @MappingTarget OrderItem orderItem)` — Partial update

**Configuration:** Standard null-skipping partial update; used by `OrderMapper` for nested mappings.

---

### PaymentMapper.java
**Purpose:** Maps between `Payment` entity and `PaymentDto`.

**Dependencies:** `uses = {OrderMapper.class}` — Includes order mappings for nested payment-order relationships

**Key Methods:** Inherits bidirectional conversion from `EntityMapper`; no custom overrides.

---

### ShoppingCartMapper.java
**Purpose:** Maps between `ShoppingCart` entity and `ShoppingCartDto`.

**Key Methods:**
- `ShoppingCart partialUpdate(ShoppingCartDto shoppingCartDto, @MappingTarget ShoppingCart shoppingCart)` — Partial update with null-value skipping

**Configuration:** Standard configuration; inherits base methods from `EntityMapper`.

---

### SpecificationMapper.java
**Purpose:** Maps between `Specificationa` embedded entity and `SpecificationDto`.

**Key Methods:**
- `Specificationa partialUpdate(SpecificationDto specificationDto, @MappingTarget Specificationa specificationa)` — Partial update

**Note:** Maps embedded value object (not a root entity); follows same null-skipping convention.

---

### PromotionMapper.java
**Purpose:** Maps between `Promotion` embedded entity and `PromotionDto`.

**Configuration:** Uses inline `componentModel = "spring"` instead of constants; minimal overrides.

**Key Methods:** Inherits from `EntityMapper` without custom additions.

---

### UserMapper.java
**Purpose:** Maps between `User` entity and `UserDto`.

**Configuration:** `config = DefaultConfigMapper.class` — Applies centralized configuration template

**Key Methods:** Inherits base bidirectional conversion from `EntityMapper`; no custom overrides.

---

## Data Flow & Dependencies

**Composition Hierarchy:**
```
OrderMapper → OrderItemMapper
PaymentMapper → OrderMapper → OrderItemMapper
ProductMapper → CategoryMapper
```

**Typical Flow:**
1. Service receives DTO from controller
2. Mapper converts DTO → Entity via `toEntity()` or `partialUpdate()`
3. Repository persists entity
4. On retrieval, mapper converts Entity → DTO via `toDto()` for response

**Null-Value Handling Strategy:**
- `toEntity()`: Rejects null DTO properties (NullValueCheckStrategy.ALWAYS)
- `toDto()`: Ignores unmapped properties on entity
- `partialUpdate()`: Skips null values in DTO, preserving existing entity state

---

## Public API

Each mapper exposes:
- Entity-to-DTO conversion: `toDto(entity)`, `toDto(List<Entity>)`
- DTO-to-entity conversion: `toEntity(dto)`, `toEntity(List<DTO>)`
- Partial updates: `partialUpdate(dto, @MappingTarget entity)` (where implemented)
- Custom conversions: `updateFromRequest()` (ProductMapper only)

All mappers are Spring-managed beans (via `componentModel = SPRING`) and injectable into services and controllers.

---

## Design Patterns

- **Mapper Pattern**: Separates domain model from API contract via DTOs
- **Strategy Pattern**: `NullValuePropertyMappingStrategy.IGNORE` provides pluggable null-handling behavior
- **Composition Pattern**: Higher-level mappers (`OrderMapper`, `PaymentMapper`) compose lower-level mappers via `uses`
- **Named Mapping**: `@Named("toDto")` enables qualified method selection in `@IterableMapping`
- **Template Method**: `DefaultConfigMapper` provides shared configuration baseline

---

## Configuration Summary

**Global Settings (all mappers):**
- `componentModel = SPRING` — Spring bean generation
- `unmappedTargetPolicy = IGNORE` — Silent unmapped property handling
- `builder = @Builder(disableBuilder = true)` — Disable builder pattern

**Update Operations:**
- `nullValuePropertyMappingStrategy = IGNORE` — Skip null DTO fields during partial updates

**Special Cases:**
- `ProductMapper.updateFromRequest()` explicitly ignores `id`, `skuInfo`, `category` to prevent unauthorized modification
- `OrderMapper.toDto()` uses Java expressions to extract latest status from history list