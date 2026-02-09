
package com.anno.ERP_SpringBoot_Experiment.service;

import com.anno.ERP_SpringBoot_Experiment.mapper.ShoppingCartMapper;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.ProductQuantity;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.model.entity.ShoppingCart;
import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.repository.AttributesRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.ShoppingCartRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.Helper;
import com.anno.ERP_SpringBoot_Experiment.service.Merchandise.ShoppingCartService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ShoppingCartDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.utils.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
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
class ShoppingCartServiceTest {

    @Mock
    private ShoppingCartRepository shoppingCartRepository;

    @Mock
    private AttributesRepository attributesRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private ShoppingCartMapper shoppingCartMapper;

    @Mock
    private Helper helper;

    @InjectMocks
    private ShoppingCartService shoppingCartService;

    @Test
    void shouldSuccessfullyProcessMultipleItemsWithDifferentQuantities() {
        // Given
        String username = "testUser";
        UUID uuid1 = UUID.randomUUID();
        UUID uuid2 = UUID.randomUUID();
        UUID uuid3 = UUID.randomUUID();
        String attributesId1 = uuid1.toString();
        String attributesId2 = uuid2.toString();
        String attributesId3 = uuid3.toString();
        
        List<ProductQuantity> items = Arrays.asList(
                new ProductQuantity(attributesId1, 5),
                new ProductQuantity(attributesId2, -2),
                new ProductQuantity(attributesId3, 0)
        );
        
        User user = new User();
        user.setName(username);
        
        ShoppingCart cart = new ShoppingCart();
        cart.setUser(user);
        cart.setItems(new ArrayList<>());
        cart.setAuditInfo(new AuditInfo());
        
        Attributes attributes1 = new Attributes();
        attributes1.setId(uuid1);
        attributes1.setName("Product 1");
        
        Attributes attributes2 = new Attributes();
        attributes2.setId(uuid2);
        attributes2.setName("Product 2");
        
        Attributes attributes3 = new Attributes();
        attributes3.setId(uuid3);
        attributes3.setName("Product 3");
        
        ShoppingCartDto expectedDto = mock(ShoppingCartDto.class);
        
        when(securityUtil.getCurrentUsername()).thenReturn(username);
        when(userRepository.findByName(username)).thenReturn(Optional.of(user));
        when(shoppingCartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(attributesRepository.findAllById(anyList())).thenReturn(Arrays.asList(attributes1, attributes2, attributes3));
        doNothing().when(helper).handleAddItem(any(), any(), any());
        doNothing().when(helper).handleDecreaseItem(any(), any(), anyString());
        doNothing().when(helper).recalculateAndUpdateTotals(any());
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(cart);
        when(shoppingCartMapper.toDto(cart)).thenReturn(expectedDto);
        
        // When
        Response<ShoppingCartDto> response = shoppingCartService.add(items);
        
        // Then
        assertNotNull(response);
        assertEquals("Cập nhật giỏ hàng thành công", response.getMessage());
        assertEquals(expectedDto, response.getData());
        verify(helper).handleAddItem(eq(cart), any(ProductQuantity.class), eq(attributes1));
        verify(helper).handleDecreaseItem(eq(cart), any(ProductQuantity.class), eq(attributesId2));
        verify(helper).recalculateAndUpdateTotals(cart);
        verify(shoppingCartRepository).save(cart);
    }
    
    @Test
    void shouldThrowBusinessExceptionWhenItemsListIsNull() {
        // Given
        List<ProductQuantity> items = null;
        
        // When & Then
        BusinessException exception = assertThrows(
            BusinessException.class,
            () -> shoppingCartService.add(items)
        );
        
        assertEquals("Danh sách sản phẩm không được rỗng", exception.getMessage());
        verify(securityUtil, never()).getCurrentUsername();
        verify(userRepository, never()).findByName(anyString());
        verify(shoppingCartRepository, never()).findByUser(any());
    }
    
    @Test
    void shouldDecreaseItemQuantityWhenQuantityIsNegative() {
        // Given
        String username = "testUser";
        UUID attributesId = UUID.randomUUID();
        String attributesIdStr = attributesId.toString();
        int negativeQuantity = -3;
        
        List<ProductQuantity> items = Collections.singletonList(
                new ProductQuantity(attributesIdStr, negativeQuantity)
        );
        
        User user = new User();
        user.setName(username);
        
        ShoppingCart cart = new ShoppingCart();
        cart.setUser(user);
        cart.setItems(new ArrayList<>());
        cart.setAuditInfo(new AuditInfo());
        
        Attributes attributes = new Attributes();
        attributes.setId(attributesId);
        attributes.setName("Test Product");
        
        ShoppingCartDto expectedDto = mock(ShoppingCartDto.class);
        
        when(securityUtil.getCurrentUsername()).thenReturn(username);
        when(userRepository.findByName(username)).thenReturn(Optional.of(user));
        when(shoppingCartRepository.findByUser(user)).thenReturn(Optional.of(cart));
        when(attributesRepository.findAllById(anyList())).thenReturn(Collections.singletonList(attributes));
        doNothing().when(helper).handleDecreaseItem(any(), any(), anyString());
        doNothing().when(helper).recalculateAndUpdateTotals(any());
        when(shoppingCartRepository.save(any(ShoppingCart.class))).thenReturn(cart);
        when(shoppingCartMapper.toDto(cart)).thenReturn(expectedDto);
        
        // When
        Response<ShoppingCartDto> response = shoppingCartService.add(items);
        
        // Then
        assertNotNull(response);
        assertEquals("Cập nhật giỏ hàng thành công", response.getMessage());
        assertEquals(expectedDto, response.getData());
        verify(helper).handleDecreaseItem(eq(cart), any(ProductQuantity.class), eq(attributesIdStr));
        verify(helper, never()).handleAddItem(any(), any(), any());
        verify(helper).recalculateAndUpdateTotals(cart);
        verify(shoppingCartRepository).save(cart);
    }
}
