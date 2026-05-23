package com.anno.ERP_SpringBoot_Experiment.service.dto.request;
import lombok.Data;
@Data
public class UpdateShippingRequest {
    private String orderId;
    private String shippingMethod;
    private String shippingInfo;
}
