# Iteration 1 Failure Feedback

The previous Worker implemented the missing Shopping Cart endpoints (`getCart`, `clearCart`) and Controller, but the gate failed due to critical logic and security flaws identified by the Reviewer and Challenger. The Forensic Auditor reported a CLEAN verdict on integrity.

You must design a strategy to fix the following issues:

## 1. Integer Overflow to Negative Pricing (Critical)
**What:** In `Helper.handleAddItem` and `ShoppingCart.addItem`, `quantity` is a signed 32-bit `int`. Adding large positive quantities can overflow `Integer.MAX_VALUE`, making `ci.getQuantity()` negative. When multiplied by `price`, the `totalPrice` becomes negative, effectively giving unbounded store credit.
**Fix:** Add strict bounds (e.g. max 100 items per request) and/or use `Math.addExact` to prevent overflow.

## 2. Math.abs() Boundary Edge Case (Medium)
**What:** `Math.abs(Integer.MIN_VALUE)` evaluates to a negative number. This breaks logic in `ShoppingCartService.add` and `Helper.handleDecreaseItem`.

## 3. Attributes.java Sale Price Bug (Critical)
**What:** In `Helper.java` (`recalculateAndUpdateTotals`), `a.getSalePrice() >= 0` makes all non-sale items FREE because `salePrice` is a primitive `double` defaulting to `0.0`. 
**Fix:** Check for `> 0`, or change `Attributes.java` to use a specific sentinel or `Double`.

## 4. Lazy Cart Initialization Race Condition (Medium)
**What:** In `ShoppingCartService.add` and `getCart`, `.orElseGet(() -> helper.createNewCart(user))` is used. Concurrent `add` requests cause a `DataIntegrityViolationException`. Furthermore, catching `DataIntegrityViolationException` inside a `@Transactional` block does not clear the Spring `rollbackOnly` flag, causing an `UnexpectedRollbackException` on commit.
**Fix:** Cart creation must be handled gracefully, e.g., using a separate transaction (`REQUIRES_NEW`), or handling it outside the main transaction to avoid tainting the parent transaction block.

Review these findings and recommend a fix strategy for the next worker.
