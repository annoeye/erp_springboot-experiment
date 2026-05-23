package com.anno.ERP_SpringBoot_Experiment.mapper;

import com.anno.ERP_SpringBoot_Experiment.model.embedded.Promotion;
import com.anno.ERP_SpringBoot_Experiment.service.dto.PromotionDto;
import org.mapstruct.Mapper;

@Mapper(builder = @org.mapstruct.Builder(disableBuilder = true), componentModel = "spring")
public interface PromotionMapper extends EntityMapper<PromotionDto, Promotion> {
}
