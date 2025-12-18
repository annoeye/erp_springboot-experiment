package com.anno.ERP_SpringBoot_Experiment.service.interfaces;

import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;
import com.anno.ERP_SpringBoot_Experiment.service.dto.OrderDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CancelOrderRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateOrderRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.OrderSearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateOrderRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.PagingResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;

import java.util.List;

public interface iOrder {

    /**
     * Tạo order mới
     */
    Response<OrderDto> createOrder(CreateOrderRequest request);

    /**
     * Tạo order từ shopping cart
     */
    Response<OrderDto> createOrderFromCart(String cartId, CreateOrderRequest request);

    /**
     * Tạo order từ booking
     */
    Response<OrderDto> createOrderFromBooking(String bookingId, CreateOrderRequest request);

    /**
     * Lấy thông tin order theo ID
     */
    Response<OrderDto> getOrderById(String orderId);

    /**
     * Lấy thông tin order theo order number
     */
    Response<OrderDto> getOrderByOrderNumber(String orderNumber);

    /**
     * Lấy danh sách orders của customer hiện tại
     */
    Response<PagingResponse<OrderDto>> getMyOrders(OrderSearchRequest request);

    /**
     * Tìm kiếm orders (admin)
     */
    Response<PagingResponse<OrderDto>> searchOrders(OrderSearchRequest request);

    /**
     * Cập nhật order
     */
    Response<OrderDto> updateOrder(UpdateOrderRequest request);

    /**
     * Cập nhật status của order
     */
    Response<OrderDto> updateOrderStatus(String orderId, OrderStatus newStatus);

    /**
     * Xác nhận order
     */
    Response<OrderDto> confirmOrder(String orderId);

    /**
     * Hủy order
     */
    Response<OrderDto> cancelOrder(CancelOrderRequest request);

    /**
     * Đánh dấu order đã giao hàng
     */
    Response<OrderDto> markAsDelivered(String orderId);

    /**
     * Hoàn thành order
     */
    Response<OrderDto> completeOrder(String orderId);

    /**
     * Lấy danh sách orders cần xử lý
     */
    Response<List<OrderDto>> getPendingOrders();

    /**
     * Lấy danh sách orders đang giao hàng
     */
    Response<List<OrderDto>> getInProgressOrders();

    /**
     * Thống kê orders
     */
    Response<?> getOrderStatistics(String startDate, String endDate);
}
