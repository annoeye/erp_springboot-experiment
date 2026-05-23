package com.anno.ERP_SpringBoot_Experiment.service.dto.response;

import com.anno.ERP_SpringBoot_Experiment.service.dto.OrderDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class OrderAdminResponse extends OrderDto {
    private String adminNotes;
}
