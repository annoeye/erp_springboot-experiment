# Request DTO Module — Group request_1

## Overview

This group contains 20 request Data Transfer Objects (DTOs) for the ERP Spring Boot application. These classes standardize HTTP request payloads for order operations, authentication, product/category management, user operations, and profile updates. Most order-related DTOs share an identical structure, while authentication and resource management DTOs have specialized fields with validation constraints.

---

## File Summaries

### 1. PickupOrderRequest.java
**Purpose:** Captures data for pickup order operations. Aggregates order metadata, shipper information, delivery timelines, and financial details into a single request object.

**Fields:**
- `orderId`, `orderNumber`, `transactionId`: Order identifiers
- `shipperId`, `shipperName`, `shipperPhone`: Shipper details
- `estimatedDeliveryDate`, `actualDeliveryDate`, `pickupDeadline`: Temporal markers
- `recipientName`, `condition`: Recipient and item state
- `refundAmount`, `amount`, `paymentMethod`: Financial data
- `note`, `reason`, `status`: Metadata
- `rawData`: Generic map for extensibility

**Annotation:** `@Data` (Lombok) — auto-generates getters, setters, equals, hashCode, toString.

---

### 2. ProcessOrderRequest.java
**Purpose:** Identical structure to PickupOrderRequest; handles order processing state transitions. Contains the same 18 fields for uniform request handling across workflow stages.

**Key fields:** Same as PickupOrderRequest.

---

### 3. ReadyForPickupRequest.java
**Purpose:** Signals order readiness for pickup. Reuses the standard order request schema (18 fields) to maintain consistency across state change operations.

**Key fields:** Same as PickupOrderRequest.

---

### 4. RefreshTokenRequest.java
**Purpose:** Authenticates token refresh operations. Validates refresh token presence and device context.

**Fields:**
- `refreshToken` (String, @NotBlank): The refresh token; validation message: "Refresh token không được để trống"
- `deviceInfo` (DeviceInfo, @NotNull, @Valid): Embedded device information; validation message: "Thông tin thiết bị không được để trống"

**Annotations:** `@FieldDefaults(level = AccessLevel.PRIVATE)`, `@Getter` — fields are private with package-scoped getters.

**Dependencies:** Imports `DeviceInfo` from `com.anno.ERP_SpringBoot_Experiment.model.embedded`.

---

### 5. RefundOrderRequest.java
**Purpose:** Manages refund request submission. Uses the standard order payload structure (18 fields shared with PickupOrderRequest, ProcessOrderRequest, etc.).

**Key fields:** Same as PickupOrderRequest.

---

### 6. ReturnOrderRequest.java
**Purpose:** Processes return order workflows. Identical schema to RefundOrderRequest and other order operations.

**Key fields:** Same as PickupOrderRequest.

---

### 7. ShipOrderRequest.java
**Purpose:** Initiates or updates shipping for orders. Maintains consistency with other order DTOs (18 shared fields).

**Key fields:** Same as PickupOrderRequest.

---

### 8. TransitionOrderRequest.java
**Purpose:** Generic order state transition trigger. Minimal, targeted fields for workflow progression.

**Fields:**
- `orderId` (String): Target order identifier
- `targetStatus` (OrderStatus enum): Current/source status
- `newStatus` (OrderStatus enum): Destination status
- `note` (String): Transition reason/notes
- `shipperId` (String): Actor identifier

**Annotation:** `@Data` (Lombok).

**Dependencies:** Imports `OrderStatus` enum from `com.anno.ERP_SpringBoot_Experiment.model.enums`.

---

### 9. UpdateAdminNotesRequest.java
**Purpose:** Allows admins to append or update notes on orders. Minimal focused schema.

**Fields:**
- `orderId` (String): Target order
- `adminNotes` (String): Admin annotation
- `notes` (String): Generic note field (potential redundancy with adminNotes)

**Annotation:** `@Data`.

---

### 10. UpdateAttributesRequest.java
**Purpose:** Updates product variant attributes (sizes, colors, prices, SEO keywords, specifications, promotions). Comprehensive validation for e-commerce product variants.

**Key fields:**
- `id` (String, @NormalizedId, @NotNull): Variant identifier; normalized automatically
- `name` (String, @Size(max=255)): Variant display name
- `price` (Double, @Positive): Base price
- `salePrice` (Double, @Positive): Promotional price
- `variantOptions` (List<VariantOptionDto>, @Valid): Key-value pairs (e.g., Color: Black, Size: L)
- `status` (StockStatus enum): Product stock status
- `keywords` (Set<String>, @Size(max=20)): SEO keywords; each ≤50 chars
- `specifications` (List<SpecificationGroupDto>, @Size(max=50), @Valid): Product specs
- `promotions` (List<PromotionDto>, @Size(max=10), @Valid): Active promotions

**Annotation:** `@FieldDefaults(level = AccessLevel.PRIVATE)`, `@Data`.

