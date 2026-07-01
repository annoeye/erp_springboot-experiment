# DTO Group Knowledge Document

## Overview

The **dto** group comprises 20 data transfer objects that facilitate communication between layers in an ERP Spring Boot application. These DTOs model core business entities—products, orders, addresses, payments, and analytics—enabling structured data exchange between controllers, services, and clients. The group follows a layered architecture pattern with embedded objects, enums, and audit trails supporting e-commerce operations.

---

## File-by-File Analysis

### ActiveLogDto
**Purpose**: Captures activity logs for user actions on system entities.

**Key Fields**:
- `performedBy` (String) — user identifier
- `targetID` (List<String>) — affected entity IDs
- `description` (String) — action description
- `createdAt` (LocalDateTime, formatted `yyyy-MM-dd HH:mm:ss`) — timestamp
- `status` (ActiveStatus enum) — action status

**Annotations**: Uses Lombok (`@Builder`, `@Getter`, `@Setter`, `@FieldDefaults`) for boilerplate reduction and Jackson `@JsonFormat` for date serialization.

**Public API**: Exposes activity metadata for audit trails and event logging.

---

### AddressDto
**Purpose**: DTO for {@link Address} entity; represents shipping/billing addresses.

**Key Fields**:
- `id` (Long) — address identifier
- `name`, `address`, `phoneNumber`, `recipientName` (String) — address details
- `user` (UserDto, @NotNull) — associated user reference

**Key Methods**:
- `toEntity()` — converts DTO to Address entity, excluding id and user (line 32-37)

**Design Pattern**: Implements conversion method for entity-to-DTO mapping.

**Public API**: Provides bidirectional DTO-to-entity conversion for address operations.

---

### AttributesDto
**Purpose**: DTO for {@link Attributes}; represents product variant attributes with pricing and inventory.

**Key Fields**:
- `id`, `name`, `price`, `salePrice` — variant identity and pricing
- `sku` (SkuInfoDto) — stock-keeping unit reference
- `variantOptions` (List<VariantOptionDto>) — variant selections
- `statusProduct` (StockStatus enum) — inventory status (IN_STOCK, OUT_OF_STOCK, LOW_STOCK)
- `specifications` (List<SpecificationGroupDto>) — product specifications
- `promotions` (List<PromotionDto>) — active promotions
- `keywords` (Set<String>) — search keywords
- `auditInfo` (AuditInfoDto) — audit metadata
- `product` (ProductDto) — parent product reference

**Dependencies**: Links to SkuInfoDto, VariantOptionDto, SpecificationGroupDto, PromotionDto, AuditInfoDto, ProductDto.

**Public API**: Exposes complete product variant data including pricing, inventory, and audit trail.

---

### AuditInfoDto
**Purpose**: DTO for {@link com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo}; tracks creation, modification, and deletion metadata.

**Key Fields**:
- `createdAt`, `createdBy` (LocalDateTime, String) — creation timestamp and user
- `updatedAt`, `updateHistory` (LocalDateTime, List<AuditEntry>) — last update and change history
- `deletedAt`, `deletedBy` (LocalDateTime, String) — soft delete metadata

**Immutability**: Uses Lombok `@Value` for immutable record-like behavior.

**Public API**: Provides comprehensive audit trail for compliance and change tracking.

---

### CategoryDto
**Purpose**: DTO for {@link Category}; represents product categories with inventory summary.

**Key Fields**:
- `id`, `name` (Long, String) — category identity
- `skuInfo` (SkuInfoDto) — SKU reference
- `productCount` (Long) — product count in category

**Public API**: Exposes category metadata with product aggregation.

---

### GetProductRequest
**Purpose**: Request DTO for product search/filtering operations.

**Key Fields**:
- `name`, `description` (String) — search filters

**Design Pattern**: Simple filter/query object pattern.

**Public API**: Request contract for product retrieval endpoints.

---

### MediaItemDto
**Purpose**: DTO for {@link com.anno.ERP_SpringBoot_Experiment.model.embedded.MediaItem}; represents media (images/videos) associated with products.

**Key Fields**:
- `key` (String, @Size(max=5)) — media identifier/type (max 5 chars)
- `url` (String) — media URL

