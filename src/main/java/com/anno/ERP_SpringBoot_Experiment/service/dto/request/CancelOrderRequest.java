package com.anno.ERP_SpringBoot_Experiment.service.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CancelOrderRequest {

    @NotNull(message = "Order ID không được để trống")
    String orderId;

    @NotBlank(message = "Lý do hủy không được để trống")
    String cancellationReason;
}
