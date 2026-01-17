package com.anno.ERP_SpringBoot_Experiment.web.rest.impl;

import com.anno.ERP_SpringBoot_Experiment.config.SecurityConfiguration;
import com.anno.ERP_SpringBoot_Experiment.authenticated.JwtAuthenticationFilter;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.PaymentInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.ShippingInfo;
import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.PaymentMethod;
import com.anno.ERP_SpringBoot_Experiment.service.dto.OrderDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CancelOrderRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateOrderRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.OrderSearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateOrderRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Page.PagingResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iOrder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit Tests cho OrderController
 */
@WebMvcTest(controllers = OrderControllerImpl.class, excludeAutoConfiguration = {
                SecurityConfiguration.class,
                SecurityAutoConfiguration.class,
                SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("OrderController Unit Tests")
class OrderControllerImplTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private iOrder orderService;

        // === PHẦN SỬA LỖI QUAN TRỌNG ===
        // Mock bean này để Spring không cố gắng tạo filter thật (nguyên nhân gây lỗi)
        @MockitoBean
        private JwtAuthenticationFilter jwtAuthenticationFilter;
        // ===============================

        private OrderDto mockOrderDto;
        private CreateOrderRequest createOrderRequest;
        private UpdateOrderRequest updateOrderRequest;
        private CancelOrderRequest cancelOrderRequest;
        private OrderSearchRequest orderSearchRequest;
        private ShippingInfo shippingInfo;

        @BeforeEach
        void setUp() {
                // Setup shipping info
                shippingInfo = new ShippingInfo();
                shippingInfo.setRecipientName("Nguyen Van A");
                shippingInfo.setPhoneNumber("0123456789");
                shippingInfo.setAddress("123 Nguyen Hue");
                shippingInfo.setCity("Ho Chi Minh");
                shippingInfo.setDistrict("District 1");
                shippingInfo.setWard("Ward 1");

                // Setup payment info
                PaymentInfo paymentInfo = new PaymentInfo();
                paymentInfo.setPaymentMethod(PaymentMethod.CASH);
                paymentInfo.setPaymentStatus(com.anno.ERP_SpringBoot_Experiment.model.enums.PaymentStatus.PENDING);

                // Setup mock OrderDto
                mockOrderDto = OrderDto.builder()
                                .id(UUID.randomUUID())
                                .orderNumber("ORD-2024-001")
                                .orderDate(LocalDateTime.now())
                                .status(OrderStatus.PENDING)
                                .customerId(UUID.randomUUID())
                                .customerName("Nguyen Van A")
                                .customerEmail("nguyenvana@example.com")
                                .customerPhone("0123456789")
                                .orderItems(Collections.emptyList())
                                .subtotal(1000000.0)
                                .discountAmount(0.0)
                                .taxAmount(100000.0)
                                .shippingFee(30000.0)
                                .totalAmount(1130000.0)
                                .shippingInfo(shippingInfo)
                                .paymentInfo(paymentInfo)
                                .build();

                // Setup CreateOrderRequest
                CreateOrderRequest.OrderItemRequest itemRequest = CreateOrderRequest.OrderItemRequest.builder()
                                .attributesId(UUID.randomUUID().toString())
                                .quantity(2)
                                .notes("Test item")
                                .build();

                createOrderRequest = CreateOrderRequest.builder()
                                .items(Arrays.asList(itemRequest))
                                .shippingInfo(shippingInfo)
                                .paymentMethod(PaymentMethod.CASH)
                                .customerNotes("Please deliver in the morning")
                                .build();

                // Setup UpdateOrderRequest
                updateOrderRequest = UpdateOrderRequest.builder()
                                .orderId(mockOrderDto.getId().toString())
                                .status(OrderStatus.CONFIRMED)
                                .adminNotes("Order confirmed")
                                .trackingNumber("TRACK-123")
                                .build();

                // Setup CancelOrderRequest
                cancelOrderRequest = CancelOrderRequest.builder()
                                .orderId(mockOrderDto.getId().toString())
                                .cancellationReason("Customer requested cancellation")
                                .build();

                // Setup OrderSearchRequest
                orderSearchRequest = OrderSearchRequest.builder()
                                .orderStatus(OrderStatus.PENDING)
                                .page(0)
                                .size(10)
                                .sortBy("orderDate")
                                .sortDirection("DESC")
                                .build();
        }

        // ==================== CUSTOMER ORDER OPERATIONS - SUCCESS TESTS
        // ====================

        @Test
        @DisplayName("Should successfully create a new order")
        void testCreateOrder_Success() throws Exception {
                // Arrange
                when(orderService.createOrder(any(CreateOrderRequest.class)))
                                .thenReturn(Response.ok(mockOrderDto));

                // Act & Assert
                mockMvc.perform(post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createOrderRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.orderNumber").value("ORD-2024-001"))
                                .andExpect(jsonPath("$.data.status").value("PENDING"))
                                .andExpect(jsonPath("$.data.totalAmount").value(1130000.0));

                // Verify
                verify(orderService, times(1)).createOrder(any(CreateOrderRequest.class));
        }

        @Test
        @DisplayName("Should successfully create order from cart")
        void testCreateOrderFromCart_Success() throws Exception {
                // Arrange
                String cartId = UUID.randomUUID().toString();
                when(orderService.createOrderFromCart(eq(cartId), any(CreateOrderRequest.class)))
                                .thenReturn(Response.ok(mockOrderDto));

                // Act & Assert
                mockMvc.perform(post("/api/orders/from-cart/" + cartId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createOrderRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.orderNumber").value("ORD-2024-001"));

                // Verify
                verify(orderService, times(1)).createOrderFromCart(eq(cartId), any(CreateOrderRequest.class));
        }

        @Test
        @DisplayName("Should successfully create order from booking")
        void testCreateOrderFromBooking_Success() throws Exception {
                // Arrange
                String bookingId = UUID.randomUUID().toString();
                when(orderService.createOrderFromBooking(eq(bookingId), any(CreateOrderRequest.class)))
                                .thenReturn(Response.ok(mockOrderDto));

                // Act & Assert
                mockMvc.perform(post("/api/orders/from-booking/" + bookingId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createOrderRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.orderNumber").value("ORD-2024-001"));

                // Verify
                verify(orderService, times(1)).createOrderFromBooking(eq(bookingId), any(CreateOrderRequest.class));
        }

        @Test
        @DisplayName("Should successfully get order by ID")
        void testGetOrderById_Success() throws Exception {
                // Arrange
                String orderId = mockOrderDto.getId().toString();
                when(orderService.getOrderById(eq(orderId)))
                                .thenReturn(Response.ok(mockOrderDto));

                // Act & Assert
                mockMvc.perform(get("/api/orders/" + orderId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.id").value(orderId))
                                .andExpect(jsonPath("$.data.orderNumber").value("ORD-2024-001"));

                // Verify
                verify(orderService, times(1)).getOrderById(eq(orderId));
        }

        @Test
        @DisplayName("Should successfully get order by order number")
        void testGetOrderByOrderNumber_Success() throws Exception {
                // Arrange
                String orderNumber = "ORD-2024-001";
                when(orderService.getOrderByOrderNumber(eq(orderNumber)))
                                .thenReturn(Response.ok(mockOrderDto));

                // Act & Assert
                mockMvc.perform(get("/api/orders/number/" + orderNumber))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.orderNumber").value(orderNumber));

                // Verify
                verify(orderService, times(1)).getOrderByOrderNumber(eq(orderNumber));
        }

        @Test
        @DisplayName("Should successfully get my orders with pagination")
        void testGetMyOrders_Success() throws Exception {
                // Arrange
                PagingResponse<OrderDto> pagingResponse = PagingResponse.<OrderDto>builder()
                                .contents(Arrays.asList(mockOrderDto))
                                .build();

                when(orderService.getMyOrders(any(OrderSearchRequest.class)))
                                .thenReturn(Response.ok(pagingResponse));

                // Act & Assert
                mockMvc.perform(post("/api/orders/my-orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(orderSearchRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.contents").isArray())
                                .andExpect(jsonPath("$.data.contents[0].orderNumber").value("ORD-2024-001"));

                // Verify
                verify(orderService, times(1)).getMyOrders(any(OrderSearchRequest.class));
        }

        @Test
        @DisplayName("Should successfully cancel order")
        void testCancelOrder_Success() throws Exception {
                // Arrange
                OrderDto cancelledOrder = OrderDto.builder()
                                .id(mockOrderDto.getId())
                                .orderNumber(mockOrderDto.getOrderNumber())
                                .status(OrderStatus.CANCELLED)
                                .cancellationReason("Customer requested cancellation")
                                .cancelledAt(LocalDateTime.now())
                                .build();

                when(orderService.cancelOrder(any(CancelOrderRequest.class)))
                                .thenReturn(Response.ok(cancelledOrder));

                // Act & Assert
                mockMvc.perform(post("/api/orders/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(cancelOrderRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                                .andExpect(jsonPath("$.data.cancellationReason")
                                                .value("Customer requested cancellation"));

                // Verify
                verify(orderService, times(1)).cancelOrder(any(CancelOrderRequest.class));
        }

        // ==================== CUSTOMER ORDER OPERATIONS - ERROR TESTS
        // ====================

        @Test
        @DisplayName("Should handle error when creating order fails")
        void testCreateOrder_Error() throws Exception {
                // Arrange
                when(orderService.createOrder(any(CreateOrderRequest.class)))
                                .thenReturn(Response.fail("Không đủ hàng trong kho", 400));

                // Act & Assert
                mockMvc.perform(post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createOrderRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.message").value("Không đủ hàng trong kho"));

                // Verify
                verify(orderService, times(1)).createOrder(any(CreateOrderRequest.class));
        }

        @Test
        @DisplayName("Should handle error when creating order from cart fails")
        void testCreateOrderFromCart_Error() throws Exception {
                // Arrange
                String cartId = UUID.randomUUID().toString();
                when(orderService.createOrderFromCart(eq(cartId), any(CreateOrderRequest.class)))
                                .thenReturn(Response.fail("Giỏ hàng không tồn tại", 404));

                // Act & Assert
                mockMvc.perform(post("/api/orders/from-cart/" + cartId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createOrderRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.message").value("Giỏ hàng không tồn tại"));

                // Verify
                verify(orderService, times(1)).createOrderFromCart(eq(cartId), any(CreateOrderRequest.class));
        }

        @Test
        @DisplayName("Should handle error when creating order from booking fails")
        void testCreateOrderFromBooking_Error() throws Exception {
                // Arrange
                String bookingId = UUID.randomUUID().toString();
                when(orderService.createOrderFromBooking(eq(bookingId), any(CreateOrderRequest.class)))
                                .thenReturn(Response.fail("Booking không tồn tại", 404));

                // Act & Assert
                mockMvc.perform(post("/api/orders/from-booking/" + bookingId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(createOrderRequest)))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.message").value("Booking không tồn tại"));

                // Verify
                verify(orderService, times(1)).createOrderFromBooking(eq(bookingId), any(CreateOrderRequest.class));
        }

        @Test
        @DisplayName("Should handle error when getting order by ID fails")
        void testGetOrderById_Error() throws Exception {
                // Arrange
                String orderId = UUID.randomUUID().toString();
                when(orderService.getOrderById(eq(orderId)))
                                .thenReturn(Response.fail("Đơn hàng không tồn tại", 404));

                // Act & Assert
                mockMvc.perform(get("/api/orders/" + orderId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Đơn hàng không tồn tại"));

                // Verify
                verify(orderService, times(1)).getOrderById(eq(orderId));
        }

        @Test
        @DisplayName("Should handle error when getting order by order number fails")
        void testGetOrderByOrderNumber_Error() throws Exception {
                // Arrange
                String orderNumber = "INVALID-ORDER";
                when(orderService.getOrderByOrderNumber(eq(orderNumber)))
                                .thenReturn(Response.fail("Không tìm thấy đơn hàng", 404));

                // Act & Assert
                mockMvc.perform(get("/api/orders/number/" + orderNumber))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Không tìm thấy đơn hàng"));

                // Verify
                verify(orderService, times(1)).getOrderByOrderNumber(eq(orderNumber));
        }

        @Test
        @DisplayName("Should handle error when getting my orders fails")
        void testGetMyOrders_Error() throws Exception {
                // Arrange
                when(orderService.getMyOrders(any(OrderSearchRequest.class)))
                                .thenReturn(Response.fail("Lỗi hệ thống khi tìm kiếm đơn hàng", 500));

                // Act & Assert
                mockMvc.perform(post("/api/orders/my-orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(orderSearchRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Lỗi hệ thống khi tìm kiếm đơn hàng"));

                // Verify
                verify(orderService, times(1)).getMyOrders(any(OrderSearchRequest.class));
        }

        @Test
        @DisplayName("Should handle error when cancelling order fails")
        void testCancelOrder_Error() throws Exception {
                // Arrange
                when(orderService.cancelOrder(any(CancelOrderRequest.class)))
                                .thenReturn(Response.fail("Không thể hủy đơn hàng đã được xác nhận", 400));

                // Act & Assert
                mockMvc.perform(post("/api/orders/cancel")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(cancelOrderRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Không thể hủy đơn hàng đã được xác nhận"));

                // Verify
                verify(orderService, times(1)).cancelOrder(any(CancelOrderRequest.class));
        }

        // ==================== ADMIN ORDER OPERATIONS - SUCCESS TESTS
        // ====================

        @Test
        @DisplayName("Should successfully search orders (Admin)")
        void testSearchOrders_Success() throws Exception {
                // Arrange
                PagingResponse<OrderDto> pagingResponse = PagingResponse.<OrderDto>builder()
                                .contents(Arrays.asList(mockOrderDto))
                                .build();

                when(orderService.searchOrders(any(OrderSearchRequest.class)))
                                .thenReturn(Response.ok(pagingResponse));

                // Act & Assert
                mockMvc.perform(post("/api/orders/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(orderSearchRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.contents").isArray())
                                .andExpect(jsonPath("$.data.contents[0].orderNumber").value("ORD-2024-001"));

                // Verify
                verify(orderService, times(1)).searchOrders(any(OrderSearchRequest.class));
        }

        @Test
        @DisplayName("Should successfully update order (Admin)")
        void testUpdateOrder_Success() throws Exception {
                // Arrange
                OrderDto updatedOrder = OrderDto.builder()
                                .id(mockOrderDto.getId())
                                .orderNumber(mockOrderDto.getOrderNumber())
                                .status(OrderStatus.CONFIRMED)
                                .adminNotes("Order confirmed")
                                .build();

                when(orderService.updateOrder(any(UpdateOrderRequest.class)))
                                .thenReturn(Response.ok(updatedOrder));

                // Act & Assert
                mockMvc.perform(put("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateOrderRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                                .andExpect(jsonPath("$.data.adminNotes").value("Order confirmed"));

                // Verify
                verify(orderService, times(1)).updateOrder(any(UpdateOrderRequest.class));
        }

        @Test
        @DisplayName("Should successfully update order status (Admin)")
        void testUpdateOrderStatus_Success() throws Exception {
                // Arrange
                String orderId = mockOrderDto.getId().toString();
                OrderStatus newStatus = OrderStatus.PROCESSING;

                OrderDto updatedOrder = OrderDto.builder()
                                .id(mockOrderDto.getId())
                                .orderNumber(mockOrderDto.getOrderNumber())
                                .status(newStatus)
                                .build();

                when(orderService.updateOrderStatus(eq(orderId), eq(newStatus)))
                                .thenReturn(Response.ok(updatedOrder));

                // Act & Assert
                mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                                .param("status", newStatus.name()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("PROCESSING"));

                // Verify
                verify(orderService, times(1)).updateOrderStatus(eq(orderId), eq(newStatus));
        }

        @Test
        @DisplayName("Should successfully confirm order (Admin)")
        void testConfirmOrder_Success() throws Exception {
                // Arrange
                String orderId = mockOrderDto.getId().toString();
                OrderDto confirmedOrder = OrderDto.builder()
                                .id(mockOrderDto.getId())
                                .orderNumber(mockOrderDto.getOrderNumber())
                                .status(OrderStatus.CONFIRMED)
                                .confirmedAt(LocalDateTime.now())
                                .confirmedBy("admin@example.com")
                                .build();

                when(orderService.confirmOrder(eq(orderId)))
                                .thenReturn(Response.ok(confirmedOrder));

                // Act & Assert
                mockMvc.perform(post("/api/orders/" + orderId + "/confirm"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                                .andExpect(jsonPath("$.data.confirmedBy").value("admin@example.com"));

                // Verify
                verify(orderService, times(1)).confirmOrder(eq(orderId));
        }

        @Test
        @DisplayName("Should successfully mark order as delivered (Admin/Shipper)")
        void testMarkAsDelivered_Success() throws Exception {
                // Arrange
                String orderId = mockOrderDto.getId().toString();
                OrderDto deliveredOrder = OrderDto.builder()
                                .id(mockOrderDto.getId())
                                .orderNumber(mockOrderDto.getOrderNumber())
                                .status(OrderStatus.DELIVERED)
                                .build();

                when(orderService.markAsDelivered(eq(orderId)))
                                .thenReturn(Response.ok(deliveredOrder));

                // Act & Assert
                mockMvc.perform(post("/api/orders/" + orderId + "/delivered"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("DELIVERED"));

                // Verify
                verify(orderService, times(1)).markAsDelivered(eq(orderId));
        }

        @Test
        @DisplayName("Should successfully complete order (Admin)")
        void testCompleteOrder_Success() throws Exception {
                // Arrange
                String orderId = mockOrderDto.getId().toString();
                OrderDto completedOrder = OrderDto.builder()
                                .id(mockOrderDto.getId())
                                .orderNumber(mockOrderDto.getOrderNumber())
                                .status(OrderStatus.COMPLETED)
                                .completedAt(LocalDateTime.now())
                                .build();

                when(orderService.completeOrder(eq(orderId)))
                                .thenReturn(Response.ok(completedOrder));

                // Act & Assert
                mockMvc.perform(post("/api/orders/" + orderId + "/complete"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

                // Verify
                verify(orderService, times(1)).completeOrder(eq(orderId));
        }

        @Test
        @DisplayName("Should successfully get pending orders (Admin)")
        void testGetPendingOrders_Success() throws Exception {
                // Arrange
                List<OrderDto> pendingOrders = Arrays.asList(mockOrderDto);
                when(orderService.getPendingOrders())
                                .thenReturn(Response.ok(pendingOrders));

                // Act & Assert
                mockMvc.perform(get("/api/orders/pending"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data[0].status").value("PENDING"));

                // Verify
                verify(orderService, times(1)).getPendingOrders();
        }

        @Test
        @DisplayName("Should successfully get in-progress orders (Admin)")
        void testGetInProgressOrders_Success() throws Exception {
                // Arrange
                OrderDto inProgressOrder = OrderDto.builder()
                                .id(UUID.randomUUID())
                                .orderNumber("ORD-2024-002")
                                .status(OrderStatus.PROCESSING)
                                .build();

                List<OrderDto> inProgressOrders = Arrays.asList(inProgressOrder);
                when(orderService.getInProgressOrders())
                                .thenReturn(Response.ok(inProgressOrders));

                // Act & Assert
                mockMvc.perform(get("/api/orders/in-progress"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.data[0].status").value("PROCESSING"));

                // Verify
                verify(orderService, times(1)).getInProgressOrders();
        }

        @Test
        @DisplayName("Should successfully get order statistics (Admin)")
        void testGetOrderStatistics_Success() throws Exception {
                // Arrange
                String startDate = "2024-01-01";
                String endDate = "2024-12-31";

                Object statistics = new Object() {
                        public final int totalOrders = 100;
                        public final double totalRevenue = 50000000.0;
                        public final int completedOrders = 80;
                        public final int cancelledOrders = 10;
                };

                when(orderService.getOrderStatistics(eq(startDate), eq(endDate)))
                                .thenReturn((Response) Response.ok(statistics));

                // Act & Assert
                mockMvc.perform(get("/api/orders/statistics")
                                .param("startDate", startDate)
                                .param("endDate", endDate))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").exists());

                // Verify
                verify(orderService, times(1)).getOrderStatistics(eq(startDate), eq(endDate));
        }

        // ==================== ADMIN ORDER OPERATIONS - ERROR TESTS
        // ====================

        @Test
        @DisplayName("Should handle error when searching orders fails (Admin)")
        void testSearchOrders_Error() throws Exception {
                // Arrange
                when(orderService.searchOrders(any(OrderSearchRequest.class)))
                                .thenReturn(Response.fail("Lỗi hệ thống khi tìm kiếm", 500));

                // Act & Assert
                mockMvc.perform(post("/api/orders/search")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(orderSearchRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Lỗi hệ thống khi tìm kiếm"));

                // Verify
                verify(orderService, times(1)).searchOrders(any(OrderSearchRequest.class));
        }

        @Test
        @DisplayName("Should handle error when updating order fails (Admin)")
        void testUpdateOrder_Error() throws Exception {
                // Arrange
                when(orderService.updateOrder(any(UpdateOrderRequest.class)))
                                .thenReturn(Response.fail("Không thể cập nhật đơn hàng đã hoàn thành", 400));

                // Act & Assert
                mockMvc.perform(put("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateOrderRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Không thể cập nhật đơn hàng đã hoàn thành"));

                // Verify
                verify(orderService, times(1)).updateOrder(any(UpdateOrderRequest.class));
        }

        @Test
        @DisplayName("Should handle error when updating order status fails (Admin)")
        void testUpdateOrderStatus_Error() throws Exception {
                // Arrange
                String orderId = UUID.randomUUID().toString();
                OrderStatus newStatus = OrderStatus.PROCESSING;

                when(orderService.updateOrderStatus(eq(orderId), eq(newStatus)))
                                .thenReturn(Response.fail("Trạng thái không hợp lệ", 400));

                // Act & Assert
                mockMvc.perform(patch("/api/orders/" + orderId + "/status")
                                .param("status", newStatus.name()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Trạng thái không hợp lệ"));

                // Verify
                verify(orderService, times(1)).updateOrderStatus(eq(orderId), eq(newStatus));
        }

        @Test
        @DisplayName("Should handle error when confirming order fails (Admin)")
        void testConfirmOrder_Error() throws Exception {
                // Arrange
                String orderId = UUID.randomUUID().toString();
                when(orderService.confirmOrder(eq(orderId)))
                                .thenReturn(Response.fail("Đơn hàng đã được xác nhận trước đó", 400));

                // Act & Assert
                mockMvc.perform(post("/api/orders/" + orderId + "/confirm"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Đơn hàng đã được xác nhận trước đó"));

                // Verify
                verify(orderService, times(1)).confirmOrder(eq(orderId));
        }

        @Test
        @DisplayName("Should handle error when marking as delivered fails (Admin/Shipper)")
        void testMarkAsDelivered_Error() throws Exception {
                // Arrange
                String orderId = UUID.randomUUID().toString();
                when(orderService.markAsDelivered(eq(orderId)))
                                .thenReturn(Response.fail("Đơn hàng chưa được giao", 400));

                // Act & Assert
                mockMvc.perform(post("/api/orders/" + orderId + "/delivered"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Đơn hàng chưa được giao"));

                // Verify
                verify(orderService, times(1)).markAsDelivered(eq(orderId));
        }

        @Test
        @DisplayName("Should handle error when completing order fails (Admin)")
        void testCompleteOrder_Error() throws Exception {
                // Arrange
                String orderId = UUID.randomUUID().toString();
                when(orderService.completeOrder(eq(orderId)))
                                .thenReturn(Response.fail("Đơn hàng chưa được giao", 400));

                // Act & Assert
                mockMvc.perform(post("/api/orders/" + orderId + "/complete"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Đơn hàng chưa được giao"));

                // Verify
                verify(orderService, times(1)).completeOrder(eq(orderId));
        }

        @Test
        @DisplayName("Should handle error when getting pending orders fails (Admin)")
        void testGetPendingOrders_Error() throws Exception {
                // Arrange
                when(orderService.getPendingOrders())
                                .thenReturn(Response.fail("Lỗi hệ thống khi lấy danh sách đơn hàng", 500));

                // Act & Assert
                mockMvc.perform(get("/api/orders/pending"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Lỗi hệ thống khi lấy danh sách đơn hàng"));

                // Verify
                verify(orderService, times(1)).getPendingOrders();
        }

        @Test
        @DisplayName("Should handle error when getting in-progress orders fails (Admin)")
        void testGetInProgressOrders_Error() throws Exception {
                // Arrange
                when(orderService.getInProgressOrders())
                                .thenReturn(Response.fail("Lỗi hệ thống khi lấy danh sách đơn hàng", 500));

                // Act & Assert
                mockMvc.perform(get("/api/orders/in-progress"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Lỗi hệ thống khi lấy danh sách đơn hàng"));

                // Verify
                verify(orderService, times(1)).getInProgressOrders();
        }

        @Test
        @DisplayName("Should handle error when getting order statistics fails (Admin)")
        void testGetOrderStatistics_Error() throws Exception {
                // Arrange
                String startDate = "2024-01-01";
                String endDate = "2024-12-31";

                when(orderService.getOrderStatistics(eq(startDate), eq(endDate)))
                                .thenReturn(Response.fail("Lỗi khi tính toán thống kê", 500));

                // Act & Assert
                mockMvc.perform(get("/api/orders/statistics")
                                .param("startDate", startDate)
                                .param("endDate", endDate))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.message").value("Lỗi khi tính toán thống kê"));

                // Verify
                verify(orderService, times(1)).getOrderStatistics(eq(startDate), eq(endDate));
        }
}