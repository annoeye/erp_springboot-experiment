//package com.anno.ERP_SpringBoot_Experiment.web.rest;
//
//import com.anno.ERP_SpringBoot_Experiment.service.PaymentService.VNPayService;
//import com.anno.ERP_SpringBoot_Experiment.service.dto.PaymentVNPayDTO;
//import com.anno.ERP_SpringBoot_Experiment.service.dto.PaymentQueryDTO;
//import com.anno.ERP_SpringBoot_Experiment.service.dto.PaymentVNPayRefundDTO;
//import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
//import com.anno.ERP_SpringBoot_Experiment.utils.HttpUtils;
//import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.validation.Valid;
//import lombok.RequiredArgsConstructor;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Map;
//
//@RestController
//@RequiredArgsConstructor
//@RequestMapping("/payments")
//public class PaymentController {
//
//    @Autowired
//    private HttpUtils httpUtils;
//
//    private final VNPayService vnPayService;
//
//    @PostMapping("/create_payment_url")
//    public Response<String> createPayment(@RequestBody PaymentVNPayDTO paymentRequest, HttpServletRequest request) {
//        try {
//            String paymentUrl = vnPayService.createPaymentUrl(paymentRequest, request);
//            return Response.ok(paymentUrl);
//        } catch (Exception e) {
//            throw new BusinessException("Lỗi khi tạo URL thanh toán: ", e.getMessage());
//        }
//    }
//
//    @PostMapping("/query")
//    public Response<String> queryTransaction(@RequestBody PaymentQueryDTO paymentQueryDTO, HttpServletRequest request) {
//        try {
//            String result = vnPayService.queryTransaction(paymentQueryDTO, request);
//            return Response.ok(result);
//        } catch (Exception e) {
//            throw new BusinessException("Lỗi khi truy vấn giao dịch: " + e.getMessage());
//        }
//    }
//
//    @PostMapping("/refund")
//    public Response<String> refundTransaction(@Valid @RequestBody PaymentVNPayRefundDTO paymentVNPayRefundDTO) {
//        try {
//            String response = vnPayService.refundTransaction(paymentVNPayRefundDTO);
//            return Response.ok(response);
//        } catch (Exception e) {
//            throw new BusinessException("Lỗi khi hoàn tiền: " + e.getMessage());
//        }
//    }
//
//    @GetMapping("/vnpay-return")
//    public ResponseEntity<?> paymentReturn(@RequestParam Map<String, String> allParams, HttpServletRequest request) {
//        // Parse VNPay response
//        PaymentVNPayRefundDTO paymentVNPayRefundDTO = new PaymentVNPayRefundDTO();
//        paymentVNPayRefundDTO.setAmount(Long.valueOf(allParams.get("vnp_Amount")));
//        paymentVNPayRefundDTO.setBankCode(allParams.get("vnp_BankCode"));
//        paymentVNPayRefundDTO.setBankTranNo(allParams.get("vnp_BankTranNo"));
//        paymentVNPayRefundDTO.setTransactionType(allParams.get("vnp_CardType"));
//        paymentVNPayRefundDTO.setOrderId(allParams.get("vnp_OrderInfo").substring(20));
//        paymentVNPayRefundDTO.setPayDate(allParams.get("vnp_PayDate"));
//        paymentVNPayRefundDTO.setResponseCode(Integer.parseInt(allParams.get("vnp_ResponseCode")));
//        paymentVNPayRefundDTO.setTmnCode(allParams.get("vnp_TmnCode"));
//        paymentVNPayRefundDTO.setTransactionNo(allParams.get("vnp_TransactionNo"));
//        paymentVNPayRefundDTO.setTransactionStatus(Integer.parseInt(allParams.get("vnp_TransactionStatus")));
//        paymentVNPayRefundDTO.setTxnRef(allParams.get("vnp_TxnRef"));
//        paymentVNPayRefundDTO.setSecureHash(allParams.get("vnp_SecureHash"));
//        paymentVNPayRefundDTO.setIpAddress(httpUtils.getIpAddress(request));
//
//        // Log toàn bộ params
//        System.out.println("VNPay Callback Params: " + allParams);
//
//        // ✅ Kiểm tra thanh toán thành công
//        // ResponseCode = 00 và TransactionStatus = 00 => Thanh toán thành công
//        if ("00".equals(allParams.get("vnp_ResponseCode")) &&
//                "00".equals(allParams.get("vnp_TransactionStatus"))) {
//
//            try {
//                // TODO: Gọi BillService để tạo Bill
//                // Bạn cần inject BillService vào controller này
//                // billService.addBill(createBillRequest, paymentDto);
//
//                System.out.println("✅ Payment SUCCESS - Bill creation logic should be called here");
//                System.out.println("Order ID: " + paymentVNPayRefundDTO.getOrderId());
//
//                return ResponseEntity.ok(Response.builder()
//                        .status(200)
//                        .message("Thanh toán thành công! Hóa đơn đang được tạo.")
//                        .data(paymentVNPayRefundDTO)
//                        .build());
//
//            } catch (Exception e) {
//                System.err.println("❌ Error creating Bill: " + e.getMessage());
//                return ResponseEntity.status(500).body(Response.builder()
//                        .status(500)
//                        .message("Thanh toán thành công nhưng lỗi khi tạo hóa đơn: " + e.getMessage())
//                        .build());
//            }
//        } else {
//            // ❌ Thanh toán thất bại
//            System.out.println("❌ Payment FAILED - Response Code: " + allParams.get("vnp_ResponseCode"));
//            return ResponseEntity.ok(Response.builder()
//                    .status(400)
//                    .message("Thanh toán thất bại. Mã lỗi: " + allParams.get("vnp_ResponseCode"))
//                    .data(paymentVNPayRefundDTO)
//                    .build());
//        }
//    }
//}
