package com.anno.ERP_SpringBoot_Experiment.service;

import com.anno.ERP_SpringBoot_Experiment.mapper.ShoppingCartMapper;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.model.entity.CartItem;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShoppingCartServiceTest {

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
    void shouldSuccessfullyProcessMultipleItemsWithDifferentQuantitiesAndCalculateTotals() {
        // Given
        String username = "testUser";
        String sku1 = "SKU-1";
        String sku2 = "SKU-2";
        String sku3 = "SKU-3";

        List<CartItemRequest> items = Arrays.asList(
                new CartItemRequest(sku1, 2),
                new CartItemRequest(sku2, -1),
                new CartItemRequest(sku3, 0)
        );

        User user = new User();
        user.setName(username);

        ShoppingCart cart = new ShoppingCart();
        cart.setUser(user);
        cart.setAuditInfo(new AuditInfo());
        cart.addItem(sku2, 3);
        cart.addItem(sku3, 1);

        Attributes attributes1 = new Attributes();
        attributes1.setSku(new SkuInfo(sku1));
        attributes1.setPrice(100.0);
        attributes1.setSalePrice(90.0);

        Attributes attributes2 = new Attributes();
        attributes2.setSku(new SkuInfo(sku2));
        attributes2.setPrice(50.0);

        Attributes attributes3 = new Attributes();
        attributes3.setSku(new SkuInfo(sku3));
        attributes3.setPrice(200.0);

        when(securityUtil.getCurrentUsername()).thenReturn(username);
        when(userRepository.findByNameOrEmail(username)).thenReturn(Optional.of(user));
        when(shoppingCartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(attributesRepository.findAllBySku_skuIn(anyList())).thenReturn(Arrays.asList(attributes1, attributes2, attributes3));
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(cart);

        // When
        Response<ShoppingCartDto> response = shoppingCartService.add(items);

        // Then
        assertNotNull(response);
        assertEquals("Cập nhật giỏ hàng thành công", response.getStatus().getMessage());

        ShoppingCartDto dto = response.getData();
        assertNotNull(dto);
        assertEquals(2, dto.getItems().size());
        assertEquals(4, dto.getTotalItems());
        assertEquals(300.0, dto.getTotalPrice());
        assertEquals(280.0, dto.getTotalSalePrice());
        assertEquals(20.0, dto.getTotalDiscount());

        verify(shoppingCartRepository).save(cart);
    }

    @Test
    void shouldThrowBusinessExceptionWhenItemsListIsNull() {
        // Given
        List<CartItemRequest> items = null;

        // When & Then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> shoppingCartService.add(items)
        );

        assertEquals("Danh sách sản phẩm không được rỗng", exception.getMessage());
        verify(securityUtil, never()).getCurrentUsername();
    }

    @Test
    void shouldDecreaseItemQuantityWhenQuantityIsNegative() {
        // Given
        String username = "testUser";
        String sku = "SKU-TEST";

        List<CartItemRequest> items = Collections.singletonList(
                new CartItemRequest(sku, -3)
        );

        User user = new User();
        user.setName(username);

        ShoppingCart cart = new ShoppingCart();
        cart.setUser(user);
        cart.setAuditInfo(new AuditInfo());
        cart.addItem(sku, 5);

        Attributes attributes = new Attributes();
        attributes.setSku(new SkuInfo(sku));
        attributes.setPrice(10.0);

        when(securityUtil.getCurrentUsername()).thenReturn(username);
        when(userRepository.findByNameOrEmail(username)).thenReturn(Optional.of(user));
        when(shoppingCartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(attributesRepository.findAllBySku_skuIn(anyList())).thenReturn(Collections.singletonList(attributes));
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(cart);

        // When
        Response<ShoppingCartDto> response = shoppingCartService.add(items);

        // Then
        assertNotNull(response);
        ShoppingCartDto dto = response.getData();
        assertEquals(1, dto.getItems().size());
        assertEquals(2, dto.getItems().get(0).getQuantity());
    }

    @Test
    void shouldCalculateCorrectlyForFreeItemsOnSale() {
        String username = "testUser";
        String sku = "SKU-FREE";
        List<CartItemRequest> items = Collections.singletonList(new CartItemRequest(sku, 1));

        User user = new User();
        user.setName(username);

        ShoppingCart cart = new ShoppingCart();
        cart.setUser(user);
        cart.setAuditInfo(new AuditInfo());

        Attributes attributes = new Attributes();
        attributes.setSku(new SkuInfo(sku));
        attributes.setPrice(10.0);
        attributes.setSalePrice(0.0); // Free on sale

        Attributes attributesNormal = new Attributes();
        attributesNormal.setSku(new SkuInfo("SKU-NORMAL"));
        attributesNormal.setPrice(15.0);
        attributesNormal.setSalePrice(0.0); // Not on sale

        cart.addItem("SKU-NORMAL", 1);
        items = Arrays.asList(new CartItemRequest(sku, 1), new CartItemRequest("SKU-NORMAL", 1));

        when(securityUtil.getCurrentUsername()).thenReturn(username);
        when(userRepository.findByNameOrEmail(username)).thenReturn(Optional.of(user));
        when(shoppingCartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(attributesRepository.findAllBySku_skuIn(anyList())).thenReturn(Arrays.asList(attributes, attributesNormal));
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(cart);

        Response<ShoppingCartDto> response = shoppingCartService.add(items);

        ShoppingCartDto dto = response.getData();
        assertEquals(40.0, dto.getTotalPrice(), "totalPrice: 10*1 (FREE) + 15*2 (NORMAL qty=2) = 40");
        assertEquals(30.0, dto.getTotalSalePrice(), "totalSalePrice: 0*1 (FREE) + 15*2 (NORMAL has no sale) = 30");
    }

    @Test
    void shouldThrowBusinessExceptionWhenDecreasingNonExistentItem() {
        String username = "testUser";
        String sku = "SKU-NOT-EXIST";
        List<CartItemRequest> items = Collections.singletonList(new CartItemRequest(sku, -1));

        User user = new User();
        user.setName(username);

        ShoppingCart cart = new ShoppingCart();
        cart.setUser(user);
        cart.setAuditInfo(new AuditInfo());

        Attributes attributes = new Attributes();
        attributes.setSku(new SkuInfo(sku));
        attributes.setPrice(10.0);

        when(securityUtil.getCurrentUsername()).thenReturn(username);
        when(userRepository.findByNameOrEmail(username)).thenReturn(Optional.of(user));
        when(shoppingCartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(attributesRepository.findAllBySku_skuIn(anyList())).thenReturn(Collections.singletonList(attributes));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> shoppingCartService.add(items));
        assertTrue(exception.getMessage().contains("không có trong giỏ hàng") || exception instanceof BusinessException,
                "Should throw exception about item not in cart, got: " + exception.getMessage());
    }

    @Test
    void shouldPruneDanglingItemsWhenRecalculating() {
        String username = "testUser";
        String skuValid = "SKU-VALID";
        String skuDangling = "SKU-DANGLING";
        
        List<CartItemRequest> items = Collections.singletonList(new CartItemRequest(skuValid, 1));

        User user = new User();
        user.setName(username);

        ShoppingCart cart = new ShoppingCart();
        cart.setUser(user);
        cart.setAuditInfo(new AuditInfo());
        cart.addItem(skuDangling, 1);

        Attributes attributesValid = new Attributes();
        attributesValid.setSku(new SkuInfo(skuValid));
        attributesValid.setPrice(10.0);

        when(securityUtil.getCurrentUsername()).thenReturn(username);
        when(userRepository.findByNameOrEmail(username)).thenReturn(Optional.of(user));
        when(shoppingCartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(attributesRepository.findAllBySku_skuIn(anyList())).thenReturn(Collections.singletonList(attributesValid));
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(cart);

        Response<ShoppingCartDto> response = shoppingCartService.add(items);
        ShoppingCartDto dto = response.getData();

        assertEquals(1, dto.getItems().size());
        assertEquals(skuValid, dto.getItems().get(0).getSku());
    }

    @Test
    void shouldValidateQuantityRange() {
        String username = "testUser";
        String sku = "SKU-1";
        
        List<CartItemRequest> items = Collections.singletonList(new CartItemRequest(sku, 10000));

        User user = new User();
        user.setName(username);

        ShoppingCart cart = new ShoppingCart();
        cart.setUser(user);
        cart.setAuditInfo(new AuditInfo());

        when(securityUtil.getCurrentUsername()).thenReturn(username);
        when(userRepository.findByNameOrEmail(username)).thenReturn(Optional.of(user));
        when(shoppingCartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        BusinessException exception = assertThrows(BusinessException.class, () -> shoppingCartService.add(items));
        assertEquals("INVALID_QUANTITY", exception.getCode());
    }

    @Test
    void shouldHandleConcurrentCartCreation() {
        String username = "testUser";
        List<CartItemRequest> items = Collections.singletonList(new CartItemRequest("SKU", 1));

        User user = new User();
        user.setName(username);

        ShoppingCart existingCart = new ShoppingCart();
        existingCart.setUser(user);
        existingCart.setAuditInfo(new AuditInfo());

        Attributes attributes = new Attributes();
        attributes.setSku(new SkuInfo("SKU"));
        attributes.setPrice(10.0);

        when(securityUtil.getCurrentUsername()).thenReturn(username);
        when(userRepository.findByNameOrEmail(username)).thenReturn(Optional.of(user));
        
        when(shoppingCartRepository.findByUser(user))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(existingCart));
            
        when(shoppingCartRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("Constraint violation"));
        when(attributesRepository.findAllBySku_skuIn(anyList())).thenReturn(Collections.singletonList(attributes));
        when(shoppingCartRepository.save(any())).thenReturn(existingCart);

        Response<ShoppingCartDto> response = shoppingCartService.add(items);
        assertNotNull(response);
    }
}
