# Entity Group Knowledge Document

## Overview

The **entity** group defines the core domain models for an ERP e-commerce system. It comprises 12 JPA entities managing products, orders, inventory, users, shopping carts, payments, and event outboxing. The entities use Lombok for boilerplate reduction, custom converters for complex types, and embedded objects for cross-cutting concerns like audit information. The design emphasizes denormalization for analytics and follows event sourcing patterns via the outbox entity.

---

## File: Address.java

**Purpose**: Represents shipping/delivery addresses linked to users. Stores geolocation data and contact information for order fulfillment.

**Key Class**:
- `Address extends IdentityOnly<Long>` — JPA entity with auto-generated ID

**Attributes**:
- `address` (String, 500 chars) — Full shipping address
- `latitude`, `longitude` (Double) — Geolocation coordinates
- `phoneNumber` (String, 20 chars) — Recipient phone
- `recipientName` (String, 200 chars) — Recipient name
- `user` (ManyToOne, lazy) — FK to User entity
- `auditInfo` (Embedded) — Creation/update timestamps

**Database**: Table `addresses` with index on `user_id`

**Design Pattern**: Entity aggregation — Address is a child of User

---

## File: Attributes.java

**Purpose**: Represents product variants/SKUs with pricing, promotions, specifications, and analytics. Each attribute record is a distinct product configuration.

**Key Class**:
- `Attributes extends BaseEntity<Long>` — JPA entity with audit trail support

**Key Attributes**:
- `sku` (Embedded SkuInfo, overridden column `sku_name`) — Product variant identifier
- `name` (String) — Attribute name
- `price`, `salePrice` (double) — Regular and promotional pricing
- `variantOptions` (List<VariantOption>, CLOB via converter) — Color, size, etc.
- `statusProduct` (Enum: StockStatus) — Stock availability
- `specifications` (List<SpecificationGroup>, CLOB) — Technical specs (converted from JSON)
- `keywords` (Set<String>, ElementCollection) — Search indexing
- `promotions` (List<Promotion>, CLOB) — Active promotions
- `costPrice`, `soldQuantity`, `totalOrders` (analytics fields) — Denormalized metrics
- `product` (ManyToOne, lazy, cascade delete) — FK to Product

**Database**: Table `Attributes` with indexes on `product_id` and `sku_name`

**Design Pattern**: Value Object with denormalized analytics; converters handle complex JSON serialization

---

## File: CartItem.java

**Purpose**: Individual line item in a shopping cart. Replaces previous CLOB JSON approach with normalized entity relationships.

**Key Class**:
- `CartItem extends IdentityOnly<Long>` — JPA entity

**Attributes**:
- `cart` (ManyToOne, lazy, FK) — Parent ShoppingCart
- `sku` (String, 100 chars, not null) — Product variant identifier
- `quantity` (Integer, default 0) — Item quantity

**Database**: Table `cart_items` with indexes on `cart_id` and `sku`

**Design Pattern**: Child entity of ShoppingCart using cascade delete

---

## File: Category.java

**Purpose**: Product categorization hierarchy. Each category groups related products and contains SKU metadata.

**Key Class**:
- `Category extends BaseEntity<Long>` — JPA entity

**Attributes**:
- `name` (String) — Category name
- `skuInfo` (Embedded SkuInfo, overridden column `sku_name`) — Category-level SKU identifier
- `products` (OneToMany, mappedBy "category", cascade all, lazy) — Child products

**Database**: Table `Category`

**Design Pattern**: Parent aggregator; uses cascade delete for orphan removal

---

## File: Order.java

**Purpose**: Core order entity tracking complete order lifecycle from creation through delivery. Implements status history as append-only list and comprehensive financial/shipping details.

**Key Class**:
- `Order extends IdentityOnly<Long>` — JPA entity

**Key Attributes**:

