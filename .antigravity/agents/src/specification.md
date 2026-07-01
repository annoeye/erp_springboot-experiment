# Specification Module Knowledge Document

## Overview

The **specification** group provides a reusable, fluent query-building framework for JPA Criteria API–based dynamic filtering across multiple entity types (User, Product, Category, Attributes, Order). It implements the **Builder** and **Strategy** patterns to construct type-safe, composable database queries without raw SQL or string concatenation.

---

## File: AttributesSpecification.java

**Purpose**: Builds dynamic JPA Criteria queries for the `Attributes` entity. Provides a fluent builder interface to compose multiple filter specifications.

**Key Classes/Functions**:
- `AttributesSpecification` — main builder class
  - `builder()` — static factory returning new instance
  - `build()` — returns compiled `Specification<Attributes>` combining all added filters via AND logic
  - `like(String value)` — private helper; wraps value in `%` wildcards and uppercases for case-insensitive matching

**Data Flow**: 
Caller instantiates via `builder()`, adds specifications (currently none implemented in this file), calls `build()` to get a `Specification<Attributes>` for use with Spring Data JPA repositories.

**Dependencies**:
- `Attributes` entity model
- `org.springframework.data.jpa.domain.Specification` — JPA criteria abstraction
- `jakarta.persistence.criteria` — CriteriaBuilder, Predicate
- Lombok `@NoArgsConstructor`

**Design Patterns**: 
- **Builder** — fluent API for composing predicates
- **Strategy** — each filter is a separate specification; combined via AND

**Public API**:
- `static AttributesSpecification builder()`
- `Specification<Attributes> build()`

**Configuration**: None (no environment variables or constants).

---

## File: CategorySpecification.java

**Purpose**: Builds dynamic JPA Criteria queries for the `Category` entity. Implements name-based filtering with case-insensitive LIKE matching.

**Key Classes/Functions**:
- `CategorySpecification` — builder for Category filters
  - `builder()` — static factory
  - `withName(String name)` — adds name LIKE filter if not empty; returns `this` for chaining
  - `build()` — returns compiled `Specification<Category>`
  - `like(String value)` — private helper

**Data Flow**: 
Builder pattern chain: `CategorySpecification.builder().withName("xyz").build()` produces a `Specification<Category>` for repository queries.

**Dependencies**:
- `Category` entity model
- `org.springframework.data.jpa.domain.Specification`
- `jakarta.persistence.criteria`
- Lombok annotations
- `org.springframework.util.ObjectUtils` — null/empty checking

**Design Patterns**: 
- **Builder** with fluent method chaining
- **Null Object** — empty specifications are filtered out during build

**Public API**:
- `static CategorySpecification builder()`
- `CategorySpecification withName(String name)`
- `Specification<Category> build()`

**Configuration**: 
- Field constant: `FIELD_NAME = "name"` (target entity attribute)

---

## File: OrderSpecification.java

**Purpose**: Builds complex dynamic JPA Criteria queries for `Order` entity based on `OrderSearchRequest` DTO. Handles multi-field filtering (order number, customer, date range, amount range, status) with N+1 query optimization and join deduplication.

**Key Classes/Functions**:
- `OrderSpecification` — static utility class
  - `build(OrderSearchRequest request)` — static method returning `Specification<Order>` with all predicates from request fields

**Data Flow**:
1. Receives `OrderSearchRequest` with optional search fields
2. Iterates through non-null/non-empty fields
3. Builds corresponding JPA predicates (LIKE for strings, EQUAL for enums/IDs, BETWEEN/comparison for dates/amounts)
4. Optimizes joins: only fetches `orderItems` relationship if query is NOT a count query
5. Applies `DISTINCT` to prevent duplicate rows from joins
6. Returns combined predicate via AND logic

**Dependencies**:
- `Order`, `OrderSearchRequest`
- `jakarta.persistence.criteria` — JoinType, Predicate
- `org.springframework.data.jpa.domain.Specification`
- `org.springframework.util.StringUtils` — text validation

**Design Patterns**: 
- **Static Builder** — single static method builds specification from DTO
- **Query Optimization** — conditional JOIN fetching based on query type (count vs. data fetch)

