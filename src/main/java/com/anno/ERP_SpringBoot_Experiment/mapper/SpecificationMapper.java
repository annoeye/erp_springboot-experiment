package com.anno.ERP_SpringBoot_Experiment.mapper;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.Specification;
import com.anno.ERP_SpringBoot_Experiment.service.dto.SpecificationDto;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface SpecificationMapper extends EntityMapper<SpecificationDto, Specification> {
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Specification partialUpdate(SpecificationDto specificationDto, @MappingTarget Specification specification);
}