*Order Information*:
- `orderNumber` (String, 50 chars, unique) — Order reference
- `status` (List<OrderStatus>, CLOB via OrderStatusListConverter) — Append-only status history
- `currentStatus` (Enum: OrderStatus) — Latest status (denormalized for query optimization)
- `trackingNumber` (String, 100 chars) — Logistics tracking ID

*Customer Information*:
- `customer` (ManyToOne, lazy) — FK to User
- `customerName`, `customerEmail`, `customerPhone` — Denormalized customer snapshot

*Order Items*:
- `orderItems` (OneToMany, mappedBy "order", cascade all, lazy) — Line items

*Financial*:
- `subtotal`, `discountAmount`, `discountCode`, `taxAmount`, `shippingFee`, `totalAmount` (Double) — Pricing breakdown

*Shipping*:
- `shippingInfo` (ManyToOne, lazy) — FK to Address
- `shippingMethod` (String, 100 chars)
- `estimatedDeliveryDate`, `actualDeliveryDate` (LocalDateTime)

*Audit Trail*:
- `customerNotes`, `adminNotes` (String, 2000 chars) — Comments
- `cancellationReason`, `cancelledAt`, `cancelledBy` — Cancellation metadata
- `confirmedAt`, `confirmedBy`, `completedAt` — Status change timestamps

*Delivery*:
- `shipperId`, `shipperName`, `shipperPhone`, `deliveryToken` — Shipper info

**Database**: Table `orders` with indexes on `customer_id` and `tracking_number`

**Design Pattern**: Event sourcing (status history); denormalization for analytics and audit trail

---

## File: OrderItem.java

**Purpose**: Individual product line in an order. Captures product snapshot, pricing, and variant details at purchase time.

**Key Class**:
- `OrderItem extends IdentityOnly<Long>` — JPA entity

**Key Attributes**:
- `order` (ManyToOne, lazy, FK, cascade delete) — Parent Order
- `product` (ManyToOne, lazy, FK) — Referenced Product
- `attributes` (ManyToOne, lazy, FK) — Referenced Attributes (variant)
- `productName`, `productSku` (String) — Denormalized product snapshot
- `attributesSku` (String, 100 chars) — Variant SKU
- `variantOptions` (List<VariantOption>, CLOB) — Color, size selections
- `quantity`, `unitPrice`, `salePrice` (Integer/Double) — Quantity and pricing
- `discountAmount`, `discountPercentage` (Double) — Item-level discount
- `subtotal`, `taxAmount` (Double) — Line totals
- `imageUrl` (String, 500 chars) — Product image at time of order

**Methods**:
- `calculateSubtotal()` — Computes subtotal from quantity, sale price, and discount

**Database**: Table `order_items` with indexes on `order_id`, `product_id`, `attributes_id`

**Design Pattern**: Immutable snapshot of product state; denormalized to preserve order data if product is deleted

---

## File: OutboxEvent.java

**Purpose**: Implements the Outbox pattern for transactional event publishing. Events are persisted in the same transaction as business data, then a scheduled job polls and publishes to Kafka, ensuring exactly-once delivery semantics.

**Key Class**:
- `OutboxEvent extends IdentityOnly<Long>` — JPA entity

**Key Attributes**:
- `aggregateType` (String, 100 chars) — Entity type (ORDER, PAYMENT, PRODUCT)
- `aggregateId` (Long) — Entity ID
- `eventType` (String, 100 chars) — Event name (ORDER_CREATED, ORDER_CONFIRMED)
- `topic` (String, 255 chars) — Target Kafka topic
- `messageKey` (String, 255 chars) — Kafka partition key
- `payload` (String, CLOB) — Event JSON payload
- `correlationId` (String, 100 chars) — Distributed trace ID
- `retryCount` (Integer, default 0) — Retry attempts
- `createdAt`, `sentAt`, `nextRetryAt` (LocalDateTime) — Timestamps
- `lastError` (String, CLOB) — Error details
- `status` (String, default "PENDING") — Status: PENDING, SENT, FAILED, DEAD

