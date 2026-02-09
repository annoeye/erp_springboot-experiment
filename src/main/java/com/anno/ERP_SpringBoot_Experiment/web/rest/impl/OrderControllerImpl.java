package com.anno.ERP_SpringBoot_Experiment.web.rest.impl;

import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;
import com.anno.ERP_SpringBoot_Experiment.service.dto.OrderDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CancelOrderRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateOrderRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.OrderSearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateOrderRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.PagingResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iOrder;
import com.anno.ERP_SpringBoot_Experiment.web.rest.OrderController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs quản lý đơn hàng")
@SecurityRequirement(name = "bearerAuth")
public class OrderControllerImpl implements OrderController {

    private final iOrder orderService;

    @Override
    @Operation(summary = "Tạo đơn hàng mới", description = "Customer tạo đơn hàng mới")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public Response<OrderDto> createOrder(CreateOrderRequest request) {
        log.info("REST request to create order");
        return orderService.createOrder(request);
    }

    @Override
    @Operation(summary = "Tạo đơn hàng từ giỏ hàng", description = "Tạo đơn hàng từ shopping cart")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public Response<OrderDto> createOrderFromCart(String cartId, CreateOrderRequest request) {
        log.info("REST request to create order from cart: {}", cartId);
        return orderService.createOrderFromCart(cartId, request);
    }

    @Override
    @Operation(summary = "Tạo đơn hàng từ booking", description = "Tạo đơn hàng từ booking")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public Response<OrderDto> createOrderFromBooking(String bookingId, CreateOrderRequest request) {
        log.info("REST request to create order from booking: {}", bookingId);
        return orderService.createOrderFromBooking(bookingId, request);
    }

    @Override
    @Operation(summary = "Lấy thông tin đơn hàng", description = "Lấy chi tiết đơn hàng theo ID")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public Response<OrderDto> getOrderById(String orderId) {
        log.info("REST request to get order: {}", orderId);
        return orderService.getOrderById(orderId);
    }

    @Override
    @Operation(summary = "Lấy thông tin đơn hàng theo mã", description = "Lấy chi tiết đơn hàng theo order number")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public Response<OrderDto> getOrderByOrderNumber(String orderNumber) {
        log.info("REST request to get order by number: {}", orderNumber);
        return orderService.getOrderByOrderNumber(orderNumber);
    }

    @Override
    @Operation(summary = "Lấy danh sách đơn hàng của tôi", description = "Customer xem danh sách đơn hàng của mình")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public Response<PagingResponse<OrderDto>> getMyOrders(OrderSearchRequest request) {
        log.info("REST request to get my orders");
        return orderService.getMyOrders(request);
    }

    @Override
    @Operation(summary = "Hủy đơn hàng", description = "Customer hoặc Admin hủy đơn hàng")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public Response<OrderDto> cancelOrder(CancelOrderRequest request) {
        log.info("REST request to cancel order: {}", request.getOrderId());
        return orderService.cancelOrder(request);
    }

    @Override
    @Operation(summary = "Tìm kiếm đơn hàng", description = "Admin tìm kiếm và lọc đơn hàng")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<PagingResponse<OrderDto>> searchOrders(OrderSearchRequest request) {
        log.info("REST request to search orders");
        return orderService.searchOrders(request);
    }

    @Override
    @Operation(summary = "Cập nhật đơn hàng", description = "Admin cập nhật thông tin đơn hàng")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<OrderDto> updateOrder(UpdateOrderRequest request) {
        log.info("REST request to update order: {}", request.getOrderId());
        return orderService.updateOrder(request);
    }

    @Override
    @Operation(summary = "Cập nhật trạng thái đơn hàng", description = "Admin cập nhật trạng thái đơn hàng")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<OrderDto> updateOrderStatus(String orderId, OrderStatus status) {
        log.info("REST request to update order status: {} to {}", orderId, status);
        return orderService.updateOrderStatus(orderId, status);
    }

    @Override
    @Operation(summary = "Xác nhận đơn hàng", description = "Admin xác nhận đơn hàng")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<OrderDto> confirmOrder(String orderId) {
        log.info("REST request to confirm order: {}", orderId);
        return orderService.confirmOrder(orderId);
    }

    @Override
    @Operation(summary = "Đánh dấu đã giao hàng", description = "Admin/Shipper đánh dấu đơn hàng đã giao")
    @PreAuthorize("hasAnyRole('ADMIN', 'SHIPPER')")
    public Response<OrderDto> markAsDelivered(String orderId) {
        log.info("REST request to mark order as delivered: {}", orderId);
        return orderService.markAsDelivered(orderId);
    }

    @Override
    @Operation(summary = "Hoàn thành đơn hàng", description = "Admin hoàn thành đơn hàng")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<OrderDto> completeOrder(String orderId) {
        log.info("REST request to complete order: {}", orderId);
        return orderService.completeOrder(orderId);
    }

    @Override
    @Operation(summary = "Lấy đơn hàng chờ xử lý", description = "Admin xem danh sách đơn hàng cần xử lý")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<List<OrderDto>> getPendingOrders() {
        log.info("REST request to get pending orders");
        return orderService.getPendingOrders();
    }

    @Override
    @Operation(summary = "Lấy đơn hàng đang giao", description = "Admin xem danh sách đơn hàng đang giao")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<List<OrderDto>> getInProgressOrders() {
        log.info("REST request to get in-progress orders");
        return orderService.getInProgressOrders();
    }

    @Override
    @Operation(summary = "Thống kê đơn hàng", description = "Admin xem thống kê đơn hàng theo thời gian")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<?> getOrderStatistics(String startDate, String endDate) {
        log.info("REST request to get order statistics from {} to {}", startDate, endDate);
        return orderService.getOrderStatistics(startDate, endDate);
    }
}
