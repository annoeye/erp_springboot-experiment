package com.anno.ERP_SpringBoot_Experiment.service;

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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ShoppingCartEmpiricalTest {

    @Mock private ShoppingCartRepository shoppingCartRepository;
    @Mock private AttributesRepository attributesRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityUtil securityUtil;
    
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
    void testFreeItemPricing() {
        User user = new User(); user.setName("test");
        ShoppingCart cart = new ShoppingCart(); cart.setUser(user);
        
        Attributes attr = new Attributes();
        attr.setSku(new SkuInfo("SKU-1"));
        attr.setPrice(100.0);
        attr.setSalePrice(0.0); // Free
        
        when(securityUtil.getCurrentUsername()).thenReturn("test");
        when(userRepository.findByNameOrEmail("test")).thenReturn(Optional.of(user));
        when(shoppingCartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(attributesRepository.findAllBySku_skuIn(anyList())).thenReturn(List.of(attr));
        when(shoppingCartRepository.save(any())).thenReturn(cart);

        Response<ShoppingCartDto> res = shoppingCartService.add(List.of(new CartItemRequest("SKU-1", 1)));
        
        assertEquals(0.0, res.getData().getTotalSalePrice(), "Free item should cost 0.0");
        assertEquals(100.0, res.getData().getTotalPrice(), "Total price should still be 100.0");
    }

    @Test
    void testNonExistentSkuDecreaseThrowsBusinessException() {
        User user = new User(); user.setName("test");
        ShoppingCart cart = new ShoppingCart(); cart.setUser(user);
        // Empty cart
        
        Attributes attr = new Attributes();
        attr.setSku(new SkuInfo("SKU-1"));
        
        when(securityUtil.getCurrentUsername()).thenReturn("test");
        when(userRepository.findByNameOrEmail("test")).thenReturn(Optional.of(user));
        when(shoppingCartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(attributesRepository.findAllBySku_skuIn(anyList())).thenReturn(List.of(attr));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            shoppingCartService.add(List.of(new CartItemRequest("SKU-1", -1)));
        });
        
        assertEquals("Sản phẩm SKU-1 không có trong giỏ hàng", ex.getMessage());
    }

    @Test
    void testDanglingItemRemoval() {
        User user = new User(); user.setName("test");
        ShoppingCart cart = new ShoppingCart(); cart.setUser(user);
        cart.setAuditInfo(new com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo());
        cart.addItem("DANGLING-SKU", 5);
        
        // Attr for new item
        Attributes attr = new Attributes();
        attr.setSku(new SkuInfo("NEW-SKU"));
        attr.setPrice(10.0);
        
        when(securityUtil.getCurrentUsername()).thenReturn("test");
        when(userRepository.findByNameOrEmail("test")).thenReturn(Optional.of(user));
        when(shoppingCartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        // Only returns NEW-SKU. DANGLING-SKU is not found in db.
        when(attributesRepository.findAllBySku_skuIn(anyList())).thenReturn(List.of(attr));
        when(shoppingCartRepository.save(any())).thenReturn(cart);

        Response<ShoppingCartDto> res = shoppingCartService.add(List.of(new CartItemRequest("NEW-SKU", 1)));
        
        // Assert dangling is removed
        boolean hasDangling = res.getData().getItems().stream().anyMatch(i -> i.getSku().equals("DANGLING-SKU"));
        assertFalse(hasDangling, "Dangling items should be removed");
    }

    @Test
    void testExtremeQuantities() {
        User user = new User(); user.setName("test");
        ShoppingCart cart = new ShoppingCart(); cart.setUser(user);
        
        Attributes attr = new Attributes();
        attr.setSku(new SkuInfo("SKU-1"));
        
        when(securityUtil.getCurrentUsername()).thenReturn("test");
        when(userRepository.findByNameOrEmail("test")).thenReturn(Optional.of(user));
        when(shoppingCartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(attributesRepository.findAllBySku_skuIn(anyList())).thenReturn(List.of(attr));

        // Test MAX_VALUE — exceeds limit 9999 → should throw BusinessException
        assertThrows(RuntimeException.class, () -> {
            shoppingCartService.add(List.of(new CartItemRequest("SKU-1", Integer.MAX_VALUE)));
        });
        
        // Test MIN_VALUE — below limit -9999 → should throw BusinessException
        assertThrows(RuntimeException.class, () -> {
            shoppingCartService.add(List.of(new CartItemRequest("SKU-1", Integer.MIN_VALUE)));
        });
    }

    @Test
    void testRaceConditionLazyInitialization() {
        User user = new User(); user.setName("test");
        
        when(securityUtil.getCurrentUsername()).thenReturn("test");
        when(userRepository.findByNameOrEmail("test")).thenReturn(Optional.of(user));
        
        // Simulate two threads hitting getCart. Both see findByUser -> empty.
        when(shoppingCartRepository.findByUser(user))
            .thenReturn(Optional.empty()) // First attempt by T1
            .thenReturn(Optional.empty()); // First attempt by T2
            // Let's assume T1 saves successfully, T2 gets DataIntegrityViolationException.
            
        when(shoppingCartRepository.saveAndFlush(any()))
            .thenThrow(new DataIntegrityViolationException("Duplicate user_id"));
            
        // When T2 gets the exception, it does another findByUser.
        // Wait, the mock needs to return Optional.empty() for that too if T1's commit hasn't been read!
        // The code in ShoppingCartService:
        /*
            catch (DataIntegrityViolationException e) {
                cart = shoppingCartRepository.findByUser(user)
                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR, "Lỗi khi lấy giỏ hàng"));
            }
        */
        
        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            shoppingCartService.getCart();
        });
        assertTrue(ex.getMessage().contains("lấy giỏ hàng") || ex.getMessage().contains("cart") || ex.getMessage().contains("Error"),
                "Should throw with cart error message, got: " + ex.getMessage());
    }
}
