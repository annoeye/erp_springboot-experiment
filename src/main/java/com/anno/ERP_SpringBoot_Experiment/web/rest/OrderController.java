package com.anno.ERP_SpringBoot_Experiment.web.rest;

import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;
import com.anno.ERP_SpringBoot_Experiment.service.dto.OrderDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CancelOrderRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateOrderRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.OrderSearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateOrderRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Page.PagingResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/orders")
public interface OrderController {

    /* ==================== Customer Order Operations ==================== */

    /**
     * Tạo order mới
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    Response<OrderDto> createOrder(@Valid @RequestBody CreateOrderRequest request);

    /**
     * Tạo order từ shopping cart
     */
    @PostMapping("/from-cart/{cartId}")
    @ResponseStatus(HttpStatus.CREATED)
    Response<OrderDto> createOrderFromCart(
            @PathVariable String cartId,
            @Valid @RequestBody CreateOrderRequest request
    );

    /**
     * Tạo order từ booking
     */
    @PostMapping("/from-booking/{bookingId}")
    @ResponseStatus(HttpStatus.CREATED)
    Response<OrderDto> createOrderFromBooking(
            @PathVariable String bookingId,
            @Valid @RequestBody CreateOrderRequest request
    );

    /**
     * Lấy thông tin order theo ID
     */
    @GetMapping("/{orderId}")
    @ResponseStatus(HttpStatus.OK)
    Response<OrderDto> getOrderById(@PathVariable String orderId);

    /**
     * Lấy thông tin order theo order number
     */
    @GetMapping("/number/{orderNumber}")
    @ResponseStatus(HttpStatus.OK)
    Response<OrderDto> getOrderByOrderNumber(@PathVariable String orderNumber);

    /**
     * Lấy danh sách orders của customer hiện tại
     */
    @PostMapping("/my-orders")
    @ResponseStatus(HttpStatus.OK)
    Response<PagingResponse<OrderDto>> getMyOrders(@RequestBody OrderSearchRequest request);

    /**
     * Hủy order
     */
    @PostMapping("/cancel")
    @ResponseStatus(HttpStatus.OK)
    Response<OrderDto> cancelOrder(@Valid @RequestBody CancelOrderRequest request);

    /* ==================== Admin Order Operations ==================== */

    /**
     * Tìm kiếm orders (Admin)
     */
    @PostMapping("/search")
    @ResponseStatus(HttpStatus.OK)
    Response<PagingResponse<OrderDto>> searchOrders(@RequestBody OrderSearchRequest request);

    /**
     * Cập nhật order (Admin)
     */
    @PutMapping
    @ResponseStatus(HttpStatus.OK)
    Response<OrderDto> updateOrder(@Valid @RequestBody UpdateOrderRequest request);

    /**
     * Cập nhật status của order (Admin)
     */
    @PatchMapping("/{orderId}/status")
    @ResponseStatus(HttpStatus.OK)
    Response<OrderDto> updateOrderStatus(
            @PathVariable String orderId,
            @RequestParam OrderStatus status
    );

    /**
     * Xác nhận order (Admin)
     */
    @PostMapping("/{orderId}/confirm")
    @ResponseStatus(HttpStatus.OK)
    Response<OrderDto> confirmOrder(@PathVariable String orderId);

    /**
     * Đánh dấu order đã giao hàng (Admin/Shipper)
     */
    @PostMapping("/{orderId}/delivered")
    @ResponseStatus(HttpStatus.OK)
    Response<OrderDto> markAsDelivered(@PathVariable String orderId);

    /**
     * Hoàn thành order (Admin)
     */
    @PostMapping("/{orderId}/complete")
    @ResponseStatus(HttpStatus.OK)
    Response<OrderDto> completeOrder(@PathVariable String orderId);

    /**
     * Lấy danh sách orders cần xử lý (Admin)
     */
    @GetMapping("/pending")
    @ResponseStatus(HttpStatus.OK)
    Response<List<OrderDto>> getPendingOrders();

    /**
     * Lấy danh sách orders đang giao hàng (Admin)
     */
    @GetMapping("/in-progress")
    @ResponseStatus(HttpStatus.OK)
    Response<List<OrderDto>> getInProgressOrders();

    /**
     * Thống kê orders (Admin)
     */
    @GetMapping("/statistics")
    @ResponseStatus(HttpStatus.OK)
    Response<?> getOrderStatistics(
            @RequestParam String startDate,
            @RequestParam String endDate
    );
}
