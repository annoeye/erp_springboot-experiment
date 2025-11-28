package com.anno.ERP_SpringBoot_Experiment.service.interfaces;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.ProductQuantity;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ShoppingCartDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;

import java.util.List;

public interface iShoppingCart {

    Response<ShoppingCartDto> add(final List<ProductQuantity> items);
    Response<ShoppingCartDto> remove(final List<String> attributesIds);
}
