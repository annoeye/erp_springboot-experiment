package com.anno.ERP_SpringBoot_Experiment.mapper;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Bill;
import com.anno.ERP_SpringBoot_Experiment.service.dto.BillDto;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface BillMapper extends EntityMapper<BillDto, Bill> {
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Bill partialUpdate(BillDto billDto, @MappingTarget Bill bill);
}
