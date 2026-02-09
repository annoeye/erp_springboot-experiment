package com.anno.ERP_SpringBoot_Experiment.service.interfaces;

import com.anno.ERP_SpringBoot_Experiment.service.dto.PaymentDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateBillRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;

public interface iBill {
    Response<?> addBill(CreateBillRequest request, PaymentDto paymentDto);
}
