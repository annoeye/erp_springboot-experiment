# Converter Module Knowledge Document

## Overview

The `converter` group provides JPA attribute converters and Spring type converters that serialize complex Java objects to/from database-compatible formats (JSON strings). The module bridges the gap between Java collections/embedded objects and relational database storage, enabling seamless persistence of nested data structures.

---

## File-by-File Analysis

### AuditEntryListConverter.java

**Purpose:** Converts `List<AuditEntry>` between Java objects and JSON strings for database persistence.

**Key Class:** `AuditEntryListConverter`
- Implements `AttributeConverter<List<AuditEntry>, String>`
- Annotated with `@Converter` (JPA auto-detection)

**Methods:**
- `convertToDatabaseColumn(List<AuditEntry>)` → `String`: Serializes list to JSON; returns `"[]"` on null or error
- `convertToEntityAttribute(String)` → `List<AuditEntry>`: Deserializes JSON to list; returns empty `ArrayList` on null/empty/error

**Dependencies:**
- `com.fasterxml.jackson.databind.ObjectMapper`: JSON serialization
- `com.fasterxml.jackson.datatype.jsr310.JavaTimeModule`: Handles `java.time` types in `AuditEntry`
- `jakarta.persistence.AttributeConverter`: JPA converter contract
- `com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditEntry`: Target model

**Design Pattern:** JPA Attribute Converter with static ObjectMapper singleton

**Error Handling:** Silent fallback to empty collections/arrays; no logging

---

### DeviceInfoListConverter.java

**Purpose:** Converts `List<DeviceInfo>` to/from JSON with error logging and exception propagation.

**Key Class:** `DeviceInfoListConverter`
- Implements `AttributeConverter<List<DeviceInfo>, String>`
- Instance-scoped ObjectMapper (differs from other converters)

**Methods:**
- `convertToDatabaseColumn(List<DeviceInfo>)` → `String`: Serializes to JSON; throws `RuntimeException` on error; returns `null` for empty/null input
- `convertToEntityAttribute(String)` → `List<DeviceInfo>`: Deserializes from JSON; throws `RuntimeException` on error; returns empty `ArrayList` for empty/null input

**Dependencies:**
- `com.fasterxml.jackson.databind.ObjectMapper`: JSON processing
- `com.fasterxml.jackson.core.JsonProcessingException`: Write errors
- `java.io.IOException`: Read errors
- `org.slf4j.Logger/LoggerFactory`: Error logging (Vietnamese messages)
- `com.anno.ERP_SpringBoot_Experiment.model.embedded.DeviceInfo`: Target model

**Design Pattern:** JPA Attribute Converter with explicit error logging and fail-fast semantics

**Error Handling:** Logs errors in Vietnamese, throws exceptions (stricter than peers)

---

### MediaItemListConverter.java

**Purpose:** Converts `List<MediaItem>` to/from JSON for database storage.

**Key Class:** `MediaItemListConverter`
- Implements `AttributeConverter<List<MediaItem>, String>`

**Methods:**
- `convertToDatabaseColumn(List<MediaItem>)` → `String`: Serializes to JSON; returns `"[]"` on error
- `convertToEntityAttribute(String)` → `List<MediaItem>`: Deserializes from JSON; returns empty `ArrayList` on error

**Dependencies:**
- `com.fasterxml.jackson.databind.ObjectMapper`: JSON serialization
- `jakarta.persistence.AttributeConverter`: JPA contract
- `com.anno.ERP_SpringBoot_Experiment.model.embedded.MediaItem`: Target model

**Design Pattern:** Standard JPA Attribute Converter with silent error recovery

---

### OrderStatusListConverter.java

**Purpose:** Converts `List<OrderStatus>` enum lists to/from JSON.

**Key Class:** `OrderStatusListConverter`
- Implements `AttributeConverter<List<OrderStatus>, String>`

**Methods:**
- `convertToDatabaseColumn(List<OrderStatus>)` → `String`: Serializes enum list to JSON; returns `"[]"` on error
- `convertToEntityAttribute(String)` → `List<OrderStatus>`: Deserializes JSON to enum list; returns empty `ArrayList` on error

**Dependencies:**
- `com.fasterxml.jackson.databind.ObjectMapper`: Enum serialization
- `com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus`: Target enum
- `jakarta.persistence.AttributeConverter`: JPA contract

**Design Pattern:** JPA Attribute Converter specialized for enum collections

---

### ProductQuantityListConverter.java

**Purpose:** Converts `List<ProductQuantity>` to/from JSON for database persistence.

**Key Class:** `ProductQuantityListConverter`
- Implements `AttributeConverter<List<ProductQuantity>, String>`

**Methods:**
- `convertToDatabaseColumn(List<ProductQuantity>)` → `String`: Serializes to JSON; returns `"[]"` on error
- `convertToEntityAttribute(String)` → `List<ProductQuantity>`: Deserializes from JSON; returns empty `ArrayList` on error

**Dependencies:**
- `com.fasterxml.jackson.databind.ObjectMapper`: JSON processing
- `jakarta.persistence.AttributeConverter`: JPA contract
- `com.anno.ERP_SpringBoot_Experiment.model.embedded.ProductQuantity`: Target model

**Design Pattern:** Standard JPA Attribute Converter

---

### PromotionListConverter.java

**Purpose:** Converts `List<Promotion>` to/from JSON, handling temporal types.

**Key Class:** `PromotionListConverter`
- Implements `AttributeConverter<List<Promotion>, String>`

**Methods:**
- `convertToDatabaseColumn(List<Promotion>)` → `String`: Serializes list to JSON; returns `"[]"` on error
- `convertToEntityAttribute(String)` → `List<Promotion>`: Deserializes JSON to list; returns empty `ArrayList` on error

