package com.anno.ERP_SpringBoot_Experiment.mapper;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Order;
import com.anno.ERP_SpringBoot_Experiment.service.dto.OrderDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.OrderAdminResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.OrderUserResponse;
import org.mapstruct.*;
import java.util.List;

@Mapper(builder = @org.mapstruct.Builder(disableBuilder = true), 
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {OrderItemMapper.class}
)
public interface OrderMapper extends EntityMapper<OrderDto, Order> {

    @Named("toDto")
    @Mapping(target = "currentStatus", expression = "java(order.getStatus() != null && !order.getStatus().isEmpty() ? order.getStatus().get(order.getStatus().size() - 1) : null)")
    @Mapping(target = "currentStatusDescription", expression = "java(order.getStatus() != null && !order.getStatus().isEmpty() ? order.getStatus().get(order.getStatus().size() - 1).getDescription() : null)")
    OrderDto toDto(Order order);

    @IterableMapping(qualifiedByName = "toDto")
    List<OrderDto> toDto(List<Order> entityList);

    @Named("toAdminResponse")
    @Mapping(target = "currentStatus", expression = "java(order.getStatus() != null && !order.getStatus().isEmpty() ? order.getStatus().get(order.getStatus().size() - 1) : null)")
    OrderAdminResponse toAdminResponse(Order order);

    @Named("toUserResponse")
    @Mapping(target = "currentStatus", expression = "java(order.getStatus() != null && !order.getStatus().isEmpty() ? order.getStatus().get(order.getStatus().size() - 1) : null)")
    OrderUserResponse toUserResponse(Order order);

    OrderAdminResponse toAdminResponseFromDto(OrderDto orderDto);

    OrderUserResponse toUserResponseFromDto(OrderDto orderDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Order partialUpdate(OrderDto orderDto, @MappingTarget Order order);
}
