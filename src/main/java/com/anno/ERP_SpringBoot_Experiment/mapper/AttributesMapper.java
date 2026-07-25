package com.anno.ERP_SpringBoot_Experiment.mapper;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Attributes;
import com.anno.ERP_SpringBoot_Experiment.service.dto.AttributesDto;
import org.mapstruct.*;

@Mapper(
        builder = @org.mapstruct.Builder(disableBuilder = true),
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        componentModel = MappingConstants.ComponentModel.SPRING,
        uses = SpecificationMapper.class)
public interface AttributesMapper extends EntityMapper<AttributesDto, Attributes> {
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Attributes partialUpdate(AttributesDto xAttributesDto, @MappingTarget Attributes attributes);
}
