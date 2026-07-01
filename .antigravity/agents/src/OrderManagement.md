# OrderManagement Group - Knowledge Document

## Overview

The **OrderManagement** group handles the complete order lifecycle in the ERP system, from creation through delivery and returns. It manages order state transitions, status validation, item composition, inventory integration, and event publishing for asynchronous processing.

---

## File: OrderService.java

**Purpose:** Core service implementing order business logic. Handles creation, retrieval, searching, and all status transitions (confirm, cancel, process, ship, deliver, return, refund). Integrates inventory management and outbox event publishing.

### Key Classes & Functions

| Function | Parameters | Return | Purpose |
|----------|-----------|--------|---------|
| `createOrder()` | `CreateOrderRequest` | `Response<OrderDto>` | Creates order from cart or direct items, initializes status based on payment method (COD auto-confirms, online payment waits), saves outbox events |
| `getOrderById()` | `String id` | `Response<OrderDto>` | Fetch order by ID |
| `getOrderByOrderNumber()` | `String n` | `Response<OrderDto>` | Fetch order by order number |
| `getMyOrders()` | `OrderSearchRequest` | `Response<PagingResponse<OrderDto>>` | Get current user's orders (20 per page, fixed) |
| `searchOrders()` | `OrderSearchRequest` | `Response<PagingResponse<OrderDto>>` | Search with pagination, sorting, custom criteria via `OrderSpecification` |
| `getPendingOrders()` | — | `Response<List<OrderDto>>` | Fetch all pending orders |
| `getInProgressOrders()` | — | `Response<List<OrderDto>>` | Fetch all in-progress orders |
| `confirmOrder()` | `ConfirmOrderRequest` | `Response<OrderDto>` | Transition to CONFIRMED, confirm inventory reservation, record timestamp & operator |
| `cancelOrder()` | `CancelOrderRequest` | `Response<OrderDto>` | Transition to CANCELLED, release inventory, emit order cancelled event with paid flag |
| `processOrder()` | `ProcessOrderRequest` | `Response<OrderDto>` | Transition to PROCESSING |
| `shipOrder()` | `ShipOrderRequest` | `Response<OrderDto>` | Transition to SHIPPING, set shipper info & delivery token |
| `deliverOrder()` | `DeliverOrderRequest` | `Response<OrderDto>` | Transition to DELIVERED, set actual delivery date |
| `completeOrder()` | `CompleteOrderRequest` | `Response<OrderDto>` | Transition to COMPLETED |
| `returnOrder()` | `ReturnOrderRequest` | `Response<OrderDto>` | Transition to RETURNING |
| `confirmReturn()` | `ConfirmReturnRequest` | `Response<OrderDto>` | Transition to RETURNED |
| `refundOrder()` | `RefundOrderRequest` | `Response<OrderDto>` | Transition to REFUNDED, release inventory |
| `processPayment()` | `PaymentCallbackRequest` | `Response<OrderDto>` | Handle payment callback: on SUCCESS transition to CONFIRMED → PROCESSING; on failure → FAILED |
| `markDelayed()` | `DelayOrderRequest` | `Response<OrderDto>` | Transition to DELAYED |
| `readyForPickup()` | `ReadyForPickupRequest` | `Response<OrderDto>` | Transition to READY_FOR_PICKUP |
| `pickupOrder()` | `PickupOrderRequest` | `Response<OrderDto>` | Transition to DELIVERED (in-store pickup path) |
| `updateShipping()` | `UpdateShippingRequest` | `Response<OrderDto>` | Update shipping method |
| `updateDelivery()` | `UpdateDeliveryRequest` | `Response<OrderDto>` | Update estimated/actual delivery dates |
| `updateAdminNotes()` | `UpdateAdminNotesRequest` | `Response<OrderDto>` | Update admin notes field |
| `setStatus()` | `String orderNumber`, `OrderStatus status` | `void` | Direct status setter (used internally) |

### Data Flow

