# Enums Group Knowledge Document

## Overview

The `enums` group comprises 13 enumeration classes that define domain-specific constants and statuses throughout an ERP Spring Boot e-commerce application. These enums standardize values for user authentication, order management, payments, inventory, role-based access, and system operations, ensuring type safety and consistency across the codebase.

---

## ActiveStatus.java

**Purpose:** Tracks user account lifecycle states and system/business actions (authentication, account management, user operations, product/order management, violations).

**Key Constants:**
- **Account States:** `LOCKED`, `ACTIVE`, `INACTIVE`, `PASSWORD_RESET`, `EMAIL_VERIFICATION`, `TWO_FACTOR_AUTH`, `INVITATION`, `ACCOUNT_UNLOCK`, `LOGIN_VERIFICATION`
- **Authentication Actions:** `LOGIN`, `LOGOUT`, `REGISTER`, `RESET_PASSWORD`, `VERIFY_EMAIL`, `CHANGE_PASSWORD`
- **User Management:** `BLOCK_USER`, `UNBLOCK_USER`, `DELETE_USER`, `UPDATE_USER_PROFILE`, `ASSIGN_ROLE`, `GET_ALL`
- **System Operations:** `STOP_WORK`, `RESUME_WORK`, `SYSTEM_MAINTENANCE`, `SYSTEM_UPDATE`, `CREATE_USER` (TODO: not implemented)
- **Product Operations:** `CREATE_PRODUCT`, `UPDATE_PRODUCT`, `DELETE_PRODUCT`, `IMPORT_PRODUCTS`, `EXPORT_PRODUCTS`, `DELETE_CATEGORY`
- **Order Operations:** `CREATE_ORDER`, `UPDATE_ORDER`, `CANCEL_ORDER`, `CONFIRM_ORDER`, `GENERATE_INVOICE`
- **Compliance:** `HANDLE_VIOLATION`, `REPORT_USER`, `WARN_USER`, `AUTO_LOG_ENTRY`, `MANUAL_LOG_ENTRY`, `OTHER_ACTION`

**Structure:** Lombok `@Getter` and `@RequiredArgsConstructor` provide immutable access to `description` field (Vietnamese descriptions).

**Public API:** Each constant is accessible as `ActiveStatus.CONSTANT_NAME.getDescription()`.

---

## CachingStatus.java

**Purpose:** Defines lifecycle states for cached recommendation/suggestion data in Redis.

**Key Constants:**
- `PENDING` — Awaiting system generation
- `PROCESSING` — Currently generating suggestions
- `GENERATED` — Successfully created and stored in Redis
- `CACHED` — Retrieved from Redis cache
- `EXPIRED` — Redis TTL exceeded
- `INVALIDATED` — Removed due to user behavior changes
- `FAILED` — Generation failure

**Structure:** Enum with `message` field (Vietnamese descriptions), Lombok annotations for getter.

**Public API:** Access via `CachingStatus.CONSTANT_NAME.getMessage()`.

**Use Case:** Auditing cache hits, misses, and expiration for recommendation systems.

---

## ErrorCodes.java

**Purpose:** Standardizes HTTP and application-level error classifications.

**Key Constants:**
- `BAD_REQUEST` — Invalid input
- `UNAUTHORIZED` — Authentication required
- `FORBIDDEN` — Authorization denied
- `NOT_FOUND` — Resource missing
- `INTERNAL_SERVER_ERROR` — Server fault
- `SERVICE_ERROR` — Business logic/service failure

**Structure:** Immutable enum with `description` field using Lombok annotations.

**Public API:** `ErrorCodes.CONSTANT_NAME.getDescription()` for error mapping.

---

## Gender.java

**Purpose:** Simple enumeration for user demographic data.

**Key Constants:** `MALE`, `FEMALE`, `OTHER`

**Structure:** Minimal enum without additional fields or Lombok decorators.

**Public API:** Direct constant access; typically used in user profile entities.

---

## OrderStatus.java

**Purpose:** Defines complete order lifecycle from creation through delivery/return/refund.

**Key Constants (15 total):**
- Initiation: `PENDING`, `WAITING_PAYMENT`
- Processing: `CONFIRMED`, `PROCESSING`, `SHIPPING`, `READY_FOR_PICKUP`
- Resolution: `DELAYED`, `DELIVERED`, `COMPLETED`, `CANCELLED`, `FAILED`
- Returns/Refunds: `RETURNING`, `RETURNED`, `REFUNDED`

**Structure:** Each constant has `displayName` (Vietnamese) and `description` (detailed Vietnamese explanation).

**Data Flow:** Represents state transitions in order management workflows; combined with `PaymentStatus` to track payment separately from order processing.

**Public API:** 
- `OrderStatus.CONSTANT_NAME.getDisplayName()` — UI-friendly label
- `OrderStatus.CONSTANT_NAME.getDescription()` — Detailed explanation

---

## PaymentMethod.java

**Purpose:** Enumerates supported payment gateways and methods.

**Key Constants:**
- `VNPAY` — Vietnamese bank transfer
- `CARD` — Visa/Mastercard
- `PAYPAL` — PayPal account
- `GOOGLE_PAY` — Google Pay
- `APPLE_PAY` — Apple Pay
- `COD` — Cash on Delivery

**Structure:** `displayName` field (Vietnamese), Lombok getters.

