package com.anno.ERP_SpringBoot_Experiment.web.rest.impl;

import com.anno.ERP_SpringBoot_Experiment.service.dto.ShoppingCartDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CartItemRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iShoppingCart;
import com.anno.ERP_SpringBoot_Experiment.web.rest.ShoppingCartController;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ShoppingCartControllerImpl implements ShoppingCartController {

    private final iShoppingCart shoppingCartService;

    @Override
    public Response<ShoppingCartDto> getCart() {
        return shoppingCartService.getCart();
    }

    @Override
    public Response<ShoppingCartDto> addToCart(final List<CartItemRequest> items) {
        return shoppingCartService.add(items);
    }

    @Override
    public Response<ShoppingCartDto> removeFromCart(final List<String> skus) {
        return shoppingCartService.remove(skus);
    }

    @Override
    public Response<ShoppingCartDto> clearCart() {
        return shoppingCartService.clearCart();
    }
}