**Methods**:
- `markAsSent()` — Sets status to SENT, clears error
- `markAsFailed(String errorMessage)` — Increments retry count, schedules exponential backoff (1m→5m→15m→1h→6h→24h), marks DEAD after 6 attempts
- `needsRetry()` — Checks if retry is needed and due
- `@PrePersist onCreate()` — Initializes createdAt and status on persist

**Database**: Table `outbox_events` with indexes on `(aggregate_type, aggregate_id)`, `sent_at`, `event_type`, `created_at`

**Design Pattern**: Event sourcing; transactional outbox; exponential backoff retry with dead-letter queue

---

## File: Payment.java

**Purpose**: Records payment transactions linked to orders. Stores provider details, transaction codes, and bank-specific metadata.

**Key Class**:
- `Payment extends IdentityOnly<UUID>` — JPA entity with UUID primary key

**Attributes**:
- `order` (ManyToOne, lazy) — FK to Order
- `provider` (String, 50 chars) — Payment gateway (Stripe, VNPay, etc.)
- `transactionCode` (String, 100 chars) — Provider transaction ID
- `amount` (Double) — Payment amount
- `status` (String, 50 chars) — Status (PENDING, SUCCESS, FAILED)
- `paymentDate` (LocalDateTime) — Timestamp
- `description` (String, 500 chars) — Notes
- `rawResponse` (String, CLOB) — Full provider response JSON
- `bankCode`, `cardType`, `bankTranNo` (String) — Bank-specific fields
- `ipAddress` (String, 50 chars) — Client IP for fraud detection

**Database**: Table `payment`

**Design Pattern**: Immutable transaction record; denormalized provider metadata for audit

---

## File: Product.java

**Purpose**: Core product entity representing a sellable item. Aggregates SKU info, media, category, and variants while tracking analytics metrics.

**Key Class**:
- `Product extends BaseEntity<Long>` — JPA entity with audit trail

**Key Attributes**:
- `name` (String) — Product name
- `skuInfo` (Embedded SkuInfo, default initialized) — Base product SKU
- `mediaItems` (List<MediaItem>, CLOB via converter) — Images/videos
- `category` (ManyToOne, lazy, cascade delete) — FK to Category
- `attributes` (OneToMany, mappedBy "product", cascade all, lazy) — Product variants
- `status` (Enum: ActiveStatus) — ACTIVE, INACTIVE

*Analytics (denormalized)*:
- `totalSoldQuantity`, `totalOrders` (Integer) — Sales count
- `totalRevenue` (BigDecimal) — Total earnings
- `viewCount` (Integer) — Page views
- `averageRating`, `reviewCount` (Double/Integer) — Review metrics

*Discount*:
- `discountPercent` (Double) — Percentage discount
- `discountStartDate`, `discountEndDate` (LocalDateTime) — Promotion window

**Database**: Table `Product` with index on `category_uuid`

**Design Pattern**: Aggregate root; denormalized analytics; cascade lifecycle management

---

## File: ProductInventory.java

**Purpose**: Manages stock levels per SKU using optimistic locking (version field) to prevent overselling in concurrent scenarios.

**Key Class**:
- `ProductInventory extends IdentityOnly<Long>` — JPA entity

**Key Attributes**:
- `sku` (String, unique, not null) — Product variant identifier
- `availableQuantity` (Integer, not null) — Purchasable stock
- `reservedQuantity` (Integer, default 0) — Stock reserved for pending orders
- `version` (Long, @Version) — Optimistic lock version
- `product` (ManyToOne, lazy) — FK to Product

**Database**: Table `product_inventory` with unique index on `sku`

**Design Pattern**: Optimistic locking for concurrent inventory updates; normalized stock tracking

---

## File: ShoppingCart.java

