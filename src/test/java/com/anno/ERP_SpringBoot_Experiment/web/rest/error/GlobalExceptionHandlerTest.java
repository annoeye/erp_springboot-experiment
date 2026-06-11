package com.anno.ERP_SpringBoot_Experiment.web.rest.error;

import com.anno.ERP_SpringBoot_Experiment.component.JwtAuthenticationFilter;
import com.anno.ERP_SpringBoot_Experiment.common.interceptor.SmartRequestInterceptor;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Unit tests for GlobalExceptionHandler.
//
// Uses a minimal test controller to trigger each exception path.
@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@org.springframework.context.annotation.Import(GlobalExceptionHandlerTest.TestController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("GlobalExceptionHandler Unit Tests")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TestService testService;

    @MockitoBean
    private com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil securityUtil;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private SmartRequestInterceptor smartRequestInterceptor;

    @org.junit.jupiter.api.BeforeEach
    void setUp() throws Exception {
        org.mockito.Mockito.when(smartRequestInterceptor.preHandle(
                org.mockito.Mockito.any(),
                org.mockito.Mockito.any(),
                org.mockito.Mockito.any()
        )).thenReturn(true);
    }

    @RestController
    @RequestMapping("/api/test")
    static class TestController {

        private final TestService testService;

        TestController(TestService testService) {
            this.testService = testService;
        }

        @GetMapping("/business-error")
        String throwBusinessError() {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "Chỉ còn 5 sản phẩm")
                    .with("availableStock", 5)
                    .with("requestedQuantity", 10);
        }

        @GetMapping("/server-error")
        String throwServerError() {
            throw new RuntimeException("Unexpected database failure");
        }
    }

    static class TestService {
        String ping() {
            return "pong";
        }
    }

    // ========== BusinessException Tests ==========

    @Test
    @DisplayName("Should return 400 with ProblemDetail for BusinessException")
    void handleBusinessException_returnsProblemDetail() throws Exception {
        mockMvc.perform(get("/api/test/business-error")
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Không đủ hàng"))
                .andExpect(jsonPath("$.detail").value("Chỉ còn 5 sản phẩm"))
                .andExpect(jsonPath("$.errorCode").value("INSUFFICIENT_STOCK"))
                .andExpect(jsonPath("$.details.availableStock").value(5))
                .andExpect(jsonPath("$.details.requestedQuantity").value(10));
    }

    // ========== Fallback Tests ==========

    @Test
    @DisplayName("Should return 500 for unhandled RuntimeException")
    void handleUnhandledException_returns500() throws Exception {
        mockMvc.perform(get("/api/test/server-error")
                        .accept(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"));
    }
}
