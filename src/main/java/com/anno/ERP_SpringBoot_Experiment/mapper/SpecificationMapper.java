package com.anno.ERP_SpringBoot_Experiment.mapper;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.Specificationa;
import com.anno.ERP_SpringBoot_Experiment.service.dto.SpecificationDto;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface SpecificationMapper extends EntityMapper<SpecificationDto, Specificationa> {
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Specificationa partialUpdate(SpecificationDto specificationDto, @MappingTarget Specificationa specificationa);
}