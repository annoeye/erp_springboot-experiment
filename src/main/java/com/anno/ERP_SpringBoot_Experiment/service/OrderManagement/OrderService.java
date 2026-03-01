package com.anno.ERP_SpringBoot_Experiment.service.OrderManagement;

import com.anno.ERP_SpringBoot_Experiment.mapper.OrderMapper;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.PaymentInfo;
import com.anno.ERP_SpringBoot_Experiment.model.entity.*;
import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.PaymentStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.PaymentType;
import com.anno.ERP_SpringBoot_Experiment.repository.*;
import com.anno.ERP_SpringBoot_Experiment.service.BillService.BillService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.OrderDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CancelOrderRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateOrderRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.OrderSearchRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.UpdateOrderRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.PageableData;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.PagingResponse;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iOrder;
import com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService implements iOrder {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final AttributesRepository attributesRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final BookingRepository bookingRepository;
    private final OrderMapper orderMapper;
    private final SecurityUtil securityUtil;
    private final BillService billService;

    @Override
    @Transactional
    public Response<OrderDto> createOrder(CreateOrderRequest request) {
        log.info("Creating new order with {} items", request.getItems().size());

        // Lấy thông tin customer hiện tại
        User customer = securityUtil.getCurrentUser()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập để đặt hàng"));

        // Tạo order
        Order order = buildOrderFromRequest(request, customer);

        // Tạo order items
        List<OrderItem> orderItems = createOrderItems(request.getItems(), order);
        order.getOrderItems().addAll(orderItems);

        // Tính toán tổng tiền
        order.calculateTotals();

        // Lưu order
        Order savedOrder = orderRepository.save(order);

        log.info("Order created successfully: {}", savedOrder.getOrderNumber());
        return Response.ok(orderMapper.toDto(savedOrder));
    }

    @Override
    @Transactional
    public Response<OrderDto> createOrderFromCart(String cartId, CreateOrderRequest request) {
        log.info("Creating order from cart: {}", cartId);

        User customer = securityUtil.getCurrentUser()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập"));

        ShoppingCart cart = shoppingCartRepository.findById(convertStringToUUID(cartId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy giỏ hàng"));

        if (!cart.getUser().getId().equals(customer.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Bạn không có quyền truy cập giỏ hàng này");
        }

        // Tạo order từ cart items
        Order order = buildOrderFromRequest(request, customer);
        order.setShoppingCartId(cartId);

        // Convert cart items to order items
        List<OrderItem> orderItems = cart.getItems().stream()
                .map(item -> {
                    Attributes attributes = attributesRepository.findById(convertStringToUUID(item.getAttributesId()))
                            .orElseThrow(() -> new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND,
                                    "Không tìm thấy sản phẩm"));
                    return buildOrderItem(attributes, item.getQuantity(), order, null);
                })
                .toList();

        order.getOrderItems().addAll(orderItems);
        order.calculateTotals();

        Order savedOrder = orderRepository.save(order);

        // Xóa giỏ hàng sau khi tạo order
        cart.getItems().clear();
        cart.updateTotals(0, 0.0, 0.0);
        shoppingCartRepository.save(cart);

        log.info("Order created from cart successfully: {}", savedOrder.getOrderNumber());
        return Response.ok(orderMapper.toDto(savedOrder));
    }

    @Override
    @Transactional
    public Response<OrderDto> createOrderFromBooking(String bookingId, CreateOrderRequest request) {
        log.info("Creating order from booking: {}", bookingId);

        User customer = securityUtil.getCurrentUser()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập"));

        Booking booking = bookingRepository.findById(convertStringToUUID(bookingId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy booking"));

        Order order = buildOrderFromRequest(request, customer);
        order.setBookingId(bookingId);

        // Convert booking products to order items
        List<OrderItem> orderItems = booking.getProducts().stream()
                .map(item -> {
                    Attributes attributes = attributesRepository.findById(convertStringToUUID(item.getAttributesId()))
                            .orElseThrow(() -> new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND,
                                    "Không tìm thấy sản phẩm"));
                    return buildOrderItem(attributes, item.getQuantity(), order, null);
                })
                .toList();

        order.getOrderItems().addAll(orderItems);
        order.calculateTotals();

        Order savedOrder = orderRepository.save(order);

        log.info("Order created from booking successfully: {}", savedOrder.getOrderNumber());
        return Response.ok(orderMapper.toDto(savedOrder));
    }

    @Override
    public Response<OrderDto> getOrderById(String orderId) {
        Order order = orderRepository.findById(convertStringToUUID(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        // Kiểm tra quyền truy cập
        User currentUser = securityUtil.getCurrentUser().orElse(null);
        if (currentUser != null && !order.getCustomer().getId().equals(currentUser.getId())) {
            // Chỉ admin mới được xem order của người khác
            if (!securityUtil.hasRole("ADMIN")) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED, "Bạn không có quyền xem đơn hàng này");
            }
        }

        return Response.ok(orderMapper.toDto(order));
    }

    @Override
    public Response<OrderDto> getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        User currentUser = securityUtil.getCurrentUser().orElse(null);
        if (currentUser != null && !order.getCustomer().getId().equals(currentUser.getId())) {
            if (!securityUtil.hasRole("ADMIN")) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED, "Bạn không có quyền xem đơn hàng này");
            }
        }

        return Response.ok(orderMapper.toDto(order));
    }

    @Override
    public Response<PagingResponse<OrderDto>> getMyOrders(OrderSearchRequest request) {
        User customer = securityUtil.getCurrentUser()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập"));

        Pageable pageable = createPageable(request);
        Page<Order> orderPage = orderRepository.findByCustomerId(customer.getId(), pageable);

        return Response.ok(createPagingResponse(orderPage));
    }

    @Override
    public Response<PagingResponse<OrderDto>> searchOrders(OrderSearchRequest request) {
        Pageable pageable = createPageable(request);
        Specification<Order> spec = buildSpecification(request);

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        return Response.ok(createPagingResponse(orderPage));
    }

    @Override
    @Transactional
    public Response<OrderDto> updateOrder(UpdateOrderRequest request) {
        Order order = orderRepository.findById(convertStringToUUID(request.getOrderId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (request.getStatus() != null) {
            validateStatusTransition(order.getStatus(), request.getStatus());
            order.setStatus(request.getStatus());
        }

        if (request.getShippingInfo() != null) {
            order.setShippingInfo(request.getShippingInfo());
        }

        if (request.getAdminNotes() != null) {
            order.setAdminNotes(request.getAdminNotes());
        }

        if (request.getTrackingNumber() != null && order.getShippingInfo() != null) {
            order.getShippingInfo().setTrackingNumber(request.getTrackingNumber());
        }

        Order savedOrder = orderRepository.save(order);
        return Response.ok(orderMapper.toDto(savedOrder));
    }

    @Override
    @Transactional
    public Response<OrderDto> updateOrderStatus(String orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(convertStringToUUID(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        validateStatusTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);

        // Cập nhật timestamps tương ứng
        updateStatusTimestamps(order, newStatus);

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} status updated to {}", order.getOrderNumber(), newStatus);
        return Response.ok(orderMapper.toDto(savedOrder));
    }

    @Override
    @Transactional
    public Response<OrderDto> confirmOrder(String orderId) {
        Order order = orderRepository.findById(convertStringToUUID(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Chỉ có thể xác nhận đơn hàng ở trạng thái PENDING");
        }

        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());
        order.setConfirmedBy(securityUtil.getCurrentUser().map(u -> u.getId().toString()).orElse(null));

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} confirmed", order.getOrderNumber());
        return Response.ok(orderMapper.toDto(savedOrder));
    }

    @Override
    @Transactional
    public Response<OrderDto> cancelOrder(CancelOrderRequest request) {
        Order order = orderRepository.findById(convertStringToUUID(request.getOrderId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (!order.canBeCancelled()) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_BE_MODIFIED,
                    "Không thể hủy đơn hàng ở trạng thái " + order.getStatus());
        }

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(request.getCancellationReason());
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelledBy(securityUtil.getCurrentUser().map(u -> u.getId().toString()).orElse(null));

        // Cập nhật payment status
        if (order.getPaymentInfo() != null) {
            order.getPaymentInfo().setPaymentStatus(PaymentStatus.CANCELLED);
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} cancelled: {}", order.getOrderNumber(), request.getCancellationReason());
        return Response.ok(orderMapper.toDto(savedOrder));
    }

    @Override
    @Transactional
    public Response<OrderDto> markAsDelivered(String orderId) {
        Order order = orderRepository.findById(convertStringToUUID(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.SHIPPED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Chỉ có thể đánh dấu đã giao cho đơn hàng đang giao");
        }

        order.setStatus(OrderStatus.DELIVERED);
        if (order.getShippingInfo() != null) {
            order.getShippingInfo().setActualDeliveryDate(LocalDateTime.now());
        }

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} marked as delivered", order.getOrderNumber());

        // ✅ TỰ ĐỘNG TẠO BILL CHO COD/BNPL KHI GIAO HÀNG THÀNH CÔNG
        try {
            PaymentType paymentType = order.getPaymentInfo() != null
                    ? PaymentType.valueOf(String.valueOf(order.getPaymentInfo().getPaymentMethod()))
                    : PaymentType.PAYMENT_UPON_DELIVERY;

            // Chỉ tạo Bill cho COD và BUY_NOW_PAY_LATER
            if (paymentType == PaymentType.PAYMENT_UPON_DELIVERY ||
                    paymentType == PaymentType.BUY_NOW_PAY_LATER) {

                Bill bill = billService.createBillForCODOrder(savedOrder.getId().toString().replace("-", ""));
                log.info("✅ Bill auto-created for COD/BNPL Order: {} -> Bill ID: {}",
                        savedOrder.getOrderNumber(), bill.getId());
            } else {
                log.info("⚠️ Order {} is not COD/BNPL, skipping Bill creation", savedOrder.getOrderNumber());
            }
        } catch (Exception e) {
            // Log error nhưng không fail transaction của Order
            log.error("❌ Failed to create Bill for Order {}: {}", savedOrder.getOrderNumber(), e.getMessage());
            // Không throw exception để không ảnh hưởng đến việc cập nhật Order status
        }

        return Response.ok(orderMapper.toDto(savedOrder));
    }

    @Override
    @Transactional
    public Response<OrderDto> completeOrder(String orderId) {
        Order order = orderRepository.findById(convertStringToUUID(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "Chỉ có thể hoàn thành đơn hàng đã giao");
        }

        order.setStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        // ✅ CẬP NHẬT ANALYTICS CHO PRODUCT VÀ ATTRIBUTES
        updateProductAnalytics(savedOrder);

        log.info("Order {} completed", order.getOrderNumber());
        return Response.ok(orderMapper.toDto(savedOrder));
    }

    /**
     * Cập nhật analytics cho Product và Attributes khi Order hoàn thành
     * - Product: totalSoldQuantity, totalRevenue, totalOrders
     * - Attributes: soldQuantity
     */
    private void updateProductAnalytics(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            Attributes attributes = item.getAttributes();

            // Cập nhật Product analytics
            product.setTotalSoldQuantity(
                    (product.getTotalSoldQuantity() != null ? product.getTotalSoldQuantity() : 0)
                            + item.getQuantity());
            product.setTotalRevenue(
                    (product.getTotalRevenue() != null ? product.getTotalRevenue() : 0.0)
                            + item.getSubtotal());
            product.setTotalOrders(
                    (product.getTotalOrders() != null ? product.getTotalOrders() : 0) + 1);
            productRepository.save(product);

            // Cập nhật Attributes analytics
            attributes.setSoldQuantity(
                    (attributes.getSoldQuantity() != null ? attributes.getSoldQuantity() : 0)
                            + item.getQuantity());
            attributesRepository.save(attributes);

            log.info("📊 Updated analytics - Product: {} (+{} qty, +{} revenue), Attributes: {} (+{} sold)",
                    product.getSkuInfo().getSku(),
                    item.getQuantity(),
                    item.getSubtotal(),
                    attributes.getSku().getSku(),
                    item.getQuantity());
        }
    }

    @Override
    public Response<List<OrderDto>> getPendingOrders() {
        List<Order> orders = orderRepository.findPendingOrders();
        List<OrderDto> orderDtos = orders.stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
        return Response.ok(orderDtos);
    }

    @Override
    public Response<List<OrderDto>> getInProgressOrders() {
        List<Order> orders = orderRepository.findInProgressOrders();
        List<OrderDto> orderDtos = orders.stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());
        return Response.ok(orderDtos);
    }

    @Override
    public Response<?> getOrderStatistics(String startDate, String endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime start = LocalDateTime.parse(startDate, formatter);
        LocalDateTime end = LocalDateTime.parse(endDate, formatter);

        List<Object[]> statistics = orderRepository.getOrderStatisticsByDate(start, end);

        // TODO: Format statistics data
        return Response.ok(statistics);
    }

    /* ==================== Private Helper Methods ==================== */

    private Order buildOrderFromRequest(CreateOrderRequest request, User customer) {
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .customer(customer)
                .customerName(customer.getFullName())
                .customerEmail(customer.getEmail())
                .customerPhone(customer.getPhoneNumber())
                .shippingInfo(request.getShippingInfo())
                .customerNotes(request.getCustomerNotes())
                .discountCode(request.getDiscountCode())
                .bookingId(request.getBookingId())
                .shoppingCartId(request.getShoppingCartId())
                .auditInfo(new AuditInfo())
                .build();

        // Setup payment info
        PaymentInfo paymentInfo = PaymentInfo.builder()
                .paymentMethod(request.getPaymentMethod())
                .paymentStatus(PaymentStatus.UNPAID)
                .build();
        order.setPaymentInfo(paymentInfo);

        // Calculate shipping fee (TODO: implement shipping fee calculation)
        order.setShippingFee(30000.0); // Default shipping fee

        return order;
    }

    private List<OrderItem> createOrderItems(List<CreateOrderRequest.OrderItemRequest> itemRequests, Order order) {
        return itemRequests.stream()
                .map(itemRequest -> {
                    Attributes attributes = attributesRepository
                            .findById(convertStringToUUID(itemRequest.getAttributesId()))
                            .orElseThrow(() -> new BusinessException(
                                    "Không tìm thấy sản phẩm với ID: " + itemRequest.getAttributesId()));

                    // Kiểm tra tồn kho
                    if (attributes.getStockQuantity() < itemRequest.getQuantity()) {
                        throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                                "Sản phẩm " + attributes.getSku().getSku() + " không đủ số lượng");
                    }

                    return buildOrderItem(attributes, itemRequest.getQuantity(), order, itemRequest.getNotes());
                })
                .collect(Collectors.toList());
    }

    private OrderItem buildOrderItem(Attributes attributes, Integer quantity, Order order, String notes) {
        Product product = attributes.getProduct();

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .product(product)
                .attributes(attributes)
                .productSku(product.getSkuInfo().getSku())
                .attributesSku(attributes.getSku().getSku())
                .color(attributes.getColor())
                .option(attributes.getOption())
                .quantity(quantity)
                .unitPrice(attributes.getPrice())
                .salePrice(attributes.getSalePrice())
                .discountAmount(0.0)
                .discountPercentage(0.0)
                .taxAmount(0.0)
                .notes(notes)
                .build();

        // Get first image if available
        if (product.getMediaItems() != null && !product.getMediaItems().isEmpty()) {
            orderItem.setImageUrl(product.getMediaItems().getFirst().getUrl());
        }

        orderItem.calculateSubtotal();
        return orderItem;
    }

    private String generateOrderNumber() {
        String prefix = "ORD";
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));

        String orderNumber = prefix + "-" + datePart + "-" + randomPart;

        // Ensure uniqueness
        while (orderRepository.existsByOrderNumber(orderNumber)) {
            randomPart = String.format("%04d", (int) (Math.random() * 10000));
            orderNumber = prefix + "-" + datePart + "-" + randomPart;
        }

        return orderNumber;
    }

    private void validateStatusTransition(OrderStatus currentStatus, OrderStatus newStatus) {
        // Define valid transitions
        boolean isValid = switch (currentStatus) {
            case PENDING -> newStatus == OrderStatus.CONFIRMED || newStatus == OrderStatus.CANCELLED;
            case CONFIRMED -> newStatus == OrderStatus.PROCESSING || newStatus == OrderStatus.CANCELLED;
            case PROCESSING -> newStatus == OrderStatus.PACKED || newStatus == OrderStatus.CANCELLED;
            case PACKED -> newStatus == OrderStatus.SHIPPED;
            case SHIPPED -> newStatus == OrderStatus.DELIVERED || newStatus == OrderStatus.RETURNED;
            case DELIVERED -> newStatus == OrderStatus.COMPLETED || newStatus == OrderStatus.RETURNED;
            case COMPLETED -> newStatus == OrderStatus.RETURNED;
            default -> false;
        };

        if (!isValid) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Không thể chuyển trạng thái từ " + currentStatus + " sang " + newStatus);
        }
    }

    private void updateStatusTimestamps(Order order, OrderStatus newStatus) {
        String userId = securityUtil.getCurrentUser().map(u -> u.getId().toString()).orElse(null);

        switch (newStatus) {
            case CONFIRMED -> {
                order.setConfirmedAt(LocalDateTime.now());
                order.setConfirmedBy(userId);
            }
            case CANCELLED -> {
                order.setCancelledAt(LocalDateTime.now());
                order.setCancelledBy(userId);
            }
            case COMPLETED -> order.setCompletedAt(LocalDateTime.now());
            case DELIVERED -> {
                if (order.getShippingInfo() != null) {
                    order.getShippingInfo().setActualDeliveryDate(LocalDateTime.now());
                }
            }
        }
    }

    private Pageable createPageable(OrderSearchRequest request) {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "orderDate";
        String sortDirection = request.getSortDirection() != null ? request.getSortDirection() : "DESC";

        Sort sort = sortDirection.equalsIgnoreCase("ASC") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return PageRequest.of(page, size, sort);
    }

    private Specification<Order> buildSpecification(OrderSearchRequest request) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (request.getOrderNumber() != null) {
                predicates.add(cb.like(root.get("orderNumber"), "%" + request.getOrderNumber() + "%"));
            }

            if (request.getCustomerId() != null) {
                predicates.add(cb.equal(root.get("customer").get("id"), convertStringToUUID(request.getCustomerId())));
            }

            if (request.getCustomerName() != null) {
                predicates.add(cb.like(root.get("customerName"), "%" + request.getCustomerName() + "%"));
            }

            if (request.getOrderStatus() != null) {
                predicates.add(cb.equal(root.get("status"), request.getOrderStatus()));
            }

            if (request.getPaymentStatus() != null) {
                predicates.add(cb.equal(root.get("paymentInfo").get("paymentStatus"), request.getPaymentStatus()));
            }

            if (request.getStartDate() != null && request.getEndDate() != null) {
                predicates.add(cb.between(root.get("orderDate"), request.getStartDate(), request.getEndDate()));
            }

            if (request.getMinAmount() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), request.getMinAmount()));
            }

            if (request.getMaxAmount() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), request.getMaxAmount()));
            }

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }

    private PagingResponse<OrderDto> createPagingResponse(Page<Order> orderPage) {
        List<OrderDto> orderDtos = orderPage.getContent().stream()
                .map(orderMapper::toDto)
                .collect(Collectors.toList());

        PageableData pageableData = PageableData.builder()
                .pageNumber(orderPage.getNumber())
                .totalPages(orderPage.getTotalPages())
                .totalElements(orderPage.getTotalElements())
                .pageSize(orderPage.getSize())
                .build();

        return PagingResponse.<OrderDto>builder()
                .contents(orderDtos)
                .paging(pageableData)
                .build();
    }

    private UUID convertStringToUUID(String id) {
        if (id == null || id.length() != 32) {
            throw new IllegalArgumentException("Invalid ID format. Expected 32 characters.");
        }

        String formattedId = String.format("%s-%s-%s-%s-%s",
                id.substring(0, 8),
                id.substring(8, 12),
                id.substring(12, 16),
                id.substring(16, 20),
                id.substring(20, 32));

        return UUID.fromString(formattedId);
    }
}