**Public API**:
- `static Specification<Order> build(OrderSearchRequest request)`

**Configuration**: 
Hardcoded field names: `"orderNumber"`, `"customer"`, `"id"`, `"customerName"`, `"customerEmail"`, `"customerPhone"`, `"currentStatus"`, `"auditInfo"`, `"createdAt"`, `"totalAmount"`, `"orderItems"`

**Performance Notes**: 
- Uses `currentStatus` column instead of JSON CLOB parsing for fast status queries
- Handles potential `NumberFormatException` when parsing customer ID

---

## File: ProductSpecification.java

**Purpose**: Builds dynamic JPA Criteria queries for `Product` entity with filters on name, SKU, and status fields using case-insensitive LIKE matching.

**Key Classes/Functions**:
- `ProductSpecification` — builder for Product filters
  - `builder()` — static factory
  - `withName(String name)` — adds name LIKE filter if not empty
  - `withSku(String sku)` — adds SKU LIKE filter if not empty
  - `withStatus(String status)` — adds status LIKE filter if not empty
  - `build()` — returns compiled `Specification<Product>`
  - `like(String value)` — private helper

**Data Flow**: 
Fluent chain: `ProductSpecification.builder().withName("x").withSku("y").build()` produces a `Specification<Product>`.

**Dependencies**:
- `Product` entity model
- `org.springframework.data.jpa.domain.Specification`
- `jakarta.persistence.criteria`
- Lombok annotations
- `org.springframework.util.ObjectUtils`

**Design Patterns**: 
- **Builder** with fluent chaining
- **Null Object** filtering

**Public API**:
- `static ProductSpecification builder()`
- `ProductSpecification withName(String name)`
- `ProductSpecification withSku(String sku)`
- `ProductSpecification withStatus(String status)`
- `Specification<Product> build()`

**Configuration**: 
Field constants: `FIELD_NAME = "name"`, `FIELD_SKU = "sku"`, `FIELD_STATUS = "status"`

---

## File: SearchCriteria.java

**Purpose**: Data transfer object representing a single search filter criterion with key, operation type, and value.

**Key Classes/Functions**:
- `SearchCriteria` — POJO with Lombok annotations
  - `key` (String) — entity attribute name or path (e.g., `"customer.name"`)
  - `operation` (SearchOperation enum) — filter operation (EQUALITY, GREATER_THAN, LIKE, etc.)
  - `value` (Object) — filter value
  - Constructor overload accepting `(key, operationStr, value)` that parses operator string
  - `parseOperation(String op)` — maps operator symbols (`:`, `!`, `>`, `<`, `~`) to enum values

**Data Flow**: 
Used by `SpecificationBuilder` to define individual predicates. Created via constructor with parsed operator or direct enum.

**Dependencies**:
- `SearchOperation` enum
- Lombok `@Data`, `@NoArgsConstructor`, `@AllArgsConstructor`

**Design Patterns**: 
- **Value Object** — immutable data container
- **Parser** — `parseOperation()` converts operator symbols to enums

**Public API**:
- Constructors: `SearchCriteria()`, `SearchCriteria(key, operation, value)`, `SearchCriteria(key, operationStr, value)`
- Getters/setters via `@Data`

**Configuration**: 
Operator mappings (hardcoded in `parseOperation()`):
- `:` → EQUALITY
- `!` → NEGATION
- `>` → GREATER_THAN
- `<` → LESS_THAN
- `~` → LIKE
- default → EQUALITY

---

## File: SpecificationBuilder.java

**Purpose**: Generic, reusable JPA Criteria query builder supporting multiple search operations (equality, negation, comparison, LIKE patterns, IN clauses). Composes a list of `SearchCriteria` into a type-safe `Specification<T>`.

