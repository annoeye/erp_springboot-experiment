package com.anno.ERP_SpringBoot_Experiment.web.rest;

import com.anno.ERP_SpringBoot_Experiment.service.VNPayService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.PaymentDTO;
import com.anno.ERP_SpringBoot_Experiment.service.dto.PaymentQueryDTO;
import com.anno.ERP_SpringBoot_Experiment.service.dto.PaymentRefundDTO;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/payments")
public class PaymentController {

    private final VNPayService vnPayService;
    @PostMapping("/create_payment_url")
    public Response<String> createPayment(@RequestBody PaymentDTO paymentRequest, HttpServletRequest request) {
        try {
            String paymentUrl = vnPayService.createPaymentUrl(paymentRequest, request);
            return Response.ok(paymentUrl);
        } catch (Exception e) {
            throw new BusinessException("Lỗi khi tạo URL thanh toán: ", e.getMessage());
        }
    }

    @PostMapping("/query")
    public Response<String> queryTransaction(@RequestBody PaymentQueryDTO paymentQueryDTO, HttpServletRequest request) {
        try {
            String result = vnPayService.queryTransaction(paymentQueryDTO, request);
            return Response.ok(result);
        } catch (Exception e) {
            throw new BusinessException("Lỗi khi truy vấn giao dịch: " + e.getMessage());
        }
    }

    @PostMapping("/refund")
    public Response<String> refundTransaction(@Valid @RequestBody PaymentRefundDTO paymentRefundDTO) {
        try {
            String response = vnPayService.refundTransaction(paymentRefundDTO);
            return Response.ok(response);
        } catch (Exception e) {
            throw new BusinessException("Lỗi khi hoàn tiền: " + e.getMessage());
        }
    }
}
