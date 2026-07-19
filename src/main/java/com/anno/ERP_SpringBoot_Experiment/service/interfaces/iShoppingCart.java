package com.anno.ERP_SpringBoot_Experiment.service.interfaces;

import com.anno.ERP_SpringBoot_Experiment.service.dto.ShoppingCartDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CartItemRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;

import java.util.List;

public interface iShoppingCart {

    Response<ShoppingCartDto> getCart();
    Response<ShoppingCartDto> add(final List<CartItemRequest> items);
    Response<ShoppingCartDto> remove(final List<String> skus);
    Response<ShoppingCartDto> clearCart();
}
