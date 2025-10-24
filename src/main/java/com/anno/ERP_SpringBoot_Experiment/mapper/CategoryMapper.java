package com.anno.ERP_SpringBoot_Experiment.mapper;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.SkuInfo;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Category;
import com.anno.ERP_SpringBoot_Experiment.service.dto.CategoryDto;
import org.mapstruct.*;

@Mapper(config = DefaultConfigMapper.class)
public interface CategoryMapper extends EntityMapper<CategoryDto, Category> {

    // 1. Hàm toDto của bạn đã đúng
    @Override
    @Mapping(source = "skuInfo.SKU", target = "sku")
    CategoryDto toDto(Category category);


    // 2. Sửa lỗi cho hàm toEntity
    @Override
    @BeanMapping(unmappedTargetPolicy = ReportingPolicy.IGNORE) // Bỏ qua id, auditInfo
    @Mapping(target = "name", ignore = true)
    // <-- Bỏ qua "name" vì Entity không có
    Category toEntity(CategoryDto dto);


    // 3. Dạy MapStruct cách map SkuInfo <-> String (dùng tên SKU)

    default String mapSkuInfoToString(SkuInfo skuInfo) {
        if (skuInfo == null) {
            return null;
        }
        return skuInfo.getSKU(); // <-- Dùng getSKU()
    }

    default SkuInfo mapStringToSkuInfo(String skuString) {
        if (skuString == null) {
            return null;
        }
        SkuInfo skuInfo = new SkuInfo();
        skuInfo.setSKU(skuString); // <-- Dùng setSKU()
        return skuInfo;
    }

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Category partialUpdate(CategoryDto categoryDto, @MappingTarget Category category);
}