package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CompleteOrderRequest {
    private String orderId;
    private String completionInfo;
    private LocalDateTime completedAt;
}
