package com.anno.ERP_SpringBoot_Experiment.web.rest;

import com.anno.ERP_SpringBoot_Experiment.service.dto.request.*;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.OrderAdminResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.OrderUserResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.PagingResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/orders")
public interface OrderController {

    /* ==================== Customer Order Operations ==================== */

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Response<OrderUserResponse> createOrder(@Valid @RequestBody CreateOrderRequest request);

    @GetMapping("/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    Response<OrderUserResponse> getOrderById(@PathVariable String orderId);

    @GetMapping("/number/{orderNumber}")
    @ResponseStatus(HttpStatus.OK)
    Response<OrderUserResponse> getOrderByOrderNumber(@PathVariable String orderNumber);

    @PostMapping("/my-orders")
    @ResponseStatus(HttpStatus.OK)
    Response<PagingResponse<OrderUserResponse>> getMyOrders(@RequestBody OrderSearchRequest request);

    @PostMapping("/cancel")
    @ResponseStatus(HttpStatus.OK)
    Response<OrderUserResponse> cancelOrder(@Valid @RequestBody CancelOrderRequest request);

    /* ==================== Admin Order Operations ==================== */

    @PostMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    Response<PagingResponse<OrderAdminResponse>> searchOrders(@RequestBody OrderSearchRequest request);

    @PutMapping("/shipping")
    @ResponseStatus(HttpStatus.OK)
    Response<OrderAdminResponse> updateShipping(@Valid @RequestBody UpdateShippingRequest request);

    @PutMapping("/delivery")
    @ResponseStatus(HttpStatus.OK)
    Response<OrderAdminResponse> updateDelivery(@Valid @RequestBody UpdateDeliveryRequest request);

    @PutMapping("/admin-notes")
    @ResponseStatus(HttpStatus.OK)
    Response<OrderAdminResponse> updateAdminNotes(@Valid @RequestBody UpdateAdminNotesRequest request);

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.OK)
    Response<OrderAdminResponse> confirmOrder(@Valid @RequestBody ConfirmOrderRequest request);

    @PostMapping("/complete")
    @ResponseStatus(HttpStatus.OK)
    Response<OrderAdminResponse> completeOrder(@Valid @RequestBody CompleteOrderRequest request);

    @GetMapping("/pending")
    @ResponseStatus(HttpStatus.OK)
    Response<List<OrderAdminResponse>> getPendingOrders();

    @GetMapping("/in-progress")
    @ResponseStatus(HttpStatus.OK)
    Response<List<OrderAdminResponse>> getInProgressOrders();

    @GetMapping("/statistics")
    @ResponseStatus(HttpStatus.OK)
    Response<?> getOrderStatistics(
            @RequestParam String startDate,
            @RequestParam String endDate);

    /*
     * ==================== Order Status Transitions (Dashboard)
     * ====================
     */

    /**
     * Dashboard Admin chuyển trạng thái đơn hàng.
     * Dùng cho: PROCESSING, DELIVERED, READY_FOR_PICKUP, RETURNING, RETURNED,
     * COMPLETED.
     * SHIPPED phải dùng endpoint riêng (cần thông tin tài xế).
     */
    @PostMapping("/transition")
    @ResponseStatus(HttpStatus.OK)
    Response<OrderAdminResponse> transitionOrder(@Valid @RequestBody TransitionOrderRequest request);

    /**
     * Dashboard Admin chuyển sang SHIPPED — bắt buộc có thông tin tài xế.
     * Hệ thống sẽ tự sinh delivery token + PIN và lưu vào Redis.
     */
    @PostMapping("/ship")
    @ResponseStatus(HttpStatus.OK)
    Response<?> shipOrder(@Valid @RequestBody TransitionOrderRequest request);

    /**
     * Admin xem PIN hiện tại của shipper.
     */
    @GetMapping("/delivery-pin/{orderNumber}")
    @ResponseStatus(HttpStatus.OK)
    Response<?> getDeliveryPin(@PathVariable String orderNumber);

    /**
     * Admin xóa PIN → shipper mở link lại sẽ thấy màn hình tạo PIN mới.
     * Dùng khi shipper quên PIN hoặc cần đổi máy.
     */
    @DeleteMapping("/delivery-pin/{orderNumber}")
    @ResponseStatus(HttpStatus.OK)
    Response<?> clearDeliveryPin(@PathVariable String orderNumber);
}