**Purpose**: User's active shopping cart with line items (normalized as CartItem entities instead of JSON CLOB). Tracks totals and timestamps for cart management.

**Key Class**:
- `ShoppingCart extends IdentityOnly<Long>` — JPA entity

**Key Attributes**:
- `auditInfo` (Embedded AuditInfo, default initialized) — Audit trail
- `user` (OneToOne, lazy, unique FK) — Single cart per user
- `cartItems` (OneToMany, mappedBy "cart", cascade all, lazy) — Line items
- `totalItems`, `totalPrice`, `totalSalePrice`, `totalDiscount` (Integer/Double) — Aggregated totals
- `lastActivityAt` (LocalDateTime) — Last modification timestamp

**Methods**:
- `addItem(String sku, int quantity)` — Add or increment SKU; creates CartItem if not present
- `removeItemBySku(String sku)` — Delete cart item by SKU
- `updateTotals(Integer totalItems, Double totalPrice, Double totalSalePrice)` — Refresh aggregate totals and discount
- `clearItems()` — Empty cart
- `touchActivity()` — Update last activity timestamp

**Database**: Table `shopping_cart` with unique index on `user_id`

**Design Pattern**: Child entity aggregation; denormalized totals for query optimization

---

## File: User.java

**Purpose**: Core user entity implementing Spring Security's `UserDetails`. Manages authentication, roles, profile data, and user ranking/loyalty tiers.

**Key Class**:
- `User extends BaseEntity<Long> implements UserDetails` — JPA entity with Spring Security integration

**Key Attributes**:
- `fullName` (String, regex validated) — Display name
- `name` (String, unique) — Username/login
- `password` (String, not null) — Encrypted password
- `phoneNumber` (String, regex: 10 digits) — Contact phone
- `email` (String, unique, not null, @Email) — Email address
- `dateOfBirth` (Date) — DOB
- `avatarUrl` (String) — Profile picture URL
- `roles` (Set<RoleType>, ElementCollection, eager) — User roles (ADMIN, USER, etc.)
- `authCode` (Embedded AuthCode, default initialized) — OTP/verification state
- `status` (Enum: ActiveStatus) — ACTIVE, LOCKED, INACTIVE
- `gender` (Enum: Gender) — M, F, OTHER
- `rank` (Enum: UserRank, default MEMBER) — Loyalty tier

**UserDetails Implementation**:
- `getAuthorities()` — Maps roles to Spring GrantedAuthority with "ROLE_" prefix
- `getUsername()` — Returns email
- `isAccountNonExpired()` — Always true
- `isAccountNonLocked()` — False if status is LOCKED
- `isCredentialsNonExpired()` — Always true
- `isEnabled()` — True if status is ACTIVE

**Database**: Table `Users` with indexes on `email` (unique), `phone_number`, `status`. ElementCollection `user_roles` stores role mappings.

**Design Pattern**: Security principal; embedded auth state; role-based access control (RBAC)

---

## Data Flow

```
User
  ├─> Address (1:M) — Shipping addresses
  ├─> ShoppingCart (1:1) — Active cart
  │    └─> CartItem (1:M) — Cart line items
  └─> Order (1:M) — Order history
       ├─> OrderItem (1:M) — Order line items
       │    ├─> Product (M:1)
       │    └─> Attributes (M:1)
       ├─> Address (M:1) — Shipping address
       └─> Payment (1:M) — Transactions

Product (1:M) → Category
Product (1:M) → Attributes (variants)
Product (1:1) → ProductInventory (stock tracking)

OutboxEvent — Async event log (decoupled from business transactions)
```

---

## Dependencies & Imports