**Public API**: Embeddable media reference for product display.

---

### OrderDto
**Purpose**: DTO for Order entity; comprehensive order representation with customer, items, pricing, and shipping.

**Key Fields**:
- `id`, `orderNumber` (Long, String) — order identity
- `status`, `currentStatus` (List<OrderStatus>, OrderStatus) — status history and current state
- `customerId`, `customerName`, `customerEmail`, `customerPhone` (Long, String×3) — customer data
- `orderItems` (List<OrderItemDto>) — line items
- `subtotal`, `discountAmount`, `taxAmount`, `shippingFee`, `totalAmount` (Double) — pricing breakdown
- `shippingInfo` (Address) — shipping address entity
- `customerNotes`, `adminNotes`, `cancellationReason` (String) — annotations
- `cancelledAt`, `cancelledBy`, `confirmedAt`, `confirmedBy`, `completedAt` (LocalDateTime, String) — status timeline
- `shoppingCartId` (String) — cart reference
- `auditInfo` (AuditInfoDto) — audit metadata

**Data Flow**: Aggregates order items, shipping, and audit info; references shopping cart.

**Public API**: Complete order data for checkout, fulfillment, and reporting workflows.

---

### OrderItemDto
**Purpose**: DTO for individual line items within orders.

**Key Fields**:
- `id`, `orderId`, `productId`, `attributesId` (Long) — identifiers
- `productName`, `productSku`, `attributesSku` (String) — product variant info
- `variantOptions` (List<VariantOptionDto>) — variant selections
- `quantity`, `unitPrice`, `salePrice` (Integer, Double) — quantity and pricing
- `discountAmount`, `discountPercentage`, `subtotal`, `taxAmount` (Double) — calculations
- `notes`, `imageUrl` (String) — metadata

**Data Flow**: Breaks down OrderDto pricing; references AttributesDto and VariantOptionDto.

**Public API**: Line-item-level order details for invoicing and analytics.

---

### PaymentDto
**Purpose**: DTO for {@link Payment}; payment transaction details.

**Key Fields**:
- `id` (UUID) — payment identifier
- `name`, `order` (String, OrderDto) — payment name and associated order
- `provider`, `transactionCode` (String) — payment gateway and reference
- `amount`, `status` (Double, String) — transaction amount and status
- `paymentDate` (LocalDateTime) — transaction timestamp
- `description`, `rawResponse` (String) — notes and gateway response
- `bankCode`, `cardType`, `bankTranNo`, `ipAddress` (String) — payment method details

**Dependencies**: References OrderDto.

**Public API**: Payment transaction record for reconciliation and audit.

---

### ProductAnalyticsDto
**Purpose**: DTO for product analytics dashboard; aggregated metrics for product performance analysis.

**Key Fields** (organized by section):

**Basic Info**:
- `productId`, `productSku`, `productName` (Long, String, String)

**Sales Metrics**:
- `totalSoldQuantity`, `totalOrders` (Integer) — units and transactions
- `totalRevenue`, `netRevenue`, `averageOrderValue` (Double) — monetary metrics

**Inventory Metrics**:
- `currentStock` (Integer) — on-hand quantity
- `sellThroughRate` (Double %) — velocity ratio
- `stockStatus` (String) — IN_STOCK, LOW_STOCK, OUT_OF_STOCK

**Performance Metrics**:
- `returnRate`, `cancellationRate`, `conversionRate`, `profitMargin` (Double %) — operational KPIs

**Engagement Metrics**:
- `viewCount`, `reviewCount` (Integer) — customer interaction
- `averageRating` (Double) — quality score

**Time-based Metrics**:
- `revenueToday`, `revenueThisWeek`, `revenueThisMonth` (Double) — period revenue
- `revenueChangePercent` (Double %) — period-over-period growth

**Design Pattern**: Value object pattern with builder for immutable analytics snapshots.

**Public API**: Dashboard and reporting data for product performance evaluation.

---

### ProductCachingDto
**Purpose**: Container for cached product recommendation lists with strategy metadata.

