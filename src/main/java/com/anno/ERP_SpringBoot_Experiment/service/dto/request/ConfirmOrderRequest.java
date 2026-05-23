package com.anno.ERP_SpringBoot_Experiment.service.dto.request;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConfirmOrderRequest {
    private String orderId;
    private String confirmationInfo;
    private LocalDateTime confirmedAt;
    private String confirmedBy;
}
