package com.anno.ERP_SpringBoot_Experiment.mapper;

import com.anno.ERP_SpringBoot_Experiment.model.entity.ShoppingCart;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ShoppingCartDto;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface ShoppingCartMapper extends EntityMapper<ShoppingCartDto, ShoppingCart>{
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    ShoppingCart partialUpdate(ShoppingCartDto shoppingCartDto, @MappingTarget ShoppingCart shoppingCart);
}
