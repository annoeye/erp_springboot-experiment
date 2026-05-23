package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import lombok.Data;
import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;

@Data
public class TransitionOrderRequest {
    private String orderId;
    private OrderStatus targetStatus;
    private OrderStatus newStatus;
    private String note;
    private String shipperId;
}
