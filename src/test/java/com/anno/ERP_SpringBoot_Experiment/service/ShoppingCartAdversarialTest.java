package com.anno.ERP_SpringBoot_Experiment.service;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.model.entity.ShoppingCart;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.repository.AttributesRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.ShoppingCartRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.Helper;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.ShoppingCartService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ShoppingCartDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CartItemRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShoppingCartAdversarialTest {

    @Mock
    private ShoppingCartRepository shoppingCartRepository;

    @Mock
    private AttributesRepository attributesRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtil securityUtil;

    private Helper helper;

    private ShoppingCartService shoppingCartService;

    @BeforeEach
    void setUp() {
        helper = new Helper(attributesRepository);
        shoppingCartService = new ShoppingCartService(
                shoppingCartRepository,
                attributesRepository,
                userRepository,
                securityUtil,
                helper
        );
    }

    @Test
    void testFreeItem_SalePriceIsZero() {
        // Given
        String username = "testUser";
        String sku = "FREE-SKU";

        List<CartItemRequest> items = Collections.singletonList(
                new CartItemRequest(sku, 1)
        );

        User user = new User();
        user.setName(username);

        ShoppingCart cart = new ShoppingCart();
        cart.setUser(user);
        cart.setAuditInfo(new com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo());

        Attributes attributes = new Attributes();
        attributes.setSku(new SkuInfo(sku));
        attributes.setPrice(100.0);
        attributes.setSalePrice(0.0); // Free item!

        when(securityUtil.getCurrentUsername()).thenReturn(username);
        when(userRepository.findByNameOrEmail(username)).thenReturn(Optional.of(user));
        when(shoppingCartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(attributesRepository.findAllBySku_skuIn(anyList())).thenReturn(Collections.singletonList(attributes));
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(cart);

        // When
        Response<ShoppingCartDto> response = shoppingCartService.add(items);

        // Then
        ShoppingCartDto dto = response.getData();
        assertEquals(100.0, dto.getTotalPrice(), "Total price should be 100.0");
        
        // This assertion will fail if the bug exists, because salePrice will evaluate to 100.0
        // Helper calculation: (a.getSalePrice() > 0 ? a.getSalePrice() : a.getPrice()) * item.getQuantity()
        // 0.0 > 0 is false, so it falls back to a.getPrice() -> 100.0
        assertEquals(0.0, dto.getTotalSalePrice(), "Total sale price should be 0.0 for a free item");
    }

    @Test
    void testNegativeQuantityForNonExistentItemThrows500() {
        // Given
        String username = "testUser";
        String sku = "NON-EXISTENT-SKU";

        List<CartItemRequest> items = Collections.singletonList(
                new CartItemRequest(sku, -1) // negative quantity
        );

        User user = new User();
        user.setName(username);

        ShoppingCart cart = new ShoppingCart();
        cart.setUser(user);
        // Cart does NOT contain the item

        Attributes attributes = new Attributes();
        attributes.setSku(new SkuInfo(sku));
        attributes.setPrice(100.0);

        when(securityUtil.getCurrentUsername()).thenReturn(username);
        when(userRepository.findByNameOrEmail(username)).thenReturn(Optional.of(user));
        when(shoppingCartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(attributesRepository.findAllBySku_skuIn(anyList())).thenReturn(Collections.singletonList(attributes));

        // When & Then
        // Expecting a RuntimeException instead of BusinessException
        RuntimeException exception = assertThrows(RuntimeException.class, () -> shoppingCartService.add(items));
        assertEquals("Sản phẩm " + sku + " không có trong giỏ hàng", exception.getMessage());
        
        // Ensure it's exactly RuntimeException, not a subclass like BusinessException
        assertEquals(RuntimeException.class, exception.getClass(), "Should throw raw RuntimeException causing 500 Error");
    }
}
