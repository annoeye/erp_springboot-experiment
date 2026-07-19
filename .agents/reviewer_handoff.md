## Review Summary

**Verdict**: REQUEST_CHANGES

## Findings

### [Critical] Logic Flaw: `a.getSalePrice() >= 0` makes all non-sale items FREE
- **What**: The sale price check `a.getSalePrice() >= 0 ? a.getSalePrice() : a.getPrice()` in `Helper.java:85` will evaluate to `true` for `0.0`.
- **Where**: `Helper.java:85` (`recalculateAndUpdateTotals` method).
- **Why**: `salePrice` is a primitive `double` in `Attributes.java` which defaults to `0.0`. An item that is NOT on sale will have a `salePrice` of `0.0`. Because `0.0 >= 0` evaluates to `true`, the system will use `0.0` as the sale price for ALL items that do not have an explicit sale price set, effectively giving them away for free. The author likely changed `> 0` to `>= 0` to support "Free on sale" items, but in doing so, broke the default behavior for all non-sale items.
- **Suggestion**: Change `Attributes.java` to use the wrapper class `Double` for `salePrice` so `null` can represent "no sale", or use a specific sentinel value like `-1.0` and check for that instead.

### [Critical] Facade Implementation: Spring Transaction Rollback on Concurrent Cart Creation
- **What**: Catching `DataIntegrityViolationException` inside a `@Transactional` block during `saveAndFlush`.
- **Where**: `ShoppingCartService.java:49` and `122` (in `add` and `getCart`).
- **Why**: When `saveAndFlush` throws a constraint violation (caught by Hibernate and translated to `DataIntegrityViolationException` by Spring), the `JpaTransactionManager` marks the current transaction as `rollbackOnly`. Catching the exception in the business layer does NOT clear this flag. Any subsequent operation in the transaction (like `shoppingCartRepository.findByUser(user)`) or the final commit will throw an `UnexpectedRollbackException`. The unit test `shouldHandleConcurrentCartCreation` passes only because it uses a mock `shoppingCartRepository` that simply throws the exception without the real transaction proxy setting the `rollbackOnly` flag.
- **Suggestion**: The creation of a new cart should be handled outside the main transaction, or in a separate transaction (e.g., a method annotated with `@Transactional(propagation = Propagation.REQUIRES_NEW)`), so its failure does not taint the parent transaction.

## Verified Claims

- Prunes dangling items → verified via manual code inspection of `Helper.java` → pass.
- Validates quantity within `[-9999, 9999]` → verified via manual code inspection of `ShoppingCartService.java` → pass.
- Throws `BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND)` → verified via manual code inspection of `Helper.java` → pass.

## Unverified Items
- Full build and test run could not be completed interactively due to time constraints, but code was verified via static analysis.