1. **Order Creation** (`createOrder`):
   - Retrieves current user via `SecurityUtil`
   - Generates order number: `ORD-yyyyMMdd-XXXX`
   - Sets initial status list based on payment method:
     - COD: `[PENDING, CONFIRMED, PROCESSING]`
     - Online: `[PENDING, WAITING_PAYMENT]`
   - If `fromCart=true`: clears shopping cart, loads items from cart
   - If `fromCart=false`: loads items from request via SKU lookup
   - Calculates totals (subtotal, discount, shipping → total amount)
   - Saves order to DB
   - Publishes **OrderCreatedEvent** and auto-transition events via `OutboxOrderHelper`

2. **Status Transitions**:
   - All transition methods follow pattern: fetch order → get current status via `OrderStatusHandler` → validate transition → apply transition → update timestamps/fields → save → emit event
   - `OrderStatusHandler` validates allowed transitions (see below)
   - All transition methods are `@Transactional`

3. **Inventory Integration**:
   - On `confirmOrder()`: calls `OrderInventoryService.confirmReservation(items)`
   - On `cancelOrder()` / `refundOrder()`: calls `OrderInventoryService.releaseInventory(items)`

4. **Event Publishing**:
   - Outbox pattern via `OutboxOrderHelper`:
     - `saveOrderCreatedEvent()` on creation
     - `saveOrderStatusChangedEvent()` on every transition
     - `saveOrderCancelledEvent()` on cancellation (includes `wasPaid` flag)

### Dependencies

| Import | Purpose |
|--------|---------|
| `OrderRepository`, `OrderItemRepository`, `AttributesRepository`, `ProductRepository`, `UserRepository`, `ShoppingCartRepository` | Data access for orders, items, attributes, products, users, carts |
| `OrderMapper` | DTO conversion |
| `SecurityUtil` | Current user & username retrieval |
| `OrderStatusHandler` | Status transition validation & management |
| `OutboxOrderHelper` | Outbox event creation |
| `OrderInventoryService` | Inventory confirmation/release |
| `SpecificationBuilder`, `OrderSpecification` | Dynamic query building for search |
| `OrderStatus`, `PaymentMethod`, `SearchOperation` enums | Domain constants |
| `AuditInfo` | Audit trail embedded object |

### Design Patterns

- **State Machine**: `OrderStatusHandler` enforces valid state transitions via static `ALLOWED` map
- **Outbox Pattern**: All domain events published via `OutboxOrderHelper` for eventual consistency
- **Specification Pattern**: Search via `OrderSpecification` + `SpecificationBuilder` for dynamic WHERE clauses
- **Mapper Pattern**: `OrderMapper` isolates entity-to-DTO conversion
- **Service Facade**: `OrderService` orchestrates multiple repositories and services

### Public API

**Service Interface**: `iOrder` (implemented)
- Order CRUD & retrieval methods
- Status transition methods (confirm, cancel, process, ship, deliver, complete, return, refund, etc.)
- Payment callback handler
- Search with pagination
- Statistics placeholder

**Request DTOs**:
- `CreateOrderRequest`, `ConfirmOrderRequest`, `CancelOrderRequest`, `ProcessOrderRequest`, `ShipOrderRequest`, `DeliverOrderRequest`, `UpdateShippingRequest`, `UpdateDeliveryRequest`, `UpdateAdminNotesRequest`, `PaymentCallbackRequest`, etc.

**Response**: `Response<OrderDto>` wrapping paginated or single order data

### Configuration & Constants

- **Order Number Format**: `ORD-yyyyMMdd-XXXX` (4-digit random suffix)
- **Default Shipping Fee**: `30000.0` VND
- **Default Page Size**: `20` items
- **Default Sort**: `auditInfo.createdAt DESC`
- **Payment Methods**: COD (cash-on-delivery) auto-transitions to PROCESSING; online methods trigger WAITING_PAYMENT
- **Audit Trail**: All orders include `AuditInfo` with creation entry and timestamp

---

## File: OrderStatusHandler.java

**Purpose:** Manages order status state machine. Validates transitions, returns current status, detects terminal states. Singleton component holding all allowed transition rules.

