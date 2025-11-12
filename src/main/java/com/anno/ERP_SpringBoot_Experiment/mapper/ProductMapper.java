package com.anno.ERP_SpringBoot_Experiment.mapper;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Product;
import com.anno.ERP_SpringBoot_Experiment.service.dto.ProductDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateProductRequest;
import org.mapstruct.*;

@Mapper(
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {CategoryMapper.class}
)
public interface ProductMapper extends EntityMapper<ProductDto, Product> {
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Product partialUpdate(ProductDto productDto, @MappingTarget Product product);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "skuInfo", ignore = true)
    @Mapping(target = "auditInfo", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "attributes", ignore = true)
    void updateFromRequest(UpdateProductRequest request, @MappingTarget Product product);
}