**Key Classes/Functions**:
- `SpecificationBuilder<T>` — generic builder parameterized on entity type
  - `params` (List<SearchCriteria>) — accumulated search criteria
  - `with(String key, SearchOperation op, Object value)` — adds criterion and returns `this`
  - `with(String key, String operation, Object value)` — stub (not implemented, returns `this`)
  - `build()` — returns `Specification<T>` combining all criteria via AND logic
  - Private predicate builders:
    - `buildEquality()` — case-insensitive string equality or direct comparison
    - `buildNegation()` — NOT EQUAL
    - `buildGreaterThan()` — Comparable comparison
    - `buildLessThan()` — Comparable comparison
    - `buildLike()` — wildcard substring match; supports List (IN) and Enum
    - `buildStartsWith()` — prefix match
    - `buildEndsWith()` — suffix match
    - `buildContains()` — substring match
    - `buildIn()` — IN clause for lists
    - `resolvePath()` — resolves dot-notation nested paths (e.g., `"customer.id"`)

**Data Flow**:
1. Caller builds criteria list or uses fluent `with()` methods
2. `build()` iterates each `SearchCriteria`
3. Resolves field path (handles nested attributes)
4. Applies operation-specific predicate builder
5. Combines all predicates via AND
6. Returns compiled `Specification<T>`

**Dependencies**:
- `SearchCriteria`
- `SearchOperation` enum
- `jakarta.persistence.criteria` — CriteriaBuilder, Path, Predicate, Root
- SLF4J `Logger`

**Design Patterns**: 
- **Generic Builder** — type-safe specification builder
- **Strategy** — operation-specific predicate builders
- **Fluent API** — chainable `with()` methods

**Public API**:
- Constructors: `SpecificationBuilder()`, `SpecificationBuilder(List<SearchCriteria> params)`
- `SpecificationBuilder<T> with(String key, SearchOperation operation, Object value)`
- `Specification<T> build()`

**Configuration**: None (hardcoded path resolution via dot notation).

**Debug Output**: Logs predicate count and param list at DEBUG level via SLF4J.

---

## File: UserSpecification.java

**Purpose**: Builds dynamic JPA Criteria queries for `User` entity with filters on fullName, email, and phoneNumber using case-insensitive LIKE matching.

**Key Classes/Functions**:
- `UserSpecification` — builder for User filters
  - `builder()` — static factory
  - `withFullName(String fullName)` — adds fullName LIKE filter if not empty
  - `withEmail(String email)` — adds email LIKE filter if not empty
  - `withPhoneNumber(String phoneNumber)` — adds phoneNumber LIKE filter if not empty
  - `build()` — returns compiled `Specification<User>`
  - `like(String value)` — private helper

**Data Flow**: 
Fluent chain: `UserSpecification.builder().withFullName("John").withEmail("x@y.com").build()` produces a `Specification<User>`.

**Dependencies**:
- `User` entity model
- `org.springframework.data.jpa.domain.Specification`
- `jakarta.persistence.criteria`
- Lombok annotations
- `org.springframework.util.ObjectUtils`

**Design Patterns**: 
- **Builder** with fluent method chaining
- **Null Object** — empty filters omitted

**Public API**:
- `static UserSpecification builder()`
- `UserSpecification withFullName(String fullName)`
- `UserSpecification withEmail(String email)`
- `UserSpecification withPhoneNumber(String phoneNumber)`
- `Specification<User> build()`

**Configuration**: 
Field constants: `FIELD_FULL_NAME = "fullName"`, `FIELD_EMAIL = "email"`, `FIELD_PHONE_NUMBER = "phoneNumber"`

---

## Cross-File Dependencies & Data Flow

```
SearchCriteria + SearchOperation enum
         ↓
    SpecificationBuilder<T> (generic)
         ↓
   Used by repository layers to build Specification<T>
         ↓
AttributesSpecification, CategorySpecification, ProductSpecification, UserSpecification, OrderSpecification
(entity-specific builders using builder pattern)
         ↓
    Spring Data JPA Repository.findAll(Specification<T>)
```

**Key Distinctions**:
- **Entity-specific builders** (Attributes, Category, Product, User) — fluent, method-per-field approach; build via predefined specifications list
- **Generic SpecificationBuilder** — flexible list-based approach supporting arbitrary operations and nested paths
- **OrderSpecification** — static utility; specialized for complex multi-entity queries with optimization

All combine predicates via AND logic and support case-insensitive string matching.