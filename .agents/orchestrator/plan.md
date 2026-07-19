# Shopping Cart Feature — Implementation Plan

## Scope
Complete the Shopping Cart backend feature in Spring Boot ERP.

## Files to Modify / Create

### 1. iShoppingCart.java (MODIFY)
Add two new methods to the interface:
```java
Response<ShoppingCartDto> getCart();
Response<ShoppingCartDto> clearCart();
```

### 2. ShoppingCartService.java (MODIFY)
Implement `getCart()` and `clearCart()`:

**getCart():**
- Get current username via `securityUtil.getCurrentUsername()`
- Load user via `userRepository.findByNameOrEmail(username)`
- Find or create cart using `shoppingCartRepository.findByUser(user).orElseGet(() -> helper.createNewCart(user))`
- Save the newly created cart if it was just created
- Return `Response.ok(helper.toDto(cart), "Lấy giỏ hàng thành công")`

**clearCart():**
- Get current username and user
- Find cart (throw BusinessException if not found)
- Call `cart.clearItems()` to clear all items
- Reset totals: `cart.updateTotals(0, 0.0, 0.0)`
- Add audit entry: `cart.getAuditInfo().addUpdateEntry("Xóa toàn bộ giỏ hàng", username)`
- Save cart
- Return `Response.ok(helper.toDto(savedCart), "Đã xóa toàn bộ giỏ hàng")`

### 3. ShoppingCartController.java (CREATE NEW)
Location: src/main/java/com/anno/ERP_SpringBoot_Experiment/web/rest/ShoppingCartController.java

Interface following AuthController.java pattern:
```java
@RequestMapping("/api/cart")
public interface ShoppingCartController {
    @GetMapping
    Response<ShoppingCartDto> getCart();
    
    @PostMapping("/add")
    Response<ShoppingCartDto> addToCart(@RequestBody List<CartItemRequest> items);
    
    @DeleteMapping("/remove")
    Response<ShoppingCartDto> removeFromCart(@RequestBody List<String> skus);
    
    @DeleteMapping("/clear")
    Response<ShoppingCartDto> clearCart();
}
```

### 4. ShoppingCartControllerImpl.java (CREATE NEW)
Location: src/main/java/com/anno/ERP_SpringBoot_Experiment/web/rest/impl/ShoppingCartControllerImpl.java

Implementation following authControllerImpl.java pattern:
- @RestController
- @RequiredArgsConstructor
- Delegates to iShoppingCart service

### 5. SecurityConfiguration.java (REVIEW — likely no change needed)
The existing `anyRequest().authenticated()` already covers `/api/cart/**`.
We can optionally add explicit rule `.requestMatchers("/api/cart/**").authenticated()` for clarity.

## Build Verification
Run: `./mvnw clean compile` from project root
Expected: BUILD SUCCESS with no errors.
