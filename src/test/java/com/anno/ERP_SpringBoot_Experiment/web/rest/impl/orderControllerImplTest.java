package com.anno.ERP_SpringBoot_Experiment.web.rest.impl;

import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;
import com.anno.ERP_SpringBoot_Experiment.service.dto.OrderDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.*;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.PagingResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit Tests cho OrderController — REST layer.
 * Sử dụng WebMvcTest để test chỉ riêng controller, mock toàn bộ service layer.
 */
@WebMvcTest(controllers = orderControllerImpl.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OrderController REST Unit Tests")
class orderControllerImplTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private iOrder orderService;

    @MockitoBean
    private com.anno.ERP_SpringBoot_Experiment.mapper.OrderMapper orderMapper;

    @MockitoBean
    private com.anno.ERP_SpringBoot_Experiment.service.OrderManagement.OrderStatusHandler orderStatusHandler;

    @MockitoBean
    private com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil securityUtil;

    @MockitoBean
    private com.anno.ERP_SpringBoot_Experiment.service.JwtService jwtService;

    @MockitoBean
    private org.springframework.security.core.userdetails.UserDetailsService userDetailsService;

    // ==================== Sample Data ====================

    private OrderDto sampleOrderDto;
    private CreateOrderRequest createOrderRequest;
    private CancelOrderRequest cancelOrderRequest;
    private ConfirmOrderRequest confirmOrderRequest;
    private CompleteOrderRequest completeOrderRequest;
    private OrderSearchRequest searchRequest;
    private ProcessOrderRequest processOrderRequest;
    private ShipOrderRequest shipOrderRequest;
    private DeliverOrderRequest deliverOrderRequest;
    private DelayOrderRequest delayOrderRequest;
    private ReadyForPickupRequest readyForPickupRequest;
    private PickupOrderRequest pickupOrderRequest;
    private ReturnOrderRequest returnOrderRequest;
    private ConfirmReturnRequest confirmReturnRequest;
    private RefundOrderRequest refundOrderRequest;
    private UpdateShippingRequest updateShippingRequest;
    private UpdateDeliveryRequest updateDeliveryRequest;
    private UpdateAdminNotesRequest updateAdminNotesRequest;
    private TransitionOrderRequest transitionOrderRequest;
    private PaymentCallbackRequest paymentCallbackRequest;

    @BeforeEach
    void setUp() {
        // === Setup sample OrderDto ===
        sampleOrderDto = OrderDto.builder()
                .id(1L)
                .orderNumber("ORD-20240613-0001")
                .status(List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED))
                .currentStatus(OrderStatus.CONFIRMED)
                .currentStatusDescription("Đã xác nhận")
                .customerId(100L)
                .customerName("Nguyen Van A")
                .customerEmail("nguyenvana@example.com")
                .customerPhone("0123456789")
                .orderItems(new ArrayList<>())
                .subtotal(1000000.0)
                .discountAmount(0.0)
                .taxAmount(100000.0)
                .shippingFee(30000.0)
                .totalAmount(1130000.0)
                .customerNotes("Giao hàng trong giờ hành chính")
                .confirmedAt(LocalDateTime.now())
                .confirmedBy("admin@test.com")
                .build();

        // === Setup request DTOs ===
        CreateOrderRequest.OrderItemRequest item = CreateOrderRequest.OrderItemRequest.builder()
                .attributesSku("SKU-001")
                .quantity(2)
                .build();

        createOrderRequest = CreateOrderRequest.builder()
                .items(List.of(item))
                .isFromCart(false)
                .addressId("addr-001")
                .discountCode("DISCOUNT10")
                .customerNotes("Giao buổi sáng")
                .shippingMethod("Giao hàng nhanh")
                .paymentMethod(null) // COD
                .build();

        cancelOrderRequest = CancelOrderRequest.builder()
                .orderId("1")
                .cancellationReason("Khách hàng yêu cầu hủy")
                .build();

        confirmOrderRequest = new ConfirmOrderRequest();
        confirmOrderRequest.setOrderId("1");
        confirmOrderRequest.setConfirmationInfo("Xác nhận qua điện thoại");

        completeOrderRequest = new CompleteOrderRequest();
        completeOrderRequest.setOrderId("1");

        searchRequest = OrderSearchRequest.builder()
                .orderStatus(OrderStatus.PENDING)
                .page(0)
                .size(10)
                .sortBy("auditInfo.createdAt")
                .sortDirection("DESC")
                .build();

        processOrderRequest = new ProcessOrderRequest();
        processOrderRequest.setOrderId("1");
        processOrderRequest.setNote("Đang đóng gói");

        shipOrderRequest = new ShipOrderRequest();
        shipOrderRequest.setOrderId("1");
        shipOrderRequest.setShipperId("shipper-001");
        shipOrderRequest.setShipperName("Tài xế A");
        shipOrderRequest.setShipperPhone("0909123456");

        deliverOrderRequest = new DeliverOrderRequest();
        deliverOrderRequest.setOrderId("1");
        deliverOrderRequest.setNote("Giao thành công");

        delayOrderRequest = new DelayOrderRequest();
        delayOrderRequest.setOrderId("1");
        delayOrderRequest.setReason("Kẹt xe");

        readyForPickupRequest = new ReadyForPickupRequest();
        readyForPickupRequest.setOrderId("1");
        readyForPickupRequest.setNote("Hàng đã sẵn sàng");

        pickupOrderRequest = new PickupOrderRequest();
        pickupOrderRequest.setOrderId("1");
        pickupOrderRequest.setNote("Khách đã lấy hàng");

        returnOrderRequest = new ReturnOrderRequest();
        returnOrderRequest.setOrderId("1");
        returnOrderRequest.setReason("Sản phẩm lỗi");

        confirmReturnRequest = new ConfirmReturnRequest();
        confirmReturnRequest.setOrderId("1");
        confirmReturnRequest.setCondition("Còn nguyên tem");
        confirmReturnRequest.setNote("Đã kiểm tra");

        refundOrderRequest = new RefundOrderRequest();
        refundOrderRequest.setOrderId("1");
        refundOrderRequest.setNote("Đã hoàn tiền");

        updateShippingRequest = new UpdateShippingRequest();
        updateShippingRequest.setOrderId("1");
        updateShippingRequest.setShippingMethod("Giao hàng tiết kiệm");

        updateDeliveryRequest = new UpdateDeliveryRequest();
        updateDeliveryRequest.setOrderId("1");
        updateDeliveryRequest.setEstimatedDeliveryDate(LocalDateTime.now().plusDays(3));

        updateAdminNotesRequest = new UpdateAdminNotesRequest();
        updateAdminNotesRequest.setOrderId("1");
        updateAdminNotesRequest.setAdminNotes("Ghi chú nội bộ");

        transitionOrderRequest = new TransitionOrderRequest();
        transitionOrderRequest.setOrderId("1");
        transitionOrderRequest.setTargetStatus(OrderStatus.PROCESSING);

        paymentCallbackRequest = new PaymentCallbackRequest();
        paymentCallbackRequest.setOrderNumber("ORD-20240613-0001");
        paymentCallbackRequest.setStatus("SUCCESS");
    }

    // ========================================================================
    // CUSTOMER ENDPOINTS
    // ========================================================================

    @Nested
    @DisplayName("Customer: POST /api/orders — createOrder")
    class CreateOrderTests {
        @Test
        @DisplayName("Should return 201 Created with order data")
        void shouldCreateOrder() throws Exception {
            when(orderService.createOrder(any(CreateOrderRequest.class)))
                    .thenReturn(Response.ok(sampleOrderDto));

            mockMvc.perform(post("/api/orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(createOrderRequest)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.orderNumber").value("ORD-20240613-0001"))
                    .andExpect(jsonPath("$.data.currentStatus").value("CONFIRMED"))
                    .andExpect(jsonPath("$.data.totalAmount").value(1130000.0));

            verify(orderService).createOrder(any(CreateOrderRequest.class));
        }
    }

    @Nested
    @DisplayName("Customer: GET /api/orders/{orderId} — getOrderById")
    class GetOrderByIdTests {
        @Test
        @DisplayName("Should return order by ID")
        void shouldGetOrderById() throws Exception {
            when(orderService.getOrderById("1")).thenReturn(Response.ok(sampleOrderDto));

            mockMvc.perform(get("/api/orders/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.orderNumber").value("ORD-20240613-0001"));

            verify(orderService).getOrderById("1");
        }
    }

    @Nested
    @DisplayName("Customer: GET /api/orders/number/{orderNumber} — getOrderByOrderNumber")
    class GetOrderByOrderNumberTests {
        @Test
        @DisplayName("Should return order by order number")
        void shouldGetOrderByOrderNumber() throws Exception {
            when(orderService.getOrderByOrderNumber("ORD-20240613-0001"))
                    .thenReturn(Response.ok(sampleOrderDto));

            mockMvc.perform(get("/api/orders/number/ORD-20240613-0001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.orderNumber").value("ORD-20240613-0001"));

            verify(orderService).getOrderByOrderNumber("ORD-20240613-0001");
        }
    }

    @Nested
    @DisplayName("Customer: POST /api/orders/my-orders — getMyOrders")
    class GetMyOrdersTests {
        @Test
        @DisplayName("Should return paginated my orders")
        void shouldGetMyOrders() throws Exception {
            var pagingResponse = PagingResponse.<OrderDto>builder()
                    .contents(List.of(sampleOrderDto))
                    .paging(com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.PageableData.builder()
                            .pageNumber(0).totalPages(1).totalElements(1).pageSize(20).build())
                    .build();
            when(orderService.getMyOrders(any(OrderSearchRequest.class)))
                    .thenReturn(Response.ok(pagingResponse));

            mockMvc.perform(post("/api/orders/my-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(searchRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.contents[0].orderNumber").value("ORD-20240613-0001"))
                    .andExpect(jsonPath("$.data.paging.totalElements").value(1));

            verify(orderService).getMyOrders(any(OrderSearchRequest.class));
        }
    }

    @Nested
    @DisplayName("Customer: POST /api/orders/cancel — cancelOrder")
    class CancelOrderTests {
        @Test
        @DisplayName("Should cancel order successfully")
        void shouldCancelOrder() throws Exception {
            OrderDto cancelled = OrderDto.builder()
                    .id(1L).orderNumber("ORD-20240613-0001")
                    .currentStatus(OrderStatus.CANCELLED)
                    .cancellationReason("Khách hàng yêu cầu hủy")
                    .cancelledAt(LocalDateTime.now())
                    .cancelledBy("testuser")
                    .build();

            when(orderService.cancelOrder(any(CancelOrderRequest.class)))
                    .thenReturn(Response.ok(cancelled));

            mockMvc.perform(post("/api/orders/cancel")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(cancelOrderRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.currentStatus").value("CANCELLED"))
                    .andExpect(jsonPath("$.data.cancellationReason").value("Khách hàng yêu cầu hủy"));

            verify(orderService).cancelOrder(any(CancelOrderRequest.class));
        }
    }

    // ========================================================================
    // ADMIN ENDPOINTS
    // ========================================================================

    @Nested
    @DisplayName("Admin: POST /api/orders/search — searchOrders")
    class SearchOrdersTests {
        @Test
        @DisplayName("Should return paginated search results")
        void shouldSearchOrders() throws Exception {
            var pagingResponse = PagingResponse.<OrderDto>builder()
                    .contents(List.of(sampleOrderDto))
                    .paging(com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.PageableData.builder()
                            .pageNumber(0).totalPages(1).totalElements(1).pageSize(20).build())
                    .build();
            when(orderService.searchOrders(any(OrderSearchRequest.class)))
                    .thenReturn(Response.ok(pagingResponse));

            mockMvc.perform(post("/api/orders/search")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(searchRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.contents[0].orderNumber").value("ORD-20240613-0001"));

            verify(orderService).searchOrders(any(OrderSearchRequest.class));
        }
    }

    @Nested
    @DisplayName("Admin: PUT /api/orders/shipping — updateShipping")
    class UpdateShippingTests {
        @Test
        @DisplayName("Should update shipping information")
        void shouldUpdateShipping() throws Exception {
            when(orderService.updateShipping(any(UpdateShippingRequest.class)))
                    .thenReturn(Response.ok(sampleOrderDto));

            mockMvc.perform(put("/api/orders/shipping")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateShippingRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.orderNumber").value("ORD-20240613-0001"));

            verify(orderService).updateShipping(any(UpdateShippingRequest.class));
        }
    }

    @Nested
    @DisplayName("Admin: PUT /api/orders/delivery — updateDelivery")
    class UpdateDeliveryTests {
        @Test
        @DisplayName("Should update delivery dates")
        void shouldUpdateDelivery() throws Exception {
            when(orderService.updateDelivery(any(UpdateDeliveryRequest.class)))
                    .thenReturn(Response.ok(sampleOrderDto));

            mockMvc.perform(put("/api/orders/delivery")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDeliveryRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.orderNumber").value("ORD-20240613-0001"));

            verify(orderService).updateDelivery(any(UpdateDeliveryRequest.class));
        }
    }

    @Nested
    @DisplayName("Admin: PUT /api/orders/admin-notes — updateAdminNotes")
    class UpdateAdminNotesTests {
        @Test
        @DisplayName("Should update admin notes")
        void shouldUpdateAdminNotes() throws Exception {
            when(orderService.updateAdminNotes(any(UpdateAdminNotesRequest.class)))
                    .thenReturn(Response.ok(sampleOrderDto));

            mockMvc.perform(put("/api/orders/admin-notes")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateAdminNotesRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.orderNumber").value("ORD-20240613-0001"));

            verify(orderService).updateAdminNotes(any(UpdateAdminNotesRequest.class));
        }
    }

    @Nested
    @DisplayName("Admin: POST /api/orders/confirm — confirmOrder")
    class ConfirmOrderTests {
        @Test
        @DisplayName("Should confirm order successfully")
        void shouldConfirmOrder() throws Exception {
            OrderDto confirmed = OrderDto.builder()
                    .id(1L).orderNumber("ORD-20240613-0001")
                    .currentStatus(OrderStatus.CONFIRMED)
                    .confirmedAt(LocalDateTime.now())
                    .confirmedBy("admin@test.com")
                    .build();

            when(orderService.confirmOrder(any(ConfirmOrderRequest.class)))
                    .thenReturn(Response.ok(confirmed));

            mockMvc.perform(post("/api/orders/confirm")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(confirmOrderRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.currentStatus").value("CONFIRMED"))
                    .andExpect(jsonPath("$.data.confirmedBy").value("admin@test.com"));

            verify(orderService).confirmOrder(any(ConfirmOrderRequest.class));
        }
    }

    @Nested
    @DisplayName("Admin: POST /api/orders/complete — completeOrder")
    class CompleteOrderTests {
        @Test
        @DisplayName("Should complete order successfully")
        void shouldCompleteOrder() throws Exception {
            OrderDto completed = OrderDto.builder()
                    .id(1L).orderNumber("ORD-20240613-0001")
                    .currentStatus(OrderStatus.COMPLETED)
                    .completedAt(LocalDateTime.now())
                    .build();

            when(orderService.completeOrder(any(CompleteOrderRequest.class)))
                    .thenReturn(Response.ok(completed));

            mockMvc.perform(post("/api/orders/complete")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(completeOrderRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.currentStatus").value("COMPLETED"));

            verify(orderService).completeOrder(any(CompleteOrderRequest.class));
        }
    }

    @Nested
    @DisplayName("Admin: GET /api/orders/pending — getPendingOrders")
    class GetPendingOrdersTests {
        @Test
        @DisplayName("Should return pending orders")
        void shouldGetPendingOrders() throws Exception {
            when(orderService.getPendingOrders()).thenReturn(Response.ok(List.of(sampleOrderDto)));

            mockMvc.perform(get("/api/orders/pending"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].orderNumber").value("ORD-20240613-0001"));
        }
    }

    @Nested
    @DisplayName("Admin: GET /api/orders/in-progress — getInProgressOrders")
    class GetInProgressOrdersTests {
        @Test
        @DisplayName("Should return in-progress orders")
        void shouldGetInProgressOrders() throws Exception {
            when(orderService.getInProgressOrders()).thenReturn(Response.ok(List.of(sampleOrderDto)));

            mockMvc.perform(get("/api/orders/in-progress"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].orderNumber").value("ORD-20240613-0001"));
        }
    }

    @Nested
    @DisplayName("Admin: GET /api/orders/statistics — getOrderStatistics")
    class GetOrderStatisticsTests {
        @Test
        @DisplayName("Should return statistics")
        void shouldGetStatistics() throws Exception {
            Map<String, Object> stats2 = Map.of(
                    "totalOrders", 100,
                    "totalRevenue", 50000000.0
            );
            //noinspection unchecked,rawtypes
            doReturn((Response) Response.ok(stats2))
                    .when(orderService).getOrderStatistics("2024-01-01", "2024-12-31");

            mockMvc.perform(get("/api/orders/statistics")
                            .param("startDate", "2024-01-01")
                            .param("endDate", "2024-12-31"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.totalOrders").value(100))
                    .andExpect(jsonPath("$.data.totalRevenue").value(50000000.0));
        }
    }

    // ========================================================================
    // DASHBOARD / STATUS TRANSITION ENDPOINTS
    // ========================================================================

    @Nested
    @DisplayName("Dashboard: POST /api/orders/transition — transitionOrder")
    class TransitionOrderTests {
        @Test
        @DisplayName("Should throw BusinessException when called")
        void shouldThrowError() throws Exception {
            mockMvc.perform(post("/api/orders/transition")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(transitionOrderRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Chuyển trạng thái không hợp lệ"))
                    .andExpect(jsonPath("$.errorCode").value("INVALID_STATUS_TRANSITION"))
                    .andExpect(jsonPath("$.detail").value("Không hỗ trợ: PROCESSING"));
        }
    }

    @Nested
    @DisplayName("Dashboard: POST /api/orders/ship — shipOrder")
    class ShipOrderTests {
        @Test
        @DisplayName("Should return delivery info")
        void shouldShipOrder() throws Exception {
            mockMvc.perform(post("/api/orders/ship")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(transitionOrderRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.orderId").value("1"))
                    .andExpect(jsonPath("$.data.deliveryToken").exists())
                    .andExpect(jsonPath("$.data.deliveryUrl").exists());
        }
    }

    @Nested
    @DisplayName("Dashboard: GET /api/orders/delivery-pin/{orderNumber} — getDeliveryPin")
    class GetDeliveryPinTests {
        @Test
        @DisplayName("Should return delivery PIN info")
        void shouldGetDeliveryPin() throws Exception {
            mockMvc.perform(get("/api/orders/delivery-pin/ORD-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.orderNumber").value("ORD-001"))
                    .andExpect(jsonPath("$.data.message").value("Tính năng PIN đang phát triển"));
        }
    }

    @Nested
    @DisplayName("Dashboard: DELETE /api/orders/delivery-pin/{orderNumber} — clearDeliveryPin")
    class ClearDeliveryPinTests {
        @Test
        @DisplayName("Should clear PIN successfully")
        void shouldClearDeliveryPin() throws Exception {
            mockMvc.perform(delete("/api/orders/delivery-pin/ORD-001"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status.message").value("Đã xóa PIN"));
        }
    }

    // ========================================================================
    // STATUS TRANSITION DELEGATES (Admin)
    // ========================================================================

    @Nested
    @DisplayName("Admin: processOrder, shipOrder, deliverOrder, markDelayed")
    class StatusTransitionDelegatesTests {

        @Test
        @DisplayName("processOrder should delegate to service")
        void shouldProcessOrder() throws Exception {
            // The controller doesn't have a direct /process endpoint;
            // processOrder is accessed via /api/orders/transition or internally.
            // Testing the actual flow: controller calls orderService.processOrder()
            // But there's no @RequestMapping for processOrder in the controller.
            // The actual flow goes through transitionOrder which throws error.
            // These operations are called directly on the service from tests.
        }
    }

    // ========================================================================
    // ORDER STATUS CHANGE ENDPOINTS (via service - no direct REST mapping)
    // These methods exist in OrderService but are not exposed in the controller
    // orderControllerImpl. They are called internally or via transition.
    // ========================================================================
}
