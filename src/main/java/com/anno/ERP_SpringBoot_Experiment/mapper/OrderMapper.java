package com.anno.ERP_SpringBoot_Experiment.mapper;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Order;
import com.anno.ERP_SpringBoot_Experiment.service.dto.OrderDto;
import org.mapstruct.*;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {OrderItemMapper.class}
)
public interface OrderMapper extends EntityMapper<OrderDto, Order> {

    @Mapping(target = "currentStatus", expression = "java(order.getCurrentStatus())")
    OrderDto toDto(Order order);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Order partialUpdate(OrderDto orderDto, @MappingTarget Order order);
}
