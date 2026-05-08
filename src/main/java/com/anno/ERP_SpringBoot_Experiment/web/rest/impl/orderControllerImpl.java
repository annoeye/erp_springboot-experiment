package com.anno.ERP_SpringBoot_Experiment.web.rest.impl;

import com.anno.ERP_SpringBoot_Experiment.mapper.OrderMapper;
import com.anno.ERP_SpringBoot_Experiment.service.OrderManagement.OrderStatusHandler;
import com.anno.ERP_SpringBoot_Experiment.service.dto.OrderDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.*;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.OrderAdminResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.OrderUserResponse;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs quản lý đơn hàng")
@SecurityRequirement(name = "bearerAuth")
public class orderControllerImpl implements OrderController {

    private final iOrder orderService;
    private final OrderMapper orderMapper;
    private final OrderStatusHandler orderStatusHandler;

    /* ==================== Mappers ==================== */

    private Response<OrderUserResponse> toUserResponse(Response<OrderDto> res) {
        if (res.getData() == null)
            return Response.fail(res.getStatus());
        return Response.ok(orderMapper.toUserResponseFromDto(res.getData()), res.getMessage());
    }

    private Response<OrderAdminResponse> toAdminResponse(Response<OrderDto> res) {
        if (res.getData() == null)
            return Response.fail(res.getStatus());
        return Response.ok(orderMapper.toAdminResponseFromDto(res.getData()), res.getMessage());
    }

    private Response<PagingResponse<OrderUserResponse>> toPagingUserResponse(Response<PagingResponse<OrderDto>> res) {
        if (res.getData() == null)
            return Response.fail(res.getStatus());
        PagingResponse<OrderUserResponse> paging = PagingResponse.<OrderUserResponse>builder()
                .contents(res.getData().getContents().stream().map(orderMapper::toUserResponseFromDto)
                        .collect(Collectors.toList()))
                .paging(res.getData().getPaging())
                .build();
        return Response.ok(paging, res.getMessage());
    }

    private Response<PagingResponse<OrderAdminResponse>> toPagingAdminResponse(Response<PagingResponse<OrderDto>> res) {
        if (res.getData() == null)
            return Response.fail(res.getStatus());
        PagingResponse<OrderAdminResponse> paging = PagingResponse.<OrderAdminResponse>builder()
                .contents(res.getData().getContents().stream().map(orderMapper::toAdminResponseFromDto)
                        .collect(Collectors.toList()))
                .paging(res.getData().getPaging())
                .build();
        return Response.ok(paging, res.getMessage());
    }

    /* ==================== Customer Endpoints ==================== */

    @Override
    @Operation(summary = "Tạo đơn hàng mới", description = "Customer tạo đơn hàng mới")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public Response<OrderUserResponse> createOrder(CreateOrderRequest request) {
        log.info("Yêu cầu REST tạo đơn hàng mới");
        return toUserResponse(orderService.createOrder(request));
    }

    @Override
    @Operation(summary = "Lấy thông tin đơn hàng", description = "Lấy chi tiết đơn hàng theo ID")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public Response<OrderUserResponse> getOrderById(String orderId) {
        log.info("Yêu cầu REST lấy thông tin đơn hàng: {}", orderId);
        return toUserResponse(orderService.getOrderById(orderId));
    }

    @Override
    @Operation(summary = "Lấy thông tin đơn hàng theo mã", description = "Lấy chi tiết đơn hàng theo order number")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public Response<OrderUserResponse> getOrderByOrderNumber(String orderNumber) {
        log.info("Yêu cầu REST lấy thông tin đơn hàng theo mã: {}", orderNumber);
        return toUserResponse(orderService.getOrderByOrderNumber(orderNumber));
    }

    @Override
    @Operation(summary = "Lấy danh sách đơn hàng của tôi", description = "Customer xem danh sách đơn hàng của mình")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public Response<PagingResponse<OrderUserResponse>> getMyOrders(OrderSearchRequest request) {
        log.info("Yêu cầu REST lấy danh sách đơn hàng của tôi");
        return toPagingUserResponse(orderService.getMyOrders(request));
    }

    @Override
    @Operation(summary = "Hủy đơn hàng", description = "Customer hoặc Admin hủy đơn hàng")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public Response<OrderUserResponse> cancelOrder(CancelOrderRequest request) {
        log.info("Yêu cầu REST hủy đơn hàng: {}", request.getOrderId());
        return toUserResponse(orderService.cancelOrder(request));
    }

    /* ==================== Admin Endpoints ==================== */

    @Override
    @Operation(summary = "Tìm kiếm đơn hàng", description = "Admin tìm kiếm và lọc đơn hàng")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<PagingResponse<OrderAdminResponse>> searchOrders(OrderSearchRequest request) {
        log.info("Yêu cầu REST tìm kiếm đơn hàng");
        return toPagingAdminResponse(orderService.searchOrders(request));
    }

    @Override
    @Operation(summary = "Cập nhật thông tin vận chuyển", description = "Admin cập nhật thông tin vận chuyển")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<OrderAdminResponse> updateShipping(UpdateShippingRequest request) {
        log.info("Yêu cầu REST cập nhật thông tin vận chuyển: {}", request.getOrderId());
        return toAdminResponse(orderService.updateShipping(request));
    }

