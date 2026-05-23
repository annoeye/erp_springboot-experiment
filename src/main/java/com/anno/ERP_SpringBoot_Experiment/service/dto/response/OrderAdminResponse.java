package com.anno.ERP_SpringBoot_Experiment.service.dto.response;

import lombok.Data;
import java.util.List;
import com.anno.ERP_SpringBoot_Experiment.service.dto.OrderDto;

@Data
public class OrderAdminResponse extends OrderDto {
    private String adminNotes;
}