**Key Fields**:
- `recommendationId`, `strategy` (String) — recommendation set identifier and algorithm
- `generatedAt` (long) — cache generation timestamp (milliseconds)
- `items` (List<ProductDto>) — recommended product list

**Design Pattern**: Cache envelope pattern.

**Public API**: Cached recommendation sets for performance optimization.

---

### ProductDetailsDto
**Purpose**: Simple DTO for product inventory details (SKU and quantity pairs).

**Key Fields**:
- `sku` (String) — stock-keeping unit
- `quantity` (int) — stock quantity

**Public API**: Minimal product inventory representation.

---

### ProductDto
**Purpose**: DTO for {@link Product}; core product representation with metadata and aggregations.

**Key Fields**:
- `id`, `name` (Long, String) — product identity
- `skuInfo` (SkuInfoDto) — SKU reference
- `mediaItems` (List<MediaItemDto>) — associated media
- `status` (ActiveStatus enum) — product status
- `viewCount`, `totalSoldQuantity` (Integer) — engagement and sales volume
- `totalRevenue` (BigDecimal) — revenue aggregate
- `discountPercent`, `discountStartDate`, `discountEndDate` (Double, LocalDateTime) — promotion window
- `categoryName` (String) — category snapshot

**Data Flow**: Aggregates media, SKU, and category info; references in AttributesDto and ProductCachingDto.

**Public API**: Primary product catalog entry for storefront and search operations.

---

### ProductQuantityDto
**Purpose**: DTO for {@link com.anno.ERP_SpringBoot_Experiment.model.embedded.ProductQuantity}; minimal SKU-quantity pair.

**Key Fields**:
- `sku` (String) — product SKU
- `quantity` (int) — quantity value

**Immutability**: Uses Lombok `@Value` for immutable record behavior.

**Public API**: Lightweight inventory data for stock adjustments and calculations.

---

### PromotionDto
**Purpose**: DTO for promotional/discount information attached to product variants.

**Key Fields**:
- `name` (String) — promotion name
- `discountPercent` (Double) — discount rate
- `startDate`, `endDate` (LocalDateTime) — promotion validity window

**Public API**: Promotion metadata for pricing calculations and display.

---

### ShoppingCartDto
**Purpose**: DTO for {@link com.anno.ERP_SpringBoot_Experiment.model.entity.ShoppingCart}; user's shopping cart with items and totals.

**Key Fields**:
- `id`, `name` (Long, String) — cart identity
- `items` (List<CartItemDto>) — line items
- `totalItems`, `totalPrice`, `totalSalePrice`, `totalDiscount` (Integer, Double) — aggregated cart metrics

**Nested Class**:
- `CartItemDto(sku: String, quantity: Integer)` — minimal cart line item

**Immutability**: Uses Lombok `@Value` for record-like behavior.

**Public API**: Shopping cart state for checkout workflow.

---

### SkuInfoDto
**Purpose**: DTO for {@link com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo}; stock-keeping unit identifier.

**Key Fields**:
- `sku` (String, @JsonProperty("sku")) — SKU code

**Design Pattern**: Simple value object for SKU abstraction and reuse across DTOs.

**Public API**: Standardized SKU reference used in ProductDto, AttributesDto, CategoryDto, ProductQuantityDto, and ShoppingCartDto.CartItemDto.

---

### SpecificationDto
**Purpose**: DTO for {@link Specificationa} (note: entity class appears misspelled); single product specification key-value pair.

**Key Fields**:
- `key` (String) — specification type (e.g., "Color")
- `data` (String, @JsonAlias("value")) — specification value; accepts "value" in JSON for backward compatibility

**Public API**: Attribute specification for variant identification and filtering.

---

### SpecificationGroupDto
**Purpose**: Container grouping related specifications for organized product attribute display.

**Key Fields**:
- `groupName` (String) — specification group category (e.g., "Dimensions")
- `specifications` (List<SpecificationDto>) — grouped specification items

**Data Flow**: Aggregates SpecificationDto; referenced in AttributesDto.

**Public API**: Hierarchical specification structure for product detail rendering.

---

## Cross-DTO Data Flow

