# ResponseConfig Group - Knowledge Document

## Overview

The **ResponseConfig** group provides standardized HTTP response structures for the ERP Spring Boot application. It encapsulates API status information, pagination metadata, and generic response wrappers to ensure consistent response formatting across all endpoints. These classes enable type-safe, reusable response objects with support for both paginated and single-item data.

---

## ApiStatus

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/response/ResponseConfig/ApiStatus.java`

**Purpose:** Represents the status metadata of an API response, containing HTTP status codes and user-facing messages.

### Key Components

- **Fields:**
  - `message` (String) — Human-readable status message
  - `code` (int) — HTTP status code

- **Constructors:**
  - `ApiStatus()` — Default no-arg constructor
  - `ApiStatus(int code)` — Creates status with code, defaults message to "Success"
  - `ApiStatus(String message, int code)` — Creates status with custom message and code

- **Methods:**
  - `getMessage()` → String — Returns the status message
  - `getCode()` → int — Returns the HTTP status code
  - `name()` → String — Alias for `getMessage()`, returns the message

### Design Pattern
Simple **Value Object** pattern — immutable data carrier for status information.

### Public API
Exposes getters for `message` and `code`; used as the status component in `Response<T>`.

---

## PageableData

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/response/ResponseConfig/PageableData.java`

**Purpose:** Encapsulates pagination metadata extracted from Spring Data's `Page` object, providing client-friendly pagination info.

### Key Components

- **Fields** (private, accessed via Lombok getters/setters):
  - `pageNumber` (int) — Current page (1-indexed for client convenience)
  - `pageSize` (int) — Number of items per page
  - `totalPages` (int) — Total number of pages
  - `totalElements` (long) — Total number of items across all pages

- **Factory Method:**
  - `from(Page<?> page)` → PageableData — Converts Spring `Page` to `PageableData`, adjusting `pageNumber` from 0-indexed to 1-indexed

### Dependencies
- `org.springframework.data.domain.Page` — Source for pagination data
- Lombok annotations (`@Getter`, `@Setter`, `@Builder`, `@FieldDefaults`) — Reduces boilerplate

### Design Pattern
**Builder Pattern** (via Lombok `@Builder`) and **Factory Method** (`from()`) for convenient construction.

### Public API
Provides pagination metadata as a structured, immutable object for REST responses.

---

## PagingResponse

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/response/ResponseConfig/PagingResponse.java`

**Purpose:** Generic wrapper for paginated list responses, combining actual content with pagination metadata.

### Key Components

- **Type Parameter:** `<T>` — Generic type for list contents

- **Fields** (private, via Lombok):
  - `contents` (List<T>, default: empty ArrayList) — The actual page of data items
  - `paging` (PageableData) — Pagination metadata

- **Factory Method:**
  - `from(Page<T> page)` → PagingResponse<T> — Converts Spring `Page<T>` to `PagingResponse<T>`, extracting content and pagination info

### Dependencies
- `org.springframework.data.domain.Page` — Source pagination object
- `PageableData` — Nested pagination metadata
- Lombok for boilerplate generation

### Design Pattern
**Generic Wrapper** with **Factory Method** for seamless conversion from Spring Data's `Page` objects.

### Public API
Exposes paginated data (`contents`) and metadata (`paging`) as a unified response structure.

---

## Response

**File:** `src/main/java/com/anno/ERP_SpringBoot_Experiment/service/dto/response/ResponseConfig/Response.java`

**Purpose:** Generic top-level HTTP response wrapper that standardizes all API responses with status and optional data payload. Provides factory methods for common HTTP scenarios (success, creation, errors, redirects).

### Key Components

- **Type Parameter:** `<T>` — Generic type for response data

- **Fields** (private, via Lombok):
  - `status` (ApiStatus) — HTTP status and message
  - `data` (T, nullable) — Response payload, excluded from JSON if null (`@JsonInclude(NON_NULL)`)

- **Factory Methods:**
  - `ok(T data)` → Response<T> — HTTP 200 with data
  - `ok(T data, String message)` → Response<T> — HTTP 200 with custom message and data
  - `ok(String message)` → Response<T> — HTTP 200 with message only (no data)
  - `created(T data)` → Response<T> — HTTP 201 with created resource
  - `noContent()` → Response<T> — HTTP 204 (no data field)
  - `found(String url)` → Response<T> — HTTP 302 with redirect URL in status message
  - `loginResponse(HttpStatus, T data)` → Response<T> — Login response with custom HTTP status
  - `fail(ApiStatus status)` → Response<T> — Generic failure response with custom status

### Dependencies
- `ApiStatus` — Carries status info
- `org.springframework.http.HttpStatus` — Standard HTTP status codes
- `com.fasterxml.jackson.annotation.JsonInclude` — Conditional JSON serialization
- Lombok for builder and accessors

### Design Pattern
**Builder Pattern** with **Factory Methods** providing semantic, fluent construction for different response scenarios.

### Public API
Exposes `status` and `data` via getters. Used as the top-level response envelope for all API endpoints. Supports type-safe, generic responses.

### Configuration
- **JSON Serialization:** `@JsonInclude(NON_NULL)` — Null fields (e.g., `data` in error responses) are excluded from JSON output, reducing payload size.

---

## Data Flow

1. **Service Layer** generates data → wraps in `Response<T>` via factory methods (e.g., `Response.ok(data)`)
2. For paginated data: Service wraps `Page<T>` → `PagingResponse<T>.from(page)` → `Response.ok(paging)`
3. **Controller** returns `Response<T>` object
4. **Spring MVC** serializes to JSON using Jackson, excluding null fields
5. **Client** receives:
   ```json
   {
     "status": { "code": 200, "message": "Success" },
     "data": { ... }
   }
   ```

---

## Dependencies Summary

| Import | Usage |
|--------|-------|
| `lombok.*` | Boilerplate elimination, builder pattern |
| `org.springframework.data.domain.Page` | Pagination source objects (PageableData, PagingResponse, Response) |
| `org.springframework.http.HttpStatus` | HTTP status codes (Response factory methods) |
| `com.fasterxml.jackson.annotation.JsonInclude` | Conditional JSON serialization in Response |

---

## Configuration & Constants

- **HTTP Status Codes** used: `OK` (200), `CREATED` (201), `NO_CONTENT` (204), `FOUND` (302)
- **JSON Serialization Policy:** Non-null fields only (`@JsonInclude(Include.NON_NULL)`)
- **Default ApiStatus Message:** "Success" (when code-only constructor used)
- **Page Indexing Conversion:** Spring's 0-indexed pages converted to 1-indexed for client APIs in `PageableData.from()`