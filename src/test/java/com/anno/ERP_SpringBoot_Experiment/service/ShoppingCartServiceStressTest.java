package com.anno.ERP_SpringBoot_Experiment.service;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.model.entity.ShoppingCart;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.repository.AttributesRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.ShoppingCartRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.Helper;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.ShoppingCartService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CartItemRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ShoppingCartDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShoppingCartServiceStressTest {

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

    private User testUser;
    private ShoppingCart cart;

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

        testUser = new User();
        testUser.setId(1L);
        testUser.setName("testuser");

        cart = new ShoppingCart();
        cart.setUser(testUser);
        cart.setCartItems(new ArrayList<>());
    }

    @Test
    void testIntegerOverflowVulnerability() {
        String sku = "SKU_OVERFLOW";

        Attributes attributes = new Attributes();
        attributes.setSku(new SkuInfo(sku));
        attributes.setPrice(100.0);
        attributes.setSalePrice(90.0);

        when(securityUtil.getCurrentUsername()).thenReturn("testuser");
        when(userRepository.findByNameOrEmail("testuser")).thenReturn(Optional.of(testUser));
        when(shoppingCartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));
        when(attributesRepository.findAllBySku_skuIn(any())).thenReturn(List.of(attributes));

        // Quantity exceeds the limit of 9999, so each request should be rejected.
        List<CartItemRequest> items = Arrays.asList(
            new CartItemRequest(sku, 1000000000)
        );

        // Service should throw BusinessException due to quantity validation (> 9999)
        assertThrows(Exception.class, () -> shoppingCartService.add(items),
                "Should reject quantity exceeding allowed range");
    }

    @Test
    void testMathAbsIntegerMinValueEdgeCase() {
        String sku = "SKU_MIN_VAL";

        Attributes attributes = new Attributes();
        attributes.setSku(new SkuInfo(sku));
        attributes.setPrice(100.0);
        attributes.setSalePrice(90.0);

        when(securityUtil.getCurrentUsername()).thenReturn("testuser");
        when(userRepository.findByNameOrEmail("testuser")).thenReturn(Optional.of(testUser));
        when(shoppingCartRepository.findByUser(testUser)).thenReturn(Optional.of(cart));
        when(attributesRepository.findAllBySku_skuIn(any())).thenReturn(List.of(attributes));

        // Pre-fill the cart
        cart.addItem(sku, 5);

        // Integer.MIN_VALUE is below -9999 so validation should reject it
        List<CartItemRequest> items = Arrays.asList(
            new CartItemRequest(sku, Integer.MIN_VALUE)
        );

        assertThrows(Exception.class, () -> shoppingCartService.add(items),
                "Should reject Integer.MIN_VALUE due to quantity range validation");
    }
}