```
ProductDto
  ├─→ SkuInfoDto (product SKU)
  ├─→ List<MediaItemDto> (product images/videos)
  └─→ categoryName (snapshot)

OrderDto
  ├─→ List<OrderItemDto> (line items)
  │   ├─→ VariantOptionDto (variant selections)
  │   └─→ attributesSku, productSku
  ├─→ Address (shipping info)
  └─→ AuditInfoDto (audit trail)

AttributesDto (product variant)
  ├─→ ProductDto (parent product)
  ├─→ SkuInfoDto (variant SKU)
  ├─→ List<VariantOptionDto> (variant options)
  ├─→ List<SpecificationGroupDto> (detailed specs)
  ├─→ List<PromotionDto> (active promotions)
  └─→ AuditInfoDto (audit info)

SpecificationGroupDto
  └─→ List<SpecificationDto> (individual specs)

ShoppingCartDto
  └─→ List<CartItemDto> (SKU + quantity pairs)

ProductAnalyticsDto (aggregated metrics)
  └─→ productId, productSku, productName (references)

PaymentDto
  └─→ OrderDto (associated order)

AddressDto
  └─→ UserDto (address owner)
```

---

## Design Patterns

| Pattern | Examples |
|---------|----------|
| **Value Object** | AuditInfoDto, ProductQuantityDto, SkuInfoDto (immutable `@Value`) |
| **Builder** | ProductDto, OrderDto, AttributesDto, SkuInfoDto (fluent construction) |
| **DTO Composition** | OrderDto aggregates OrderItemDto; AttributesDto aggregates PromotionDto, SpecificationGroupDto |
| **Entity-to-DTO Conversion** | AddressDto.toEntity() (line 32-37) for reverse mapping |
| **Enum Integration** | ActiveStatus, OrderStatus, StockStatus for domain constraints |
| **JSON Serialization** | @JsonFormat, @JsonProperty, @JsonAlias for API contract flexibility |
| **Validation** | @NotNull, @Size decorators for input constraints |
| **Cache Envelope** | ProductCachingDto wraps List<ProductDto> with metadata |

---

## External Dependencies

| Dependency | Usage |
|------------|-------|
| **Lombok** | `@Data`, `@Builder`, `@Getter`, `@Setter`, `@Value`, `@FieldDefaults`, `@AllArgsConstructor`, `@NoArgsConstructor` (boilerplate elimination) |
| **Jackson** | `@JsonFormat`, `@JsonProperty`, `@JsonAlias` (JSON serialization control) |
| **Jakarta Validation** | `@NotNull`, `@Size` (JSR-380 validation annotations) |
| **Enums** | ActiveStatus, OrderStatus, StockStatus (from model.enums) |
| **Embedded Objects** | AuditEntry, AuditInfo, SkuInfo, Specificationa, ProductQuantity, MediaItem, ShoppingCart (from model.embedded/entity) |

---

## Configuration & Constants

- **JSON Date Format**: `yyyy-MM-dd HH:mm:ss` (ProductDto, ActiveLogDto via @JsonFormat)
- **Serialization**: `implements Serializable` for all major DTOs (AddressDto, AttributesDto, AuditInfoDto, etc.)
- **Stock Status Values**: IN_STOCK, OUT_OF_STOCK, LOW_STOCK (AttributesDto.statusProduct)
- **FieldDefaults Access Level**: PRIVATE (ActiveLogDto, OrderDto, OrderItemDto, GetProductRequest)

---

## Public API Summary

The dto group exposes:
- **Product Domain**: ProductDto, AttributesDto, SkuInfoDto, SpecificationDto, SpecificationGroupDto, PromotionDto, MediaItemDto, ProductAnalyticsDto
- **Order Domain**: OrderDto, OrderItemDto, ShoppingCartDto
- **Customer Domain**: AddressDto, UserDto (reference)
- **Payment Domain**: PaymentDto
- **Audit Domain**: AuditInfoDto, ActiveLogDto
- **Search/Filtering**: GetProductRequest, ProductDetailsDto, ProductQuantityDto
- **Caching**: ProductCachingDto

All DTOs support client-server serialization and are composable into higher-order requests/responses.