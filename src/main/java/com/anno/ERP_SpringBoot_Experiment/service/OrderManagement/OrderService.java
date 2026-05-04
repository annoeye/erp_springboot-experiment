package com.anno.ERP_SpringBoot_Experiment.service.OrderManagement;

import com.anno.ERP_SpringBoot_Experiment.event.producer.OrderKafkaProducer;
import com.anno.ERP_SpringBoot_Experiment.mapper.OrderMapper;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.entity.*;
import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.SearchOperation;
import com.anno.ERP_SpringBoot_Experiment.repository.*;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.SearchCriteria;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.SpecificationBuilder;
import com.anno.ERP_SpringBoot_Experiment.service.BillService.BillService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.OrderDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.kafkaDtos.CustomerInfo;
import com.anno.ERP_SpringBoot_Experiment.service.dto.kafkaDtos.OrderEventDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.kafkaDtos.PaymentOptions;
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    private final OrderKafkaProducer orderKafkaProducer;

    @Override
    @Transactional
    public Response<OrderDto> createOrder(CreateOrderRequest request) {
        List<SearchCriteria> criteriaList = new ArrayList<>();

        User customer = securityUtil.getCurrentUser()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập để đặt hàng"));

        criteriaList.add(new SearchCriteria(
                "sku.sku",
                SearchOperation.IN.getSymbol(),
                request.getItems().stream()
                        .map(CreateOrderRequest.OrderItemRequest::getAttributesSku)
                        .toList()));

        SpecificationBuilder<Attributes> builder = new SpecificationBuilder<>(criteriaList);
        Specification<Attributes> spec = builder.build();
        List<Attributes> attributes = attributesRepository.findAll(spec);

        List<Integer> orderItemQuantity = request.getItems().stream()
                .map(CreateOrderRequest.OrderItemRequest::getQuantity)
                .toList();

        if (attributes.size() != orderItemQuantity.size()) {
            throw new BusinessException(ErrorCode.ATTRIBUTES_OUT_OF_STOCK, "Lỗi sản phẩm không tồn tại.");
        }

        if (!IntStream.range(0, attributes.size())
                .allMatch(i -> attributes.get(i).getStockQuantity() >= orderItemQuantity.get(i))) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "Sản phẩm không đủ số lượng tồn kho");
        }

        Order order = buildOrderFromRequest(request, customer, null);
        List<OrderItem> orderItems = createOrderItems(request.getItems(), order);
        order.getOrderItems().addAll(orderItems);
        order.calculateTotals();

        orderMapper.toDto(orderRepository.save(order));

        log.info("Order created successfully");
        HttpServletRequest httpServletRequest = null;
        try {
            httpServletRequest = ((ServletRequestAttributes) RequestContextHolder
                    .getRequestAttributes()).getRequest();
        } catch (Exception e) {
            log.warn("Không lấy được HttpServletRequest");
        }

        String language = "vn";
        if (httpServletRequest != null && httpServletRequest.getLocale() != null) {
            String clientLang = httpServletRequest.getLocale().getLanguage();
            if ("en".equalsIgnoreCase(clientLang)) {
                language = "en";
            }
        }

        // (shipping method) nhan tai cua hang || giao hang (tinh tien dua vao ~)
        OrderEventDto eventDto = OrderEventDto.builder()
                .paymentProvider(order.getShippingMethod())
                .amount(order.getTotalAmount())
                .currency(!(order.getShippingMethod().equals("PAYPAL")) ? "VND" : "USD") // nen chua lam
                .orderId(order.getOrderNumber())
                .orderDescription(order.getCustomerNotes() != null ? order.getCustomerNotes()
                        : "Thanh toan don hang " + order.getOrderNumber())
                .customerInfo(CustomerInfo.builder()
                        .appUserId(securityUtil.getCurrentUserId())
                        .ipAddress(securityUtil.getIpAddress())
                        .language(language)
                        .build())
                .paymentOptions(PaymentOptions.builder()
                        .paymentMethod(request.getPaymentMethod().toString().toUpperCase())
                        .extraData("Đơn hàng: " + order.getOrderNumber())
                        .build())
                .build();

        log.info("===> Đang chuẩn bị gửi Kafka cho đơn hàng: {}", eventDto.getOrderId());
        orderKafkaProducer.sendOrderCreatedEvent(eventDto);
        log.info("===> Đã gọi lệnh gửi Kafka xong!");
        return Response.ok("Tạo thành công đơn đặt hàng");
    }

    @Override
    @Transactional
    public Response<OrderDto> createOrderFromCart(String cartId, CreateOrderRequest request) {
        log.info("Creating order from cart: {}", cartId);

        User customer = securityUtil.getCurrentUser()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập"));

        ShoppingCart cart = shoppingCartRepository.findById(convertStringToLong(cartId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy giỏ hàng"));

        if (!cart.getUser().getId().equals(customer.getId())) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "Bạn không có quyền truy cập giỏ hàng này");
        }

        // Tạo order từ cart items
        Order order = buildOrderFromRequest(request, customer, null);

        // Convert cart items to order items
        List<OrderItem> orderItems = cart.getItems().stream()
                .map(item -> {
                    Attributes attributes = attributesRepository.findById(convertStringToLong(item.getAttributesId()))
                            .orElseThrow(() -> new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND,
                                    "Không tìm thấy sản phẩm"));
                    return buildOrderItem(attributes, item.getQuantity(), order);
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

        Booking booking = bookingRepository.findById(convertStringToLong(bookingId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy booking"));

        Order order = buildOrderFromRequest(request, customer, null);
        order.setBookingId(bookingId);

        // Convert booking products to order items
        List<OrderItem> orderItems = booking.getProducts().stream()
                .map(item -> {
                    Attributes attributes = attributesRepository.findById(convertStringToLong(item.getAttributesId()))
                            .orElseThrow(() -> new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND,
                                    "Không tìm thấy sản phẩm"));
                    return buildOrderItem(attributes, item.getQuantity(), order);
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
        Order order = orderRepository.findById(convertStringToLong(orderId))
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
        Order order = orderRepository.findById(convertStringToLong(request.getOrderId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (request.getStatus() != null) {
            validateStatusTransition(order.getCurrentStatus(), request.getStatus());
            order.appendStatus(request.getStatus());
            order.getAuditInfo().addUpdateEntry("Cập nhật trạng thái đơn hàng: " + request.getStatus(), securityUtil.getCurrentUsername());
        }

        if (request.getShippingInfo() != null) {
            order.setShippingInfo(request.getShippingInfo());
            order.getAuditInfo().addUpdateEntry("Cập nhật thông tin giao hàng", securityUtil.getCurrentUsername());
        }

        if (request.getAdminNotes() != null) {
            order.setAdminNotes(request.getAdminNotes());
            order.getAuditInfo().addUpdateEntry("Cập nhật ghi chú quản trị", securityUtil.getCurrentUsername());
        }

        Order savedOrder = orderRepository.save(order);
        return Response.ok(orderMapper.toDto(savedOrder));
    }
    //
    // @Override
    // @Transactional
    // public Response<OrderDto> updateOrderStatus(String orderId, OrderStatus
    // newStatus) {
    // Order order = orderRepository.findById(convertStringToLong(orderId))
    // .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không
    // tìm thấy đơn hàng"));
    //
    // validateStatusTransition(order.getStatus(), newStatus);
    // order.setStatus(newStatus);
    //
    // // Cập nhật timestamps tương ứng
    // updateStatusTimestamps(order, newStatus);
    //
    // Order savedOrder = orderRepository.save(order);
    // log.info("Order {} status updated to {}", order.getOrderNumber(), newStatus);
    // return Response.ok(orderMapper.toDto(savedOrder));
    // }

    @Override
    @Transactional
    public Response<OrderDto> confirmOrder(String orderId) {
        Order order = orderRepository.findById(convertStringToLong(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (order.getCurrentStatus() != OrderStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Chỉ có thể xác nhận đơn hàng ở trạng thái PENDING");
        }

        order.appendStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());
        order.setConfirmedBy(securityUtil.getCurrentUser().map(u -> u.getId().toString()).orElse(null));
        order.getAuditInfo().addUpdateEntry("Xác nhận đơn hàng", securityUtil.getCurrentUsername());

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} confirmed", order.getOrderNumber());
        return Response.ok(orderMapper.toDto(savedOrder));
    }

    @Override
    @Transactional
    public Response<OrderDto> cancelOrder(CancelOrderRequest request) {
        Order order = orderRepository.findById(convertStringToLong(request.getOrderId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (!order.canBeCancelled()) {
            throw new BusinessException(ErrorCode.ORDER_CANNOT_BE_MODIFIED,
                    "Không thể hủy đơn hàng ở trạng thái " + order.getCurrentStatus());
        }

        order.appendStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(request.getCancellationReason());
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelledBy(securityUtil.getCurrentUser().map(u -> u.getId().toString()).orElse(null));
        order.getAuditInfo().addUpdateEntry("Hủy đơn hàng: " + request.getCancellationReason(), securityUtil.getCurrentUsername());

        Order savedOrder = orderRepository.save(order);
        log.info("Order {} cancelled: {}", order.getOrderNumber(), request.getCancellationReason());
        return Response.ok(orderMapper.toDto(savedOrder));
    }

    // @Override
    // @Transactional
    // public Response<OrderDto> markAsDelivered(String orderId) {
    // Order order = orderRepository.findById(convertStringToLong(orderId))
    // .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không
    // tìm thấy đơn hàng"));
    //
    // if (order.getStatus() != OrderStatus.SHIPPED) {
    // throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION,
    // "Chỉ có thể đánh dấu đã giao cho đơn hàng đang giao");
    // }
    //
    // order.setStatus(OrderStatus.DELIVERED);
    //
    // Order savedOrder = orderRepository.save(order);
    // log.info("Order {} marked as delivered", order.getOrderNumber());
    //
    // // ✅ TỰ ĐỘNG TẠO BILL KHI GIAO HÀNG THÀNH CÔNG
    // try {
    // Bill bill =
    // billService.createBillForCODOrder(savedOrder.getId().toString().replace("-",
    // ""));
    // log.info("✅ Bill auto-created for Order: {} -> Bill ID: {}",
    // savedOrder.getOrderNumber(), bill.getId());
    // } catch (Exception e) {
    // // Log error nhưng không fail transaction của Order
    // log.error("❌ Failed to create Bill for Order {}: {}",
    // savedOrder.getOrderNumber(), e.getMessage());
    // // Không throw exception để không ảnh hưởng đến việc cập nhật Order status
    // }
    //
    // return Response.ok(orderMapper.toDto(savedOrder));
    // }

    @Override
    @Transactional
    public Response<OrderDto> completeOrder(String orderId) {
        Order order = orderRepository.findById(convertStringToLong(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (order.getCurrentStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.INVALID_STATUS_TRANSITION, "Chỉ có thể hoàn thành đơn hàng đã giao");
        }

        order.appendStatus(OrderStatus.COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        order.getAuditInfo().addUpdateEntry("Hoàn thành đơn hàng", securityUtil.getCurrentUsername());

        Order savedOrder = orderRepository.save(order);

        // ✅ CẬP NHẬT ANALYTICS CHO PRODUCT VÀ ATTRIBUTES
        updateProductAnalytics(savedOrder);

        log.info("Order {} completed", order.getOrderNumber());
        return Response.ok(orderMapper.toDto(savedOrder));
    }

    /**
     * Cập nhật analytics cho Product và Attributes khi Order hoàn thành
     * Sử dụng Atomic Update để đảm bảo tính chính xác và hiệu năng
     */
    private void updateProductAnalytics(Order order) {
        log.info("📊 Processing analytics for Order: {}", order.getOrderNumber());
        for (OrderItem item : order.getOrderItems()) {
            try {
                Long productId = item.getProduct().getId();
                Long attributesId = item.getAttributes().getId();

                // 1. Cập nhật Product analytics (Atomic)
                productRepository.updateTotalSoldQuantity(productId, item.getQuantity());
                productRepository.updateTotalRevenue(productId, BigDecimal.valueOf(item.getSubtotal()));
                productRepository.updateTotalOrders(productId);

                // 2. Cập nhật Attributes analytics (Atomic)
                attributesRepository.updateSoldQuantity(attributesId, item.getQuantity());
                attributesRepository.updateTotalOrders(attributesId);

                log.debug("✅ Updated analytics for Product ID: {} and Attribute ID: {}", productId, attributesId);
            } catch (Exception e) {
                log.error("❌ Failed to update analytics for OrderItem {}: {}", item.getId(), e.getMessage());
                // Không throw exception để tránh rollback cả đơn hàng nếu chỉ lỗi thống kê
            }
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

    private Order buildOrderFromRequest(CreateOrderRequest request, User customer, Address address) {
        return Order.builder()
                .orderNumber(generateOrderNumber())
                .status(new ArrayList<>(List.of(OrderStatus.PENDING)))
                .customer(customer)
                .customerName(customer.getFullName())
                .customerEmail(customer.getEmail())
                .customerPhone(customer.getPhoneNumber())
                .shippingMethod(request.getShippingMethod())
                .shippingInfo(address)
                .customerNotes(request.getCustomerNotes())
                .discountCode(request.getDiscountCode()) // chưa làm
                .shippingFee(30000.0) // chưa làm
                .auditInfo(new AuditInfo())
                .build();
    }

    private List<OrderItem> createOrderItems(List<CreateOrderRequest.OrderItemRequest> itemRequests, Order order) {
        return itemRequests.stream()
                .map(itemRequest -> {
                    Attributes attributes = attributesRepository
                            .findAttributesBySku_sku(itemRequest.getAttributesSku())
                            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND,
                                    STR."Không tìm thấy sản phẩm với ID: \{itemRequest.getAttributesSku()}"));

                    // Kiểm tra số lượng hợp lệ
                    if (itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {
                        throw new BusinessException(ErrorCode.INVALID_QUANTITY, "Số lượng sản phẩm phải lớn hơn 0");
                    }

                    // Kiểm tra tồn kho
                    if (attributes.getStockQuantity() < itemRequest.getQuantity()) {
                        throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                                "Sản phẩm " + attributes.getSku().getSku() + " không đủ số lượng");
                    }

                    return buildOrderItem(attributes, itemRequest.getQuantity(), order);
                })
                .collect(Collectors.toList());
    }

    private OrderItem buildOrderItem(Attributes attributes, Integer quantity, Order order) {
        Product product = attributes.getProduct();

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .product(product)
                .productName(product.getName())
                .attributes(attributes)
                .productSku(product.getSkuInfo().getSku())
                .attributesSku(attributes.getSku().getSku())
                .variantOptions(new java.util.ArrayList<>(attributes.getVariantOptions()))
                .quantity(quantity)
                .unitPrice(attributes.getPrice())
                .salePrice(attributes.getSalePrice())
                .discountAmount(0.0)
                .discountPercentage(0.0)
                .taxAmount(0.0)
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

    // private void updateStatusTimestamps(Order order, OrderStatus newStatus) {
    // String userId = securityUtil.getCurrentUser().map(u ->
    // u.getId().toString()).orElse(null);
    //
    // switch (newStatus) {
    // case CONFIRMED -> {
    // order.setConfirmedAt(LocalDateTime.now());
    // order.setConfirmedBy(userId);
    // }
    // case CANCELLED -> {
    // order.setCancelledAt(LocalDateTime.now());
    // order.setCancelledBy(userId);
    // }
    // case COMPLETED -> order.setCompletedAt(LocalDateTime.now());
    // case DELIVERED -> {
    // if (order.getShippingInfo() != null) {
    // order.getShippingInfo().setActualDeliveryDate(LocalDateTime.now());
    // }
    // }
    // }
    // }

    private Pageable createPageable(OrderSearchRequest request) {
        int page = request.getPage() != null ? request.getPage() : 0;
        int size = request.getSize() != null ? request.getSize() : 20;
        String sortBy = request.getSortBy() != null ? request.getSortBy() : "auditInfo.createdAt";
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
                predicates.add(cb.equal(root.get("customer").get("id"), convertStringToLong(request.getCustomerId())));
            }

            if (request.getCustomerName() != null) {
                predicates.add(cb.like(root.get("customerName"), "%" + request.getCustomerName() + "%"));
            }

            if (request.getOrderStatus() != null) {
                // Vì status giờ là TEXT lưu JSON ["PENDING", "PAID"], dùng LIKE để tìm kiếm
                predicates.add(cb.like(root.get("status").as(String.class), "%\"" + request.getOrderStatus().name() + "\"%"));
            }

            if (request.getStartDate() != null && request.getEndDate() != null) {
                predicates.add(cb.between(root.get("auditInfo").get("createdAt"), request.getStartDate(), request.getEndDate()));
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

    private Long convertStringToLong(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID không được để trống.");
        }
        try {
            return Long.valueOf(id.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("ID phải là một số nguyên hợp lệ.");
        }
    }
    @Transactional
    public void setStatus(String orderNumber, OrderStatus status) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Lỗi: Đơn hàng không tồn tại."));
        order.appendStatus(status);
        String updatedBy = "Hệ thống xử lý trạng thái đơn hàng.";
        order.getAuditInfo().addUpdateEntry(status.toString(), updatedBy);
    }
}
