# Request DTO Group Knowledge Document

## Overview

The **request** group contains 20 Data Transfer Objects (DTOs) that standardize HTTP request payloads across the ERP system. These DTOs handle domain operations including product management (creation, search, attributes), order lifecycle (creation, cancellation, status updates), cart operations, authentication, and payment callbacks. All classes use Lombok for boilerplate reduction and Jakarta/Fasterxml validation annotations for input sanitization.

---

## File-by-File Analysis

### AccountVerificationRequest
**Purpose:** Handles password change validation during account verification.

**Key Class:**
- `AccountVerificationRequest` — Validates new password and confirmation
  - `newPassword` (String) — New password, min 6 characters, required
  - `confirmPassword` (String) — Password confirmation, required

**Validation:** Uses `@NotBlank` and `@Size(min=6)` constraints. Fields are mapped from JSON as `new_password` and `confirm_password`.

**Design Pattern:** Simple value object with immutable fields via `@Getter` and `@FieldDefaults(PRIVATE)`.

---

### AttributeInput
**Purpose:** Represents a single product variant with pricing, promotions, and stock details for bulk attribute creation.

**Key Class:**
- `AttributeInput` — Variant specification container
  - `name`, `value` (String) — Variant identifiers
  - `price`, `salePrice` (BigDecimal) — Pricing tiers
  - `variantOptions` (List<VariantOption>) — Variant dimensions
  - `promotions` (List<Promotion>) — Active promotions
  - `specifications` (List<SpecificationGroup>) — Product specs
  - `statusProduct` (StockStatus enum) — Inventory status

**Dependencies:** Imports `VariantOption`, `Promotion`, `SpecificationGroup` from `model.embedded` and `StockStatus` from `model.enums`.

---

### AttributesSearchRequest
**Purpose:** Multi-criteria search request for product variants/attributes with pagination and date range filtering.

**Key Class:**
- `AttributesSearchRequest` — Search filter container
  - `keyword` (String) — Text search
  - `productId`, `ids`, `productIds`, `skus` (various) — Entity lookups
  - `statuses` (List<String>) — Stock status filters
  - `minPrice`, `maxPrice`, `minSalePrice`, `maxSalePrice` (Double) — Price ranges
  - `minSoldQuantity`, `maxSoldQuantity` (Integer) — Volume filters
  - `minCostPrice`, `maxCostPrice` (Double) — Cost ranges
  - `createdBy` (String) — Creator filter
  - `createdFrom`, `createdTo`, `updatedFrom`, `updatedTo` (LocalDateTime) — Date ranges
  - `paging` (PagingRequest) — Pagination (defaults to page 1, size 10)

**Data Flow:** Passed to service layer for querying; `paging` object converts to Spring `Pageable`.

---

### CancelOrderRequest
**Purpose:** Encapsulates order cancellation with ID normalization and reason tracking.

**Key Class:**
- `CancelOrderRequest`
  - `orderId` (String) — Order identifier, normalized via `@NormalizedId` (uppercase + dash removal)
  - `cancellationReason` (String) — Required cancellation justification

**Custom Annotations:** Uses `@NormalizedId` for automatic ID transformation before validation.

**Design Pattern:** Builder pattern with `@Builder` and `@AllArgsConstructor`.

---

### CartItemRequest
**Purpose:** Lightweight immutable request for adding items to cart.

**Key Class:**
- `CartItemRequest` — Immutable value object via `@Value`
  - `sku` (String) — Product variant identifier
  - `quantity` (int) — Desired quantity

---

### CategorySearchRequest
**Purpose:** Search request for product categories with filtering and pagination.

**Key Class:**
- `CategorySearchRequest`
  - `ids`, `skus`, `names` (List<String>) — Category identifiers
  - `keyword` (String) — Text search
  - `createdBy` (String) — Creator filter
  - `createdFrom`, `createdTo`, `updatedFrom`, `updatedTo` (LocalDateTime) — Audit date ranges
  - `paging` (PagingRequest) — Pagination support

---

### CheckoutRequest
**Purpose:** Validates cart items before order creation with nested item structure.