**Public API:** `PaymentMethod.CONSTANT_NAME.getDisplayName()` for checkout UI.

**Dependencies:** Used alongside `PaymentType` and `PaymentStatus` to model complete payment workflows.

---

## PaymentStatus.java

**Purpose:** Tracks payment transaction states independent of order status.

**Key Constants:**
- `UNPAID` — No payment received
- `PENDING` — Processing payment
- `COD` — Payment upon delivery
- `PAID` — Successfully completed
- `FAILED` — Transaction rejected
- `CANCELLED` — Transaction cancelled
- `REFUND_FAILED` — Refund processing error
- `REFUNDED` — Refund completed

**Structure:** Immutable enum with `displayName` field.

**Data Flow:** Orthogonal to `OrderStatus`; order may be `CONFIRMED` while payment is still `PENDING`.

**Public API:** `PaymentStatus.CONSTANT_NAME.getDisplayName()`.

---

## PaymentType.java

**Purpose:** Categorizes high-level payment arrangement types beyond individual methods.

**Key Constants:**
- `PAYMENT_UPON_DELIVERY` — Pay at receipt
- `MOMO` — Momo wallet payment
- `BUY_NOW_PAY_LATER` — Installment/deferred payment

**Structure:** `description` field (Vietnamese), Lombok annotations.

**Relationships:** Complements `PaymentMethod` (which is the specific gateway) and `PaymentStatus` (which is the transaction state).

**Public API:** `PaymentType.CONSTANT_NAME.getDescription()`.

---

## RoleType.java

**Purpose:** Defines authorization levels for role-based access control (RBAC).

**Key Constants:**
- `USER` — End customer
- `ADMIN` — System administrator
- `SUPER_ADMIN` — Unrestricted system access
- `EMPLOYEE` — Staff member
- `MANAGEMENT` — Supervisory/managerial access

**Structure:** Simple enum without fields (role names are identifiers).

**Public API:** Used in authorization decorators and permission checking (e.g., `@RoleRequired(RoleType.ADMIN)`).

**Design Pattern:** Type-safe role enumeration; roles are assigned via `ActiveStatus.ASSIGN_ROLE` actions.

---

## SearchOperation.java

**Purpose:** Defines query operators for dynamic search/filtering in REST APIs.

**Key Constants (9 total):**
- `EQUALITY` (`:`) — Exact match
- `NEGATION` (`!`) — Exclude
- `GREATER_THAN` (`>`) — Numeric/date comparison
- `LESS_THAN` (`<`) — Numeric/date comparison
- `LIKE` (`~`) — Pattern match
- `STARTS_WITH`, `ENDS_WITH`, `CONTAINS` — String operations
- `IN` — Multiple values

**Structure:** Each constant has a `symbol` field for query string representation.

**Data Flow:** Parsed from query parameters (e.g., `?field:value` or `?field>100`) to build dynamic SQL predicates.

**Public API:** `SearchOperation.CONSTANT_NAME.getSymbol()` for query parsing.

**Design Pattern:** Symbol-to-operation mapping enables flexible REST query DSL without custom annotations.

---

## ShoppingCartType.java

**Purpose:** Categorizes shopping cart states (minimal usage).

**Key Constants:**
- `SHOPPING_CART` — Active cart state

**Structure:** Single constant with `description` field ("Trạng thái của Giỏ hàng.").

**Note:** Appears underutilized; may expand for cart state tracking (active, abandoned, converted).

**Public API:** `ShoppingCartType.SHOPPING_CART.getDescription()`.

---

## StockStatus.java

**Purpose:** Tracks product inventory availability.

**Key Constants:**
- `AVAILABLE` — Stock on hand
- `UNAVAILABLE` — Out of stock
- `COMING_SOON` — Restocking expected
- `NOT_ACTIVE` — Product not yet active

**Structure:** `value` field (Vietnamese labels), Lombok getters.

**Data Flow:** Set on products; influences `OrderStatus` transitions (cannot proceed if stock unavailable).

**Public API:** `StockStatus.CONSTANT_NAME.getValue()` for UI display.

---

## UserRank.java

**Purpose:** Defines customer loyalty/membership tiers for rewards/benefits programs.

**Key Constants (6 total):**
- `MEMBER` — Base level
- `BRONZE`, `SILVER`, `GOLD` — Progression tiers
- `PLATINUM`, `DIAMOND` — Premium levels

**Structure:** Simple enum without fields (rank names are identifiers).

**Public API:** Direct constant access in user profile/loyalty contexts.

**Design Pattern:** Ordinal-based progression (intended for tier comparisons and benefit eligibility).

---

## Cross-Cutting Concerns

**Dependencies:** No inter-enum dependencies; all are standalone type definitions.

**External Dependencies:** 
- `lombok.Getter` — Automatic getter generation (all except `Gender`, `RoleType`, `UserRank`)
- `lombok.RequiredArgsConstructor` — Constructor for immutable final fields

**Configuration:** All descriptions/messages are hardcoded in Vietnamese; no externalized configuration.

**Design Patterns:**
- **Type Safety:** All enums prevent invalid state assignments.
- **Immutability:** Lombok `@RequiredArgsConstructor` ensures instances cannot be modified.
- **Symbol Mapping:** `SearchOperation` uses symbols for DSL query parsing.

**Public API Surface:** Enums are consumed across models, controllers, services, and repositories for type-safe state management, authorization, and business logic decisions.