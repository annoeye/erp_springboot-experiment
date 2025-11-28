
package com.anno.ERP_SpringBoot_Experiment.service;

import com.anno.ERP_SpringBoot_Experiment.config.VNPayConfig;
import com.anno.ERP_SpringBoot_Experiment.service.dto.PaymentDTO;
import com.anno.ERP_SpringBoot_Experiment.utils.VNPayUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VNPayServiceTest {

    @Mock
    private VNPayConfig vnPayConfig;

    @Mock
    private VNPayUtils vnPayUtils;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    private VNPayService vnPayService;

    @Test
    void shouldCreatePaymentUrlWithValidPaymentDTO() {
        // ===== ARRANGE (Chuẩn bị dữ liệu) =====
        // Tạo đối tượng thanh toán với:
        // - Số tiền: 100,000 VNĐ
        // - Ngân hàng: NCB
        // - Ngôn ngữ: Tiếng Việt
        PaymentDTO paymentDTO = new PaymentDTO();
        paymentDTO.setAmount(100000L);
        paymentDTO.setBankCode("NCB");
        paymentDTO.setLanguage("vn");

        // Mock các phương thức phụ thuộc để trả về giá trị giả:
        when(vnPayUtils.getRandomNumber(8)).thenReturn("12345678"); // Mã giao dịch giả
        when(vnPayUtils.getIpAddress(httpServletRequest)).thenReturn("127.0.0.1"); // IP giả
        when(vnPayConfig.getVnpTmnCode()).thenReturn("TESTCODE"); // Mã terminal giả
        when(vnPayConfig.getVnpReturnUrl()).thenReturn("http://localhost:8080/return"); // URL return giả
        when(vnPayConfig.getVnpPayUrl()).thenReturn("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"); // URL VNPay giả
        when(vnPayConfig.getSecretKey()).thenReturn("TESTSECRETKEY"); // Secret key giả
        when(vnPayUtils.hmacSHA512(anyString(), anyString())).thenReturn("mockSecureHash"); // Hash giả

        // ===== ACT (Thực thi hành động) =====
        // Gọi phương thức cần test: tạo URL thanh toán
        String paymentUrl = vnPayService.createPaymentUrl(paymentDTO, httpServletRequest);

        // ===== ASSERT (Kiểm tra kết quả) =====
        // 1. Kiểm tra URL không null
        assertNotNull(paymentUrl);
        
        // 2. Kiểm tra URL bắt đầu đúng với domain VNPay
        assertTrue(paymentUrl.startsWith("https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?"));
        
        // 3. Kiểm tra số tiền đã được nhân 100 (100,000 * 100 = 10,000,000)
        // VNPay yêu cầu số tiền tính bằng đơn vị nhỏ nhất
        assertTrue(paymentUrl.contains("vnp_Amount=10000000"));
        
        // 4. Kiểm tra mã ngân hàng có trong URL
        assertTrue(paymentUrl.contains("vnp_BankCode=NCB"));
        
        // 5. Kiểm tra ngôn ngữ có trong URL
        assertTrue(paymentUrl.contains("vnp_Locale=vn"));
        
        // 6. Kiểm tra mã giao dịch có trong URL
        assertTrue(paymentUrl.contains("vnp_TxnRef=12345678"));
        
        // 7. Kiểm tra chữ ký bảo mật có trong URL
        assertTrue(paymentUrl.contains("vnp_SecureHash=mockSecureHash"));
        
        // ===== VERIFY (Xác minh các phương thức đã được gọi) =====
        // Đảm bảo các phương thức helper đã được gọi đúng
        verify(vnPayUtils).getRandomNumber(8); // Đã tạo mã giao dịch 8 ký tự
        verify(vnPayUtils).getIpAddress(httpServletRequest); // Đã lấy IP address
        verify(vnPayUtils).hmacSHA512(anyString(), anyString()); // Đã tạo chữ ký bảo mật
    }
}