### Key Classes & Functions

| Function | Parameters | Return | Purpose |
|----------|-----------|--------|---------|
| `transitionTo()` | `Order order`, `OrderStatus target`, `String note` | `void` | Validates transition, appends target to status list, sets as currentStatus; throws `BusinessException` if invalid |
| `getCurrentStatus()` | `Order order` | `OrderStatus` | Returns last status in list (defaults to PENDING if empty) |
| `isTerminal()` | `OrderStatus s` | `boolean` | Returns true if status is COMPLETED, CANCELLED, FAILED, or REFUNDED |
| `isValidTransition()` | `OrderStatus current`, `OrderStatus target` | `boolean` | Checks if target is in allowed set for current (no-throw variant) |

### State Machine Definition

Static `ALLOWED` map defines transitions:

```
PENDING → {CONFIRMED, WAITING_PAYMENT, CANCELLED}
WAITING_PAYMENT → {CONFIRMED, CANCELLED, FAILED}
CONFIRMED → {PROCESSING, CANCELLED}
PROCESSING → {SHIPPING, READY_FOR_PICKUP, CANCELLED}
SHIPPING → {DELIVERED, DELAYED, RETURNING}
DELAYED → {SHIPPING, RETURNING}
READY_FOR_PICKUP → {DELIVERED, RETURNING}
DELIVERED → {COMPLETED, RETURNING}
COMPLETED → {} (terminal)
FAILED → {} (terminal)
CANCELLED → {} (terminal)
RETURNING → {RETURNED}
RETURNED → {REFUNDED}
REFUNDED → {} (terminal)
```

### Design Patterns

- **State Machine**: Hardcoded transition table with validated graph
- **Immutable Rules**: Static initialization block prevents runtime modification
- **Fail-Fast**: Throws `BusinessException` with localized message on invalid transition

### Public API

- `transitionTo(Order, OrderStatus, String)`: primary method for state changes
- `getCurrentStatus(Order)`: read current state
- `isValidTransition(OrderStatus, OrderStatus)`: validation check (non-throwing)
- `isTerminal(OrderStatus)`: terminal state detection

### Dependencies

| Import | Purpose |
|--------|---------|
| `OrderStatus` enum | State values & display names |
| `Order` entity | Order object to mutate |
| `BusinessException`, `ErrorCode` | Error handling |

---

## Data Flow Summary

```
CreateOrderRequest
  ↓
OrderService.createOrder()
  ├→ SecurityUtil.getCurrentUser()
  ├→ Generate order number
  ├→ Build OrderItems (from cart or direct)
  ├→ Calculate totals
  ├→ OrderRepository.save()
  ├→ OutboxOrderHelper.saveOrderCreatedEvent()
  └→ OutboxOrderHelper.saveOrderStatusChangedEvent() [if COD or online]

Status Transition (e.g., confirmOrder)
  ↓
OrderService.confirmOrder()
  ├→ OrderStatusHandler.getCurrentStatus()
  ├→ OrderStatusHandler.transitionTo() [validates, mutates order.status list]
  ├→ OrderInventoryService.confirmReservation()
  ├→ OrderRepository.save()
  ├→ OutboxOrderHelper.saveOrderStatusChangedEvent()
  └→ Log transition

Payment Callback
  ↓
OrderService.processPayment()
  ├→ Validate current status is WAITING_PAYMENT or PENDING
  ├→ If SUCCESS: transitionTo(CONFIRMED) → transitionTo(PROCESSING)
  ├→ If FAILED: transitionTo(FAILED)
  ├→ Emit event
  └→ OrderRepository.save()
```

---

## Integration Points

- **Inventory**: `OrderInventoryService` confirms/releases stock on state transitions
- **Events**: `OutboxOrderHelper` publishes domain events for external subscribers
- **Auth**: `SecurityUtil` enforces user identity for order creation & retrieval
- **Search**: `OrderSpecification` + Spring Data `Specification` for dynamic queries
- **Audit**: `AuditInfo` embedded object tracks creation & updates