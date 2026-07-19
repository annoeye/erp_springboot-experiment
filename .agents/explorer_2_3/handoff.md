# Shopping Cart Iteration 2 - Fix Strategy

## 1. Observation
The Iteration 1 feedback identified 4 critical and medium issues in the implementation of the Shopping Cart feature:
1. **Integer Overflow**: `ShoppingCart.addItem` modifies quantity via `ci.setQuantity(ci.getQuantity() + quantity)`. Since `quantity` is bounded per-request but not cumulatively, repeated additions can overflow to negative, leading to unbounded store credit.
2. **Math.abs() Edge Case**: `ShoppingCartService.add` uses `Math.abs(quantity)` which evaluates to a negative number if `quantity == Integer.MIN_VALUE`.
3. **Sale Price Bug**: `Helper.recalculateAndUpdateTotals` checks `a.getSalePrice() >= 0`. `salePrice` is a `Double` object. Unboxing a null or treating a zero sale price makes items free.
4. **Transaction Tainting**: `ShoppingCartService` uses `saveAndFlush` in a try-catch for `DataIntegrityViolationException` inside a main `@Transactional` block. This leaves the transaction `rollbackOnly`, causing an `UnexpectedRollbackException`.

## 2. Logic Chain
1. To prevent integer overflow in `ShoppingCart.addItem`, we need to use `Math.addExact` and validate that the final cumulative quantity does not exceed a business limit (e.g., 9999).
2. To avoid the `Math.abs()` edge case, we can simply use `-quantity` since we already verify `quantity < 0` and bounded it at `-9999`. 
3. To correctly calculate sale price, `Helper.java` must check `a.getSalePrice() != null && a.getSalePrice() > 0` before applying it.
4. To fix the transaction issue, the cart creation fallback must be isolated in a new transaction. We can create a method `getOrCreateCart(User user)` in `Helper.java` (which is a proxied `@Component`) annotated with `@Transactional(propagation = Propagation.REQUIRES_NEW)`, and inject `ShoppingCartRepository` into `Helper`.

## 3. Caveats
- Injecting `ShoppingCartRepository` into `Helper` must not cause a circular dependency. Since `Helper` currently only injects `AttributesRepository`, it is safe.
- If the `MAX_QUANTITY` cap needs to be strict, 9999 is used as a reasonable default. The worker can adjust this threshold as required.

## 4. Conclusion
The next implementer should apply the following fixes:
- **`ShoppingCart.java`**: Update `addItem` to use `Math.addExact` and cap the total quantity (e.g. 9999). Throw an exception if exceeded.
- **`ShoppingCartService.java`**: Replace `Math.abs(quantity)` with `-quantity`. Delegate cart fetching/creation to `helper.getOrCreateCart(user)`.
- **`Helper.java`**: 
  - Inject `ShoppingCartRepository`.
  - Add `@Transactional(propagation = Propagation.REQUIRES_NEW)` to a new `getOrCreateCart` method that implements the try-catch `saveAndFlush` logic.
  - In `recalculateAndUpdateTotals`, change the sale price condition to `sp != null && sp > 0 ? sp : a.getPrice()`.

## 5. Verification Method
- Run `mvn test` to execute the existing JUnit tests.
- Inspect `ShoppingCart.java` for `Math.addExact` and bounds checking.
- Inspect `Helper.java` to ensure the `REQUIRES_NEW` transaction isolation is implemented for cart initialization, and that the sale price check is null-safe.
