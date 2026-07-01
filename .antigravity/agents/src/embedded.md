# Embedded Module Knowledge Document

## Overview

The **embedded** group contains 13 lightweight JPA embeddable value objects and data transfer classes designed to be composed into larger domain entities. These classes encapsulate cross-cutting concerns like audit trails, payment processing, device tracking, product specifications, and promotional data. They follow the embedded entity pattern, allowing composite data structures to be stored within parent entities as denormalized columns.

---

## AuditEntry

**Purpose:** Immutable record of a single audit event—action performed, actor, timestamp.

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/model/embedded/AuditEntry.java`

**Key Fields:**
- `action` (String) — what was done
- `performBy` (String) — original actor
- `updatedBy` (String) — who made the update
- `timestamp` (LocalDateTime) — when the action occurred
- `updatedAt` (LocalDateTime) — update timestamp
- `details` (String) — additional context

**Annotations:** `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` (Lombok).

**Design Pattern:** Value object with builder pattern for construction.

---

## AuditInfo

**Purpose:** Embeddable audit metadata container tracking creation, updates, soft deletes, and full update history. Designed for `IdentityOnly` entities (Order, ShoppingCart); `BaseEntity` entities manage their own audit fields.

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/model/embedded/AuditInfo.java`

**Key Fields:**
- `createdAt` (LocalDateTime) — readonly, set by `@CreatedDate`
- `createdBy` (String) — readonly, set by `@CreatedBy`
- `updatedAt` (LocalDateTime) — latest modification time
- `updateHistory` (List<AuditEntry>) — persisted as CLOB via `AuditEntryListConverter`
- `deletedAt` (LocalDateTime) — soft delete timestamp
- `deletedBy` (String) — who deleted

**Key Methods:**
- `addUpdateEntry(String action, String updatedBy)` — appends audit entry, updates `updatedAt`
- `getLatestUpdate()` — returns most recent audit entry or null
- `markDeletedAfter30Days(String deletedByUser)` — schedules deletion 30 days ahead
- `markDeletedNow(String deletedByUser)` — immediate soft delete
- `restore()` — clears deletion fields
- `isDeleted()` — boolean check on `deletedAt != null`

**Annotations:** `@Embeddable`, `@EntityListeners(AuditingEntityListener.class)`, `@Convert(converter = AuditEntryListConverter.class)`.

**Dependencies:** 
- `AuditEntryListConverter` (from `com.anno.ERP_SpringBoot_Experiment.config.converter`) — converts `List<AuditEntry>` to/from CLOB.
- Spring Data JPA auditing (`@CreatedDate`, `@CreatedBy`, `AuditingEntityListener`).

**Design Pattern:** Embeddable aggregate value object with temporal soft-delete and history tracking.

---

## AuthCode

**Purpose:** Embeddable credential code container holding authentication/authorization codes with expiry and purpose classification.

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/model/embedded/AuthCode.java`

**Key Fields:**
- `code` (String) — the authentication code
- `purpose` (ActiveStatus enum) — classification (e.g., email verification, password reset)
- `expiryDate` (LocalDateTime) — when code expires

**Annotations:** `@Embeddable`, `@Data`, `@Enumerated(EnumType.STRING)`.

**Dependencies:** `ActiveStatus` enum from `com.anno.ERP_SpringBoot_Experiment.model.enums`.

---

## DeviceInfo

**Purpose:** Captures client device and environment metadata—OS, browser, screen, IP, timezone, device ID—for session tracking and security audits.

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/model/embedded/DeviceInfo.java`

**Key Fields:**
- `deviceType`, `osName`, `osVersion` (String) — platform info
- `browserName`, `browserVersion` (String) — browser specifics
- `screenWidth`, `screenHeight` (Integer) — display resolution
- `userAgent` (String) — raw user-agent string
- `ipAddress` (String) — client IP
- `language`, `timeZone` (String) — locale
- `deviceId` (String) — unique device identifier

**Key Constructor:**
- Copy constructor `DeviceInfo(DeviceInfo other)` — defensive clone of all fields.

**Annotations:** `@Embeddable`, `@Data`.

**Design Pattern:** Data transfer object with shallow copy constructor.

---

## MediaItem

**Purpose:** Key-value pair for storing media references (images, videos, etc.) with URL and identifier.

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/model/embedded/MediaItem.java`

**Key Fields:**
- `key` (String) — identifier/name
- `url` (String) — resource URL

**Annotations:** `@Embeddable`, `@Data`, `@Builder`.

---

## PaymentInfo

**Purpose:** Embeddable payment transaction record tracking method, status, amounts, gateway, and refunds.

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/model/embedded/PaymentInfo.java`