**Key Classes:**
- `CheckoutRequest` — Container for checkout items
  - `items` (List<CheckoutItem>) — Non-empty list of items, validated via `@Valid`

- `CheckoutItem` — Nested static class
  - `sku` (String) — Product variant identifier, required
  - `quantity` (Integer) — Must be positive (`@Positive`), required

**Validation Hierarchy:** Nested `@Valid` enables cascading validation of CheckoutItem fields.

---

### CompleteOrderRequest
**Purpose:** Marks an order as completed with completion metadata.

**Key Class:**
- `CompleteOrderRequest`
  - `orderId` (String) — Target order
  - `completionInfo` (String) — Completion details/notes
  - `completedAt` (LocalDateTime) — Completion timestamp

---

### ConfirmOrderRequest
**Purpose:** Confirms order acceptance with auditing information.

**Key Class:**
- `ConfirmOrderRequest`
  - `orderId` (String) — Target order
  - `confirmationInfo` (String) — Confirmation details
  - `confirmedAt` (LocalDateTime) — Confirmation timestamp
  - `confirmedBy` (String) — User who confirmed

---

### ConfirmReturnRequest
**Purpose:** Handles return/refund confirmation with shipping and payment metadata.

**Key Class:**
- `ConfirmReturnRequest` — Flexible container with 21 fields
  - Order identifiers: `orderId`, `orderNumber`
  - Shipping: `shipperId`, `shipperName`, `shipperPhone`, `estimatedDeliveryDate`, `actualDeliveryDate`, `pickupDeadline`
  - Recipient: `recipientName`, `condition`
  - Refund: `refundAmount`
  - Payment: `transactionId`, `status`, `paymentMethod`, `amount`
  - Notes: `note`, `reason`
  - Extensibility: `rawData` (Map<String, Object>) for unmodeled fields

**Design Pattern:** Flexible structure accepts arbitrary data via `rawData` map for integration scenarios.

---

### CreateAttributesBatchRequest
**Purpose:** Batch creation of product variants using product ID with shared metadata.

**Key Class:**
- `CreateAttributesBatchRequest`
  - `name` (String) — Common product name for all variants, required
  - `productId` (String) — Parent product ID, normalized via `@NormalizedId`, supports aliases (`id`, `productId`, `product_id`)
  - `keywords` (Set<String>) — SEO keywords
  - `attributes` (List<AttributeInput>) — Variant list, non-empty, validated via `@Valid`

**Custom Annotations:** `@NormalizedId` + `@JsonAlias` for flexible input parsing.

**Dependencies:** References `AttributeInput` for variant details.

---

### CreateAttributesRequest
**Purpose:** Creates product variants using product SKU instead of ID.

**Key Class:**
- `CreateAttributesRequest`
  - `name` (String) — Common product name, required
  - `productSku` (String) — Parent product SKU, required, supports aliases
  - `keywords` (Set<String>) — SEO keywords
  - `attributes` (List<AttributeInput>) — Variant list, non-empty, validated via `@Valid`

**Key Difference from CreateAttributesBatchRequest:** Uses SKU lookup instead of ID lookup.

---

### CreateOrderRequest
**Purpose:** Creates new orders with flexible sourcing (direct items or from cart), payment, and shipping details.

**Key Classes:**
- `CreateOrderRequest` — Main order creation container
  - `items` (List<OrderItemRequest>) — Order line items (optional if `isFromCart=true`)
  - `isFromCart` (boolean) — Flag to load items from user's cart
  - `addressId` (String) — Shipping address, required
  - `discountCode` (String) — Promotional code
  - `customerNotes` (String) — Customer remarks
  - `shippingMethod` (String) — Shipping option
  - `paymentMethod` (PaymentMethod enum) — Payment type
  - `language` (String) — Payment UI language (vn, en)
  - `bankCode` (String) — Bank identifier for VNPay/similar

- `OrderItemRequest` — Nested line item
  - `attributesSku` (String) — Product variant SKU, required
  - `quantity` (Integer) — Order quantity, required

**Dependencies:** Imports `PaymentMethod` enum from `model.enums`.

**Design Pattern:** Builder pattern with nested static class for composition.

---