**Dependencies:** `NormalizedId` annotation (custom), `StockStatus`, `PromotionDto`, `SpecificationGroupDto`, `VariantOptionDto`.

**Validation Pattern:** Comprehensive nested validation using `@Valid` and Jakarta annotations; error messages in Vietnamese.

---

### 11. UpdateCategoryRequest.java
**Purpose:** Updates product category metadata (name, description).

**Fields:**
- `id` (String, @NotBlank): Category identifier
- `name` (String): New category name
- `description` (String): Category description

**Annotation:** `@Data`.

---

### 12. UpdateDeliveryRequest.java
**Purpose:** Modifies delivery metadata for orders after shipping.

**Fields:**
- `orderId` (String): Target order
- `estimatedDeliveryDate` (LocalDateTime): Updated expected delivery
- `actualDeliveryDate` (LocalDateTime): Recorded actual delivery
- `deliveryInfo` (String): Delivery notes/tracking info

**Annotation:** `@Data`.

---

### 13. UpdateOrderRequest.java
**Purpose:** Comprehensive order update handler. Supports partial updates to order state, shipping address, admin notes, and tracking.

**Fields:**
- `orderId` (String, @NormalizedId, @NotNull): Order ID; auto-normalized (uppercase, remove dashes)
- `status` (OrderStatus enum): New order status
- `shippingInfo` (Address entity): Updated shipping address
- `adminNotes` (String): Admin-facing annotations
- `trackingNumber` (String): Carrier tracking reference

