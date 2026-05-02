package com.anno.ERP_SpringBoot_Experiment.mapper;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Payment;
import com.anno.ERP_SpringBoot_Experiment.service.dto.PaymentDto;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING, uses = {OrderMapper.class})
public interface PaymentMapper extends EntityMapper<PaymentDto, Payment> {
}