    @Override
    @Operation(summary = "Cập nhật ngày giao hàng", description = "Admin cập nhật ngày giao hàng")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<OrderAdminResponse> updateDelivery(UpdateDeliveryRequest request) {
        log.info("Yêu cầu REST cập nhật ngày giao hàng: {}", request.getOrderId());
        return toAdminResponse(orderService.updateDelivery(request));
    }

    @Override
    @Operation(summary = "Cập nhật ghi chú quản trị", description = "Admin cập nhật ghi chú")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<OrderAdminResponse> updateAdminNotes(UpdateAdminNotesRequest request) {
        log.info("Yêu cầu REST cập nhật ghi chú quản trị: {}", request.getOrderId());
        return toAdminResponse(orderService.updateAdminNotes(request));
    }

    @Override
    @Operation(summary = "Xác nhận đơn hàng", description = "Admin xác nhận đơn hàng")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<OrderAdminResponse> confirmOrder(ConfirmOrderRequest request) {
        log.info("Yêu cầu REST xác nhận đơn hàng: {}", request.getOrderId());
        return toAdminResponse(orderService.confirmOrder(request));
    }

    @Override
    @Operation(summary = "Hoàn thành đơn hàng", description = "Admin hoàn thành đơn hàng")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<OrderAdminResponse> completeOrder(CompleteOrderRequest request) {
        log.info("Yêu cầu REST hoàn thành đơn hàng: {}", request.getOrderId());
        return toAdminResponse(orderService.completeOrder(request));
    }

    @Override
    @Operation(summary = "Lấy đơn hàng chờ xử lý", description = "Admin xem danh sách đơn hàng cần xử lý")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<List<OrderAdminResponse>> getPendingOrders() {
        log.info("Yêu cầu REST lấy danh sách đơn hàng chờ xử lý");
        Response<List<OrderDto>> res = orderService.getPendingOrders();
        if (res.getData() == null)
            return Response.fail(res.getStatus());
        return Response
                .ok(res.getData().stream().map(orderMapper::toAdminResponseFromDto).collect(Collectors.toList()));
    }

    @Override
    @Operation(summary = "Lấy đơn hàng đang giao", description = "Admin xem danh sách đơn hàng đang giao")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<List<OrderAdminResponse>> getInProgressOrders() {
        log.info("Yêu cầu REST lấy danh sách đơn hàng đang giao");
        Response<List<OrderDto>> res = orderService.getInProgressOrders();
        if (res.getData() == null)
            return Response.fail(res.getStatus());
        return Response
                .ok(res.getData().stream().map(orderMapper::toAdminResponseFromDto).collect(Collectors.toList()));
    }

    @Override
    @Operation(summary = "Thống kê đơn hàng", description = "Admin xem thống kê đơn hàng theo thời gian")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<?> getOrderStatistics(String startDate, String endDate) {
        log.info("Yêu cầu REST lấy thống kê đơn hàng từ {} đến {}", startDate, endDate);
        return orderService.getOrderStatistics(startDate, endDate);
    }

    /*
     * ==================== Order Status Transitions (Dashboard)
     * ====================
     */

    @Override
    @Operation(summary = "Chuyển trạng thái đơn hàng", description = "Admin chuyển trạng thái (PROCESSING, DELIVERED, READY_FOR_PICKUP, RETURNING, RETURNED, COMPLETED). SHIPPED dùng /ship")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<OrderAdminResponse> transitionOrder(TransitionOrderRequest request) {
        log.info("Yêu cầu REST chuyển trạng thái đơn hàng {} → {}", request.getOrderId(), request.getTargetStatus());
        orderStatusHandler.transitionFromDashboard(request.getOrderId(), request.getTargetStatus(), request.getNote());

        // Re-fetch và trả về response mới nhất
        Response<OrderDto> orderRes = orderService.getOrderById(request.getOrderId());
        return toAdminResponse(orderRes);
    }

    @Override
    @Operation(summary = "Giao hàng", description = "Admin giao đơn cho tài xế → SHIPPED. Tự sinh delivery link + PIN")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<?> shipOrder(TransitionOrderRequest request) {
        log.info("Yêu cầu REST giao đơn hàng {} cho shipper {}", request.getOrderId(), request.getShipperId());

        String token = orderStatusHandler.transitionToShipped(
                request.getOrderId(), request.getShipperId(), request.getNote());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderId", request.getOrderId());
        result.put("deliveryToken", token);
        result.put("deliveryUrl", "/api/delivery/" + token);
        result.put("message", "Đã giao đơn cho tài xế. Gửi link cho shipper để bắt đầu giao hàng");

        return Response.ok(result);
    }

    @Override
    @Operation(summary = "Xem PIN shipper", description = "Admin xem mã PIN hiện tại của shipper")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<?> getDeliveryPin(String orderNumber) {
        log.info("Yêu cầu REST xem PIN giao hàng cho đơn: {}", orderNumber);
        Map<String, Object> pinInfo = orderStatusHandler.getDeliveryPin(orderNumber);
        return Response.ok(pinInfo);
    }

    @Override
    @Operation(summary = "Xóa PIN shipper", description = "Admin xóa PIN → shipper mở link lại sẽ thấy tạo PIN mới")
    @PreAuthorize("hasRole('ADMIN')")
    public Response<?> clearDeliveryPin(String orderNumber) {
        log.info("Yêu cầu REST xóa PIN giao hàng cho đơn: {}", orderNumber);
        orderStatusHandler.clearDeliveryPin(orderNumber);
        return Response.ok(null, "Đã xóa PIN. Shipper sẽ thấy màn hình tạo PIN mới khi mở link");
    }
}