**Common across all entities**:
- `jakarta.persistence.*` — JPA annotations (Entity, Table, Column, ManyToOne, OneToMany, etc.)
- `lombok.*` — Boilerplate (Getter, Setter, Builder, AllArgsConstructor, NoArgsConstructor, FieldDefaults, SuperBuilder)
- `com.anno.ERP_SpringBoot_Experiment.model.base.*` — Base classes (IdentityOnly, BaseEntity)
- `com.anno.ERP_SpringBoot_Experiment.model.embedded.*` — Embedded types (AuditInfo, SkuInfo, VariantOption, SpecificationGroup, Promotion, MediaItem, AuthCode)
- `com.anno.ERP_SpringBoot_Experiment.model.enums.*` — Enums (OrderStatus, StockStatus, ActiveStatus, Gender, RoleType, UserRank)
- `com.anno.ERP_SpringBoot_Experiment.config.converter.*` — Custom converters (VariantOptionListConverter, SpecificationGroupListConverter, PromotionListConverter, MediaItemListConverter, OrderStatusListConverter)
- `org.hibernate.annotations.OnDelete` — Cascade behavior control
- `com.fasterxml.jackson.annotation.JsonIgnore` — Serialization exclusion
- `org.springframework.security.core.*` — Spring Security (User class only)

---

## Design Patterns

1. **Aggregate Root** — Product, Order, User, ShoppingCart own child entities
2. **Value Object** — Embedded types (AuditInfo, SkuInfo, VariantOption, AuthCode)
3. **Event Sourcing** — Order.status (append-only history), OutboxEvent (transactional event log)
4. **Outbox Pattern** — OutboxEvent ensures exactly-once Kafka publishing
5. **Snapshot/Denormalization** — OrderItem, Attributes store product state at transaction time; analytics fields (totalOrders, soldQuantity) pre-calculated
6. **Optimistic Locking** — ProductInventory.version prevents overselling
7. **Lazy Loading** — ManyToOne/OneToMany relations use FetchType.LAZY to minimize queries
8. **Cascade Lifecycle** — Parent deletion cascades to orphaned children (CascadeType.ALL, orphanRemoval=true)
9. **Spring Security Principal** — User implements UserDetails for authentication/authorization

---

## Public API

**Entities expose**:
- Builder patterns (via Lombok) for object construction
- Getter/setter accessors for all fields
- Helper methods (Order status queries, ShoppingCart item operations, OutboxEvent retry logic, OrderItem subtotal calculation, User authority resolution)
- Enum types for type-safe status/state representation
- Collection access (product attributes, order items, cart items, user roles)

**Entity relationships expose**:
- ManyToOne/OneToMany navigation (traversing from parent to child or FK to parent)
- Lazy-loaded relations for performance control
- Cascade behavior for transactional consistency

---

## Configuration & Constants

**Annotations & Validation**:
- `@NotNull`, `@Email`, `@Pattern` — Bean validation constraints on User
- `@Enumerated(EnumType.STRING)` — Enum persistence as string (not ordinal)
- `@Version` — Optimistic lock version (ProductInventory only)
- `@Embedded`, `@AttributeOverride` — Embed complex types with column remapping

**Converter-Driven Serialization**:
- `VariantOptionListConverter`, `SpecificationGroupListConverter`, `PromotionListConverter`, `MediaItemListConverter` — Serialize List/Set to CLOB JSON
- `OrderStatusListConverter` — Append-only status history

**Database Constraints**:
- Unique indexes: email, phone (User); order_number (Order); sku (ProductInventory); user_id (ShoppingCart); user_id (Address)
- Foreign keys: cascade delete on Product/Category, Order/OrderItem, ShoppingCart/CartItem
- Length limits: address (500), email (200), product name (500), etc.

**Default Values**:
- `quantity` (CartItem, ShoppingCart): 0
- `reservedQuantity` (ProductInventory): 0
- `costPrice` (Attributes): 0.0
- `auditInfo` (Address, ShoppingCart, Order): new AuditInfo()
- `rank` (User): UserRank.MEMBER
- `status` (OutboxEvent): "PENDING"
- `retryCount` (OutboxEvent): 0