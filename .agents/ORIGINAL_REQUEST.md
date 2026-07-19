# Original User Request

## Initial Request — 2026-07-15T09:46:21+07:00

Implement the Shopping Cart feature (Web, Service, Repository, Entity layers) in the existing Spring Boot ERP application, managing user carts, items, and total calculations based on the provided technical specification.

Working directory: ~/Projects/erp_springboot-experiment
Integrity mode: development

## Requirements

### R1. Database Models & Repositories
Create `ShoppingCart` and `CartItem` entities with the specified fields, 1-1 relationship with `User`, and 1-N relationship with `CartItem`. Implement Spring Data JPA repositories.
Note: If `User` or `Attributes` entities do not already exist in the codebase, create basic versions or mocks so the code compiles and runs.

### R2. Service Logic & Total Calculation
Implement `ShoppingCartService` and `Helper` to handle lazy cart initialization, dynamic quantity updates (add/remove/decrease), and automatic recalculation of `totalItems`, `totalPrice`, `totalSalePrice`, and `totalDiscount`. 

### R3. REST API Endpoints
Implement `ShoppingCartController` with endpoints for getting the cart, adding items, removing items by SKU, and clearing the cart. Ensure all endpoints require authentication (or simulated authentication if security is not fully configured).

### R4. DTOs and Data Privacy
Implement `ShoppingCartDto` and `CartItemDto`. **Strictly remove all internal `id` fields** from the API responses (DTOs) to prevent exposing internal database identifiers. 

## Acceptance Criteria

### API Functionality
- [ ] `GET /api/cart` creates a new cart if none exists and returns 200 OK.
- [ ] `POST /api/cart/add` correctly updates quantities (adding, subtracting, or removing if <= 0) and recalculates totals.
- [ ] `DELETE /api/cart/remove` and `DELETE /api/cart/clear` successfully remove items.
- [ ] API responses (DTOs) strictly do not contain `id` fields.

### Logic Verification
- [ ] Unit tests (JUnit/Mockito) are written for the `ShoppingCartService` or `ShoppingCartController` to verify that totals (`totalPrice`, `totalSalePrice`, `totalDiscount`) accurately reflect the quantities and prices matching the `Attributes` logic.
- [ ] Cart interaction properly records AuditInfo (created_at, updated_at).