**Dependencies:**
- `com.fasterxml.jackson.databind.ObjectMapper`: JSON serialization
- `com.fasterxml.jackson.datatype.jsr310.JavaTimeModule`: Date/time handling in `Promotion`
- `jakarta.persistence.AttributeConverter`: JPA contract
- `com.anno.ERP_SpringBoot_Experiment.model.embedded.Promotion`: Target model

**Design Pattern:** JPA Attribute Converter with JSR-310 temporal support

---

### SpecificationGroupListConverter.java

**Purpose:** Converts `List<SpecificationGroup>` to/from JSON.

**Key Class:** `SpecificationGroupListConverter`
- Implements `AttributeConverter<List<SpecificationGroup>, String>`

**Methods:**
- `convertToDatabaseColumn(List<SpecificationGroup>)` → `String`: Serializes to JSON; returns `"[]"` on error
- `convertToEntityAttribute(String)` → `List<SpecificationGroup>`: Deserializes from JSON; returns empty `ArrayList` on error

**Dependencies:**
- `com.fasterxml.jackson.databind.ObjectMapper`: JSON processing
- `jakarta.persistence.AttributeConverter`: JPA contract
- `com.anno.ERP_SpringBoot_Experiment.model.embedded.SpecificationGroup`: Target model

**Design Pattern:** Standard JPA Attribute Converter

---

### StringSetConverter.java

**Purpose:** Converts `Set<String>` to/from JSON for database storage.

**Key Class:** `StringSetConverter`
- Implements `AttributeConverter<Set<String>, String>`

**Methods:**
- `convertToDatabaseColumn(Set<String>)` → `String`: Serializes set to JSON; returns `"[]"` on error
- `convertToEntityAttribute(String)` → `Set<String>`: Deserializes JSON to `HashSet`; returns empty `HashSet` on error

**Dependencies:**
- `com.fasterxml.jackson.databind.ObjectMapper`: JSON serialization
- `jakarta.persistence.AttributeConverter`: JPA contract

**Design Pattern:** JPA Attribute Converter for collection types

---

### StringToDeviceInfoConverter.java

**Purpose:** Spring type converter that deserializes JSON strings to `DeviceInfo` objects for HTTP request parameter binding.

**Key Class:** `StringToDeviceInfoConverter`
- Implements Spring's `Converter<String, DeviceInfo>`
- Annotated with `@Component` and `@AllArgsConstructor` (dependency injection)

**Methods:**
- `convert(String)` → `DeviceInfo`: Deserializes JSON string to `DeviceInfo`; returns `null` on error

**Dependencies:**
- `com.fasterxml.jackson.databind.ObjectMapper`: Injected dependency for JSON parsing
- `org.springframework.core.convert.converter.Converter`: Spring conversion contract
- `org.springframework.stereotype.Component`: Spring bean registration
- `org.slf4j.Logger/LoggerFactory`: Error logging (Vietnamese)
- `com.anno.ERP_SpringBoot_Experiment.model.embedded.DeviceInfo`: Target model

**Design Pattern:** Spring Type Converter with constructor injection

**Error Handling:** Logs errors with input context, returns `null` on failure

---

### VariantOptionListConverter.java

**Purpose:** Converts `List<VariantOption>` to/from JSON for database persistence.

**Key Class:** `VariantOptionListConverter`
- Implements `AttributeConverter<List<VariantOption>, String>`

**Methods:**
- `convertToDatabaseColumn(List<VariantOption>)` → `String`: Serializes to JSON; returns `"[]"` on error
- `convertToEntityAttribute(String)` → `List<VariantOption>`: Deserializes from JSON; returns empty `ArrayList` on error

**Dependencies:**
- `com.fasterxml.jackson.databind.ObjectMapper`: JSON processing
- `jakarta.persistence.AttributeConverter`: JPA contract
- `com.anno.ERP_SpringBoot_Experiment.model.embedded.VariantOption`: Target model

**Design Pattern:** Standard JPA Attribute Converter

---

## Cross-Cutting Concerns

### Data Flow

1. **JPA Converters** (9 files implementing `AttributeConverter`):
   - Entity load: Database JSON string → `convertToEntityAttribute()` → Java object/collection
   - Entity persist: Java object/collection → `convertToDatabaseColumn()` → Database JSON string

2. **Spring Type Converter** (StringToDeviceInfoConverter):
   - HTTP request parameter: String query parameter → `convert()` → `DeviceInfo` object
   - Used for automatic type conversion in controller method parameters

### JSON Serialization Strategy

- **ObjectMapper reuse:** Most converters use static singleton; `DeviceInfoListConverter` uses instance-scoped
- **Temporal support:** `AuditEntryListConverter` and `PromotionListConverter` register `JavaTimeModule` for `java.time` types
- **TypeReference pattern:** Used for generic type preservation (`new TypeReference<List<T>>() {}`)

### Error Handling Variance

- **Silent fallback (8 files):** Return empty collections/`"[]"` on error; no logging
- **Fail-fast with logging (2 files):** `DeviceInfoListConverter` and `StringToDeviceInfoConverter` throw exceptions and log errors

### Public API

**Exposed to entity layer:**
- All 9 JPA converters auto-discovered by JPA via `@Converter` annotation
- Applied via `@Convert(converter = ...)` on entity fields

**Exposed to Spring:**
- `StringToDeviceInfoConverter` registered as Spring bean; auto-discovered for type conversion
- Used by Spring's ConversionService for request parameter binding

### Configuration

- **No environment variables or settings files**
- **Static constants:** Jackson `ObjectMapper` configuration via `JavaTimeModule` registration
- **All converters operate with default ObjectMapper settings** (except explicit module registrations)