### CreateProductRequest
**Purpose:** Product creation with basic metadata and discount scheduling.

**Key Class:**
- `CreateProductRequest`
  - `name` (String) — Product name, required
  - `categorySku` (String) — Parent category SKU, required, supports aliases
  - `status` (String) — Product status
  - `discountPercent` (Double) — Discount percentage (optional)
  - `discountStartDate`, `discountEndDate` (LocalDateTime) — Discount validity window

**JSON Mapping:** Uses `@JsonAlias` for flexible input field names.

---

### DelayOrderRequest
**Purpose:** Records order delivery delay with comprehensive shipping and payment context (identical to ConfirmReturnRequest structure).

**Key Class:**
- `DelayOrderRequest` — 21-field container
  - Core fields: `orderId`, `orderNumber`, `status`
  - Shipping: `shipperId`, `shipperName`, `shipperPhone`, `estimatedDeliveryDate`, `actualDeliveryDate`, `pickupDeadline`
  - Payment: `transactionId`, `paymentMethod`, `amount`
  - Metadata: `note`, `reason`, `condition`, `refundAmount`, `recipientName`
  - Extensibility: `rawData` (Map<String, Object>)

**Design Pattern:** Mirrors ConfirmReturnRequest for consistent third-party integration handling.

---

### DeliverOrderRequest
**Purpose:** Records successful delivery with shipping and payment metadata (identical structure to ConfirmReturnRequest and DelayOrderRequest).

**Key Class:**
- `DeliverOrderRequest` — 21-field container
  - Same fields as DelayOrderRequest for consistent state transition recording

**Design Pattern:** Parallel structure indicates shared data model across order lifecycle events.

---

### GetProductRequest
**Purpose:** Advanced product search with multi-dimensional filtering including sales analytics and ratings.

**Key Class:**
- `GetProductRequest`
  - Text/ID search: `keyword`, `categoryId`, `productIds`, `skus`, `statuses`, `categoryIds`
  - Creator: `createdBy`
  - Sales metrics: `minSoldQuantity`, `maxSoldQuantity`, `minRevenue`, `maxRevenue`, `minOrders`, `maxOrders`, `minView`
  - Customer feedback: `minRating`, `minReviews`
  - Audit dates: `createdFrom`, `createdTo`, `updatedFrom`, `updatedTo`
  - `paging` (PagingRequest) — Pagination (defaults to page 1, size 10)

**Data Flow:** Comprehensive filter object enables analytics-driven product queries.

---

### OrderSearchRequest
**Purpose:** Dedicated order search with customer info, status, amount, and sorting capabilities.

**Key Class:**
- `OrderSearchRequest`
  - Order identifiers: `orderNumber`
  - Customer info: `customerId` (normalized via `@NormalizedId`), `customerName`, `customerEmail`, `customerPhone`
  - Status: `orderStatus` (OrderStatus enum)
  - Date range: `startDate`, `endDate`
  - Amount filters: `minAmount`, `maxAmount`
  - Pagination: `page`, `size`
  - Sorting: `sortBy` (field name), `sortDirection` (ASC/DESC)

**Dependencies:** Imports `OrderStatus` enum from `model.enums`.

**Design Pattern:** Builder pattern with explicit pagination/sorting fields (differs from PagingRequest approach).

---

### PagingRequest
**Purpose:** Reusable pagination abstraction converting user input to Spring Data `Pageable`.

**Key Class:**
- `PagingRequest`
  - `page` (int) — Page number, defaults to 1 (converted to 0-indexed for Spring)
  - `size` (int) — Items per page, defaults to 10
  - `orders` (Map<String, String>) — Sort mappings (field → ASC/DESC)

**Key Methods:**
- `pageable()` — Converts to Spring `PageRequest` with sorting
- `sortable(Map<String, String>)` — Constructs Spring `Sort` object
  - Filters empty keys and `additionalProp*` (Swagger schema artifacts)
  - Defaults to DESC for unrecognized values, ASC as fallback

**Public API:** Used by all search requests as composition/delegation; enables consistent pagination across domain.

---

### PaymentCallbackRequest
**Purpose:** Webhook handler for payment provider callbacks with full payment and shipping context (identical to ConfirmReturnRequest/DelayOrderRequest structure).