**Annotations:** `@Data`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor`, `@FieldDefaults(level = AccessLevel.PRIVATE)`.

**Dependencies:** `Address` entity, `OrderStatus` enum, `NormalizedId` annotation.

---

### 14. UpdateProductRequest.java
**Purpose:** Updates core product attributes: name, category, active status, and discount settings.

**Fields:**
- `id` (String, @NormalizedId, @NotNull): Product ID; auto-normalized
- `name` (String): Product display name
- `categoryId` (String, @NormalizedId): Parent category ID; normalized
- `status` (ActiveStatus enum): Publication/active status
- `discountPercent` (Double): Discount percentage (optional)
- `discountStartDate` (LocalDateTime): Discount effective start
- `discountEndDate` (LocalDateTime): Discount expiration

**Annotations:** `@Data`, `@FieldDefaults(level = AccessLevel.PRIVATE)`.

**Dependencies:** `ActiveStatus` enum, `NormalizedId` annotation.

---

### 15. UpdateProfileRequest.java
**Purpose:** Allows users to update personal profile information with strict validation patterns.

**Fields:**
- `fullName` (String, @Pattern): Letters and spaces only; regex `^[\\p{L}\\s]*$`
- `phoneNumber` (String, @Pattern): 10 digits or empty; regex `^\\d{10}$|^$`
- `dateOfBirth` (Date): User birth date
- `gender` (Gender enum): User gender
- `avatarUrl` (String): Profile image URL

**Annotations:** `@Getter`, `@Setter`, `@FieldDefaults(level = AccessLevel.PRIVATE)`.

**Dependencies:** `Gender` enum.

**Validation Pattern:** Regex-based field validation with Vietnamese error messages.

---

### 16. UpdateShippingRequest.java
**Purpose:** Updates shipping method and associated info for an order.

**Fields:**
- `orderId` (String): Target order
- `shippingMethod` (String): Courier or shipping service type
- `shippingInfo` (String): Tracking number or delivery notes

**Annotation:** `@Data`.

---

### 17. UserLoginRequest.java
**Purpose:** Authenticates user credentials and captures device context for login sessions.

**Fields:**
- `usernameOrEmail` (String, @NotBlank, @Size(min=3, max=50)): Login identifier; 3–50 characters
- `password` (String, @NotBlank): User password; no length validation
- `deviceInfo` (DeviceInfo, @NotNull, @Valid): Embedded device metadata

**Annotations:** `@FieldDefaults(level = AccessLevel.PRIVATE)`, `@Getter`.

**Dependencies:** `DeviceInfo` from `com.anno.ERP_SpringBoot_Experiment.model.embedded`.

**Validation:** Comprehensive; error messages in Vietnamese.

---

### 18. UserRegisterRequest.java
**Purpose:** Captures user registration data with password confirmation and email validation.

**Fields:**
- `fullName` (String, @NotBlank): User full name
- `name` (String, @NotBlank, @Size(min=3, max=50)): Username; 3–50 characters
- `email` (String, @NotBlank, @Email): Email address; standard email format
- `password` (String, @NotBlank, @Size(min=6)): Password; minimum 6 characters
- `confirmPassword` (String, @NotBlank): Password confirmation (note: no explicit equality validation in DTO; likely enforced in service layer)

**Annotations:** `@Getter`, `@FieldDefaults(level = AccessLevel.PRIVATE)`.

**Validation:** Jakarta constraints; error messages in Vietnamese.

---

### 19. UserSearchRequest.java
**Purpose:** Query filter for user search operations. Flexible, multi-field lookup criteria.

**Fields:**
- `fullName` (String): Search by user full name
- `email` (String): Search by email address
- `numberPhone` (String): Search by phone number

**Annotations:** `@Data`, `@FieldDefaults(level = AccessLevel.PRIVATE)`.

---

### 20. VariantGroupInput.java
**Purpose:** Input for creating/updating product variant groups (e.g., "Color", "Size").

**Fields:**
- `name` (String): Variant group name

**Annotation:** `@Data`.

---

## Cross-File Patterns & Data Flow

### Shared Order DTO Pattern
Seven classes (PickupOrderRequest, ProcessOrderRequest, ReadyForPickupRequest, RefundOrderRequest, ReturnOrderRequest, ShipOrderRequest) share an identical 18-field structure:
```
orderId, note, reason, shipperId, shipperName, shipperPhone,
estimatedDeliveryDate, actualDeliveryDate, pickupDeadline,
recipientName, condition, refundAmount, orderNumber, transactionId,
status, paymentMethod, amount, rawData
```
This suggests a generic order event/command pattern where state transitions reuse the same payload schema.

### Authentication Flow
- **UserRegisterRequest** → User account creation (email + password validation)
- **UserLoginRequest** → Session initiation (credentials + device context)
- **RefreshTokenRequest** → Token renewal (refresh token + device validation)

Device context (DeviceInfo) is captured in login/refresh operations, likely for session tracking and multi-device management.

### Product Management Flow
- **UpdateProductRequest** → Core product metadata (name, category, active status, discounts)
- **UpdateAttributesRequest** → Variant-level customization (prices, specifications, promotions, SEO)
- **UpdateCategoryRequest** → Category hierarchy updates
- **VariantGroupInput** → Variant group definitions

### Order Fulfillment Flow
- **TransitionOrderRequest** → Generic state machine trigger
- **UpdateOrderRequest** → Comprehensive order metadata update
- **UpdateDeliveryRequest** → Post-shipment delivery tracking
- **UpdateShippingRequest** → Shipping method changes
- **UpdateAdminNotesRequest** → Admin annotations

---

## Dependencies & Imports

**Common External Dependencies:**
- `jakarta.validation.*` — Bean validation (NotBlank, NotNull, Email, Pattern, Size, Positive, Valid)
- `lombok.*` — Code generation (Data, Getter, Setter, Builder, AllArgsConstructor, NoArgsConstructor, FieldDefaults, AccessLevel)
- `com.fasterxml.jackson.annotation.JsonProperty` — JSON serialization hints (in UpdateAttributesRequest)

**Internal Dependencies:**
- `com.anno.ERP_SpringBoot_Experiment.common.annotation.NormalizedId` — Custom annotation for ID normalization (uppercase, dash removal)
- `com.anno.ERP_SpringBoot_Experiment.model.enums.*` — OrderStatus, StockStatus, ActiveStatus, Gender
- `com.anno.ERP_SpringBoot_Experiment.model.embedded.DeviceInfo` — Device context for authentication
- `com.anno.ERP_SpringBoot_Experiment.model.entity.Address` — Shipping address entity
- `com.anno.ERP_SpringBoot_Experiment.service.dto.*` — Nested DTOs: PromotionDto, SpecificationGroupDto, VariantOptionDto

---

## Public API & Design Patterns

### Exposure
All classes are public DTOs exposed via REST endpoints. They serve as:
- **Request deserialization targets** for Spring MVC `@RequestBody` parameters
- **Validation constraints enforcement** via Jakarta Validation
- **Type-safe data carriers** between HTTP layer and service/business logic

### Design Patterns

1. **Data Transfer Object (DTO)** — Standard pattern; separates API contract from domain models
2. **Builder Pattern** — Used in UpdateOrderRequest for flexible object construction
3. **Fluent Validation** — Jakarta annotations declare validation rules declaratively
4. **Normalized ID Pattern** — Custom @NormalizedId annotation applies automatic ID normalization (likely via AOP or request interceptor)
5. **Generic Event Schema** — Seven order DTOs reuse identical structure for polymorphic state transitions
6. **Extensibility via rawData Map** — Order DTOs include `Map<String, Object> rawData` for forward compatibility

---

## Configuration & Constants

**Validation Constants (Vietnamese):**
- "Refresh token không được để trống" — Refresh token required
- "Thông tin thiết bị không được để trống" — Device info required
- "Tên đăng nhập hoặc email không được để trống" — Username/email required
- "Mật khẩu không được để trống" — Password required
- "Email không đúng định dạng" — Invalid email format
- "Mật khẩu phải có ít nhất 6 ký tự" — Password minimum 6 characters

**Field Limits:**
- Username: 3–50 chars
- Category name: 255 chars
- SEO keywords: max 20, each ≤50 chars
- Specifications: max 50
- Promotions: max 10
- Phone: exactly 10 digits
- Discount dates: LocalDateTime (no range validation in DTO)

**Enums Used:**
- OrderStatus, StockStatus, ActiveStatus, Gender — Domain enumerations