**Key Fields:**
- `paymentMethod` (PaymentMethod enum) — payment type
- `paymentStatus` (PaymentStatus enum) — processing state
- `paymentDate` (LocalDateTime) — transaction timestamp
- `transactionId` (String, max 200) — unique transaction ref
- `paymentGateway` (String, max 100) — processor (VNPay, MoMo, etc.)
- `paidAmount` (Double) — amount charged
- `refundAmount` (Double) — amount refunded
- `refundDate` (LocalDateTime) — refund timestamp
- `notes` (String, max 1000) — payment notes

**Annotations:** `@Embeddable`, `@Data`, `@Builder`, `@Enumerated(EnumType.STRING)`.

**Dependencies:** 
- `PaymentMethod` enum
- `PaymentStatus` enum

---

## ProductQuantity

**Purpose:** Simple SKU-quantity pair for inventory or order line items.

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/model/embedded/ProductQuantity.java`

**Key Fields:**
- `sku` (String) — product SKU
- `quantity` (int) — quantity value

**Annotations:** `@Embeddable`, `@Data`.

---

## Promotion

**Purpose:** Time-bounded promotional offer with discount percentage.

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/model/embedded/Promotion.java`

**Key Fields:**
- `name` (String) — promotion title
- `discountPercent` (Double) — discount percentage
- `startDate`, `endDate` (LocalDateTime) — validity window

**Annotations:** `@Data`, `@Builder`.

---

## SkuInfo

**Purpose:** Encapsulates SKU generation logic. Creates unique SKU strings by appending random 4-digit suffix to lowercased name.

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/model/embedded/SkuInfo.java`

**Key Fields:**
- `sku` (String) — the generated SKU value

**Key Methods:**
- `createSku(String name)` — generates SKU as `name.toLowerCase() + random(1000–9999)`, returns new `SkuInfo` instance.

**Annotations:** `@Embeddable`, `@Data`, `@Builder`.

**Dependencies:** `ThreadLocalRandom` for thread-safe randomization; `@NonNull` annotation from `org.jspecify`.

---

## Specification

**Purpose:** Generic key-value pair for storing individual product/entity specifications.

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/model/embedded/Specification.java`

**Key Fields:**
- `key` (String) — spec name/identifier
- `data` (String) — spec value

**Annotations:** `@Embeddable`, `@Data`.

---

## SpecificationGroup

**Purpose:** Named grouping of multiple specifications (e.g., "Dimensions", "Color Options").

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/model/embedded/SpecificationGroup.java`

**Key Fields:**
- `groupName` (String) — group label
- `specifications` (List<Specificationa>) — list of specs (note: typo in class name `Specificationa`).

**Annotations:** `@Data`, `@Builder`.

---

## Specificationa

**Purpose:** Named specification key-value pair. Appears to be duplicate/alternative to `Specification` (note class name typo: `Specificationa` vs `Specification`).

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/model/embedded/Specificationa.java`

**Key Fields:**
- `name` (String) — specification name
- `value` (String) — specification value

**Annotations:** `@Data`, `@Builder`.

**Design Pattern:** Redundant with `Specification`; likely a refactoring artifact.

---

## VariantOption

**Purpose:** Product variant dimension (e.g., "Color", "Size") with list of available values.

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/model/embedded/VariantOption.java`

**Key Fields:**
- `name` (String) — variant dimension name
- `values` (List<String>) — available choices

**Annotations:** `@Data`, `@Builder`.

---

## Data Flow & Composition

- **AuditInfo** embeds **AuditEntry** list, converted via `AuditEntryListConverter`.
- **SpecificationGroup** embeds **Specificationa** list.
- **PaymentInfo**, **DeviceInfo**, **AuthCode**, **MediaItem**, **ProductQuantity**, **SkuInfo**, **Promotion**, **Specification**, **VariantOption** are all standalone embeddables.
- Parent entities (Order, ShoppingCart, User, Product) compose these objects via `@Embedded` annotation.

---

## Public API Summary

**Embeddable Classes (JPA-managed):**
- `AuditInfo` — audit tracking API with lifecycle methods
- `AuthCode` — credential storage
- `DeviceInfo` — session/security metadata
- `MediaItem` — media reference
- `PaymentInfo` — transaction record
- `ProductQuantity` — inventory pair
- `SkuInfo` — SKU generation
- `Specification` — key-value spec
- `SpecificationGroup` — grouped specs

**Non-Embeddable DTO Classes:**
- `AuditEntry` — audit event record
- `Promotion` — promotional offer
- `Specificationa` — duplicate spec record
- `VariantOption` — variant dimensions

---

## Configuration Notes

- **CLOB Serialization:** `AuditInfo.updateHistory` persisted as CLOB via custom converter.
- **Enums:** `PaymentMethod`, `PaymentStatus`, `ActiveStatus` use `@Enumerated(EnumType.STRING)`.
- **Timestamps:** Leverage Spring Data JPA's `@CreatedDate`, `@CreatedBy` auditing framework.
- **Default Collections:** `AuditInfo.updateHistory` initialized as empty `ArrayList` via `@Builder.Default`.