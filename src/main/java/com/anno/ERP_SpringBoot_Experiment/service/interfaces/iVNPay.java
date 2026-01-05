package com.anno.ERP_SpringBoot_Experiment.service.interfaces;

import com.anno.ERP_SpringBoot_Experiment.service.dto.PaymentVNPayDTO;
import com.anno.ERP_SpringBoot_Experiment.service.dto.PaymentQueryDTO;
import com.anno.ERP_SpringBoot_Experiment.service.dto.PaymentVNPayRefundDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

public interface iVNPay {
    String createPaymentUrl(PaymentVNPayDTO paymentRequest, HttpServletRequest request);
    String queryTransaction(PaymentQueryDTO paymentQueryDTO, HttpServletRequest request) throws IOException;
    String refundTransaction(PaymentVNPayRefundDTO refundDTO) throws IOException;
}