**Key Class:**
- `PaymentCallbackRequest` — 21-field container
  - Payment: `transactionId`, `status`, `paymentMethod`, `amount`, `orderId`
  - Shipping: `shipperId`, `shipperName`, `shipperPhone`, `estimatedDeliveryDate`, `actualDeliveryDate`, `pickupDeadline`
  - Audit: `note`, `reason`, `condition`, `refundAmount`, `recipientName`, `orderNumber`
  - Extensibility: `rawData` (Map<String, Object>) for provider-specific fields

**Design Pattern:** Flexible event integration model allowing unmodeled payment provider attributes via `rawData`.

---

## Cross-Cutting Concerns

### Validation Framework
All DTOs use **Jakarta Validation** annotations:
- `@NotNull`, `@NotBlank` — Mandatory fields
- `@NotEmpty` — Non-empty collections
- `@Size(min, max)` — String length constraints
- `@Positive` — Numeric bounds
- `@Valid` — Cascading validation for nested objects

### Custom Annotations
- `@NormalizedId` — Applied to `orderId`, `customerId`, `productId` fields; normalizes to uppercase + removes dashes before validation

### JSON Mapping Flexibility
- `@JsonProperty` — Explicit field name mapping (snake_case input)
- `@JsonAlias` — Accepts multiple input field names (e.g., `productId`, `product_id`, `id` all resolve to same field)

### Lombok Conventions
- `@Data` — Auto-generates getters, setters, equals, hashCode, toString
- `@Builder` — Enables fluent object construction
- `@FieldDefaults(level=PRIVATE)` — All fields private by default
- `@AllArgsConstructor`, `@NoArgsConstructor` — Constructor variants
- `@Value` — Immutable variant (CartItemRequest)

### Pagination & Sorting Pattern
- All search requests include `PagingRequest paging` (defaults to page 1, size 10)
- `OrderSearchRequest` uses explicit `page`, `size`, `sortBy`, `sortDirection` fields instead
- `PagingRequest.orders` Map enables arbitrary field-based sorting

### State Transition DTOs
`ConfirmReturnRequest`, `DelayOrderRequest`, `DeliverOrderRequest`, `PaymentCallbackRequest` share identical 21-field structure, suggesting shared event model for order lifecycle webhooks/callbacks.

---

## Public API Summary

| DTO Class | Primary Use | Consumer |
|-----------|------------|----------|
| AccountVerificationRequest | Password change validation | Auth controller |
| AttributeInput | Variant specification | CreateAttributesRequest/Batch |
| AttributesSearchRequest | Variant search with pagination | Attribute service |
| CancelOrderRequest | Order cancellation | Order service |
| CartItemRequest | Cart addition | Cart controller |
| CategorySearchRequest | Category search | Category service |
| CheckoutRequest | Pre-order validation | Checkout controller |
| CompleteOrderRequest | Order completion | Order service |
| ConfirmOrderRequest | Order confirmation | Order service |
| ConfirmReturnRequest | Return confirmation | Return/webhook service |
| CreateAttributesBatchRequest | Batch variant creation by product ID | Attribute controller |
| CreateAttributesRequest | Variant creation by product SKU | Attribute controller |
| CreateOrderRequest | Order creation (direct or cart-based) | Order controller |
| CreateProductRequest | Product creation | Product controller |
| DelayOrderRequest | Delivery delay recording | Webhook handler |
| DeliverOrderRequest | Delivery confirmation | Webhook handler |
| GetProductRequest | Advanced product search | Product service |
| OrderSearchRequest | Order search/filtering | Order service |
| PagingRequest | Pagination utility | All search requests |
| PaymentCallbackRequest | Payment provider webhook | Payment webhook handler |

---

## Configuration & Constants

**No explicit constants or environment variables** are defined within request DTOs; they delegate to:
- Model enums: `PaymentMethod`, `OrderStatus`, `StockStatus`
- Embedded model classes: `VariantOption`, `Promotion`, `SpecificationGroup`
- Jakarta Validation constraints for field-level business rules

Default pagination: **page=1, size=10** (PagingRequest).