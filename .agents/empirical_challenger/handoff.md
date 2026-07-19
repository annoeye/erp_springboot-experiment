# Handoff Report — Empirical Challenge of Shopping Cart

**Type:** Hard
**Agent:** empirical_challenger
**Timestamp:** 2026-07-15T10:10:00+07:00

## 1. Observation

- Explored `ShoppingCartService.java` and `Helper.java`.
- Noticed how `Helper.handleAddItem` and `ShoppingCart.addItem` combine quantities: `ci.setQuantity(ci.getQuantity() + quantity)`. Both fields are 32-bit signed integers (`int`).
- Noticed `ShoppingCartService.add` loop: `int quantity = item.getQuantity();` then `Math.abs(quantity)` is used for negative values.
- Noticed `totalPrice` calculation inside `Helper.recalculateAndUpdateTotals` simply uses `a.getPrice() * item.getQuantity()`.
- Noticed `ShoppingCartService.add` uses `shoppingCartRepository.findByUser(user).orElseGet(() -> helper.createNewCart(user));` followed by `save(cart)` later, with no handling of concurrency or unique constraint violations.
- Discovered that running tests empirically with `./mvnw` or `javac` is currently blocked by environment permission timeouts in this session, so I synthesized an explicit stress test class.

## 2. Logic Chain

1. **Integer Overflow to Negative Pricing:** 
   - Since `quantity` is a signed 32-bit integer, providing a list of large positive quantities for the same SKU (e.g., `[1000000000, 1000000000, 1000000000]`) accumulates in `ShoppingCart.addItem()`.
   - The addition overflows `Integer.MAX_VALUE`, making `ci.getQuantity()` negative (e.g., `-1294967296`).
   - When `Helper.recalculateAndUpdateTotals` multiplies this negative quantity by `price`, the resulting `totalPrice` becomes deeply negative. This allows a user to "buy" a cart with a negative total price, effectively extracting money or gaining unbounded store credit.
2. **Math.abs() Boundary Edge Case:**
   - When a user sends a quantity of `Integer.MIN_VALUE` (`-2147483648`), the code path for `quantity < 0` is taken.
   - `Math.abs(Integer.MIN_VALUE)` evaluates to `-2147483648` due to two's complement limits.
   - In `Helper.handleDecreaseItem`, `newQty = ci.getQuantity() - (-2147483648)`. If `ci.getQuantity()` is small, `newQty` overflows back to negative, causing `newQty <= 0` to trigger, silently deleting the item instead of throwing an error.
3. **Lazy Cart Initialization Race Condition:**
   - Because `helper.createNewCart(user)` does not save immediately, and `ShoppingCart` has a unique constraint on `user_id` (`@JoinColumn(..., unique = true)`), two concurrent `add()` requests for a new user will both create a transient cart.
   - The final `save()` call by the second concurrent thread will trigger a `DataIntegrityViolationException`, resulting in a 500 error instead of gracefully updating the cart.

## 3. Caveats

- I could not empirically execute the generated stress tests because environment permission constraints continuously timed out `mvn` and `javac` execution calls.
- I wrote the exact JUnit 5 exploit payload into `src/test/java/com/anno/ERP_SpringBoot_Experiment/service/ShoppingCartServiceStressTest.java`. This file can be run directly once permissions allow.

## 4. Conclusion

**FAILED (HIGH RISK)**

The Shopping Cart calculations are mathematically vulnerable.
- **Critical Risk:** Integer overflow on item quantity enables negative total prices, acting as a severe business logic vulnerability.
- **Medium Risk:** `Math.abs` flaw handles extreme negative boundaries unpredictably.
- **Medium Risk:** Concurrent lazy initialization will throw unhandled 500 exceptions under load.

Mitigations required:
- Add overflow checks (`Math.addExact`) or strict validation bounds on `item.getQuantity()` (e.g. max 100 items per request).
- Catch `DataIntegrityViolationException` in `ShoppingCartService` or use a synchronized construct / DB pessimistic lock for cart initialization.

## 5. Verification Method

Run the generated adversarial test suite:
```bash
cd /home/ddicgegd/Projects/erp_springboot-experiment
./mvnw test -Dtest=ShoppingCartServiceStressTest
```
If the tests pass, it confirms that the vulnerabilities exist (since the tests assert that the price becomes negative and the item is unexpectedly removed).
