package com.anno.ERP_SpringBoot_Experiment.service.interfaces;

import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;
import com.anno.ERP_SpringBoot_Experiment.service.dto.OrderDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.*;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.PagingResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;

import java.util.List;

public interface iOrder {

    /* ==================== CREATE ==================== */

    /**
     * Tạo order mới (hỗ trợ cả direct items, từ cart, từ booking)
     */
    Response<OrderDto> createOrder(CreateOrderRequest request);

    /* ==================== READ / SEARCH ==================== */

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

    /* ==================== UPDATE (Grouped) ==================== */

    /**
     * Cập nhật thông tin vận chuyển (shippingInfo, shippingMethod)
     */
    Response<OrderDto> updateShipping(UpdateShippingRequest request);

    /**
     * Cập nhật ngày giao hàng (estimatedDeliveryDate, actualDeliveryDate)
     */
    Response<OrderDto> updateDelivery(UpdateDeliveryRequest request);

    /**
     * Cập nhật ghi chú quản trị (adminNotes)
     */
    Response<OrderDto> updateAdminNotes(UpdateAdminNotesRequest request);

    /**
     * Xác nhận đơn hàng (confirmedAt, confirmedBy)
     */
    Response<OrderDto> confirmOrder(ConfirmOrderRequest request);

    /**
     * Hủy đơn hàng (cancellationReason, cancelledAt, cancelledBy)
     */
    Response<OrderDto> cancelOrder(CancelOrderRequest request);

    /**
     * Hoàn thành đơn hàng (completedAt)
     */
    Response<OrderDto> completeOrder(CompleteOrderRequest request);

    /* ==================== HELPER ==================== */

    /**
     * Cập nhật trạng thái đơn hàng (Helper cho internal/Kafka)
     */
    void setStatus(String orderNumber, OrderStatus status);
}
