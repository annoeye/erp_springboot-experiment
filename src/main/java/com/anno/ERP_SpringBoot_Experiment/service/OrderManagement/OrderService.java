package com.anno.ERP_SpringBoot_Experiment.service.OrderManagement;

import com.anno.ERP_SpringBoot_Experiment.event.producer.OrderKafkaProducer;
import com.anno.ERP_SpringBoot_Experiment.mapper.OrderMapper;
import com.anno.ERP_SpringBoot_Experiment.model.embedded.AuditInfo;
import com.anno.ERP_SpringBoot_Experiment.model.entity.*;
import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;
import com.anno.ERP_SpringBoot_Experiment.model.enums.PaymentMethod;
import com.anno.ERP_SpringBoot_Experiment.model.enums.SearchOperation;
import com.anno.ERP_SpringBoot_Experiment.repository.*;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.SearchCriteria;
import com.anno.ERP_SpringBoot_Experiment.repository.specification.SpecificationBuilder;
import com.anno.ERP_SpringBoot_Experiment.service.BillService.BillService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.OrderDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.kafkaDtos.CustomerInfo;
import com.anno.ERP_SpringBoot_Experiment.service.dto.kafkaDtos.OrderEventDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.kafkaDtos.PaymentOptions;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.*;
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
    private final OrderStatusHandler orderStatusHandler;

    @Override
    @Transactional
    public Response<OrderDto> createOrder(CreateOrderRequest request) {
        log.info("Đang tạo đơn hàng...");

        User customer = securityUtil.getCurrentUser()
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập để đặt hàng"));

        Order order = buildOrderFromRequest(request, customer, null); // Address logic should be handled appropriately

        List<OrderItem> orderItems = new ArrayList<>();

        if (request.isFromCart()) {
            // Logic cho Cart (tự động lấy theo user đang đăng nhập)
            ShoppingCart cart = shoppingCartRepository.findByUser(customer)
                    .orElseThrow(
                            () -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy giỏ hàng của bạn"));

            if (cart.getItems() == null || cart.getItems().isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Giỏ hàng của bạn đang trống");
            }

            orderItems = cart.getItems().stream()
                    .map(item -> {
                        Attributes attributes = attributesRepository
                                .findById(convertStringToLong(item.getAttributesId()))
                                .orElseThrow(() -> new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND,
                                        "Không tìm thấy sản phẩm"));
                        return buildOrderItem(attributes, item.getQuantity(), order);
                    })
                    .toList();

            // Xóa giỏ hàng
            cart.getItems().clear();
            cart.updateTotals(0, 0.0, 0.0);
            shoppingCartRepository.save(cart);

        } else if (request.getBookingId() != null && !request.getBookingId().isEmpty()) {
            // Logic cho Booking
            Booking booking = bookingRepository.findById(convertStringToLong(request.getBookingId()))
                    .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy booking"));
            order.setBookingId(request.getBookingId());

            orderItems = booking.getProducts().stream()
                    .map(item -> {
                        Attributes attributes = attributesRepository
                                .findById(convertStringToLong(item.getAttributesId()))
                                .orElseThrow(() -> new BusinessException(ErrorCode.ATTRIBUTES_NOT_FOUND,
                                        "Không tìm thấy sản phẩm"));
                        return buildOrderItem(attributes, item.getQuantity(), order);
                    })
                    .toList();
        } else {
            // Logic cho Items trực tiếp
            if (request.getItems() == null || request.getItems().isEmpty()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Danh sách sản phẩm không được trống");
            }

            List<SearchCriteria> criteriaList = new ArrayList<>();
            criteriaList.add(new SearchCriteria("sku.sku", SearchOperation.IN.getSymbol(),
                    request.getItems().stream().map(CreateOrderRequest.OrderItemRequest::getAttributesSku).toList()));

            Specification<Attributes> spec = new SpecificationBuilder<Attributes>(criteriaList).build();
            List<Attributes> attributesList = attributesRepository.findAll(spec);

            List<Integer> quantities = request.getItems().stream().map(CreateOrderRequest.OrderItemRequest::getQuantity)
                    .toList();

            if (attributesList.size() != quantities.size()) {
                throw new BusinessException(ErrorCode.ATTRIBUTES_OUT_OF_STOCK, "Lỗi sản phẩm không tồn tại.");
            }

            if (!IntStream.range(0, attributesList.size())
                    .allMatch(i -> attributesList.get(i).getStockQuantity() >= quantities.get(i))) {
                throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK, "Sản phẩm không đủ số lượng tồn kho");
            }

            orderItems = createOrderItems(request.getItems(), order);
        }

        order.setOrderItems(orderItems);
        for (OrderItem item : orderItems) {
            item.setOrder(order);
        }
        calculateOrderTotals(order);

        Order savedOrder = orderRepository.save(order);
        log.info("Tạo đơn hàng thành công: {}", savedOrder.getOrderNumber());

        sendKafkaEvent(savedOrder, request);

        return Response.ok(orderMapper.toDto(savedOrder));
    }

    @Override
    public Response<OrderDto> getOrderById(String orderId) {
        Order order = orderRepository.findById(convertStringToLong(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        checkOrderAccess(order);
        return Response.ok(orderMapper.toDto(order));
    }

    @Override
    public Response<OrderDto> getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        checkOrderAccess(order);
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

    /* ==================== UPDATE (Grouped) ==================== */

    @Override
    @Transactional
    public Response<OrderDto> updateShipping(UpdateShippingRequest request) {
        Order order = orderRepository.findById(convertStringToLong(request.getOrderId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (request.getShippingInfo() != null) {
            order.setShippingInfo(request.getShippingInfo());
        }
        if (request.getShippingMethod() != null) {
            order.setShippingMethod(request.getShippingMethod());
        }

        order.getAuditInfo().addUpdateEntry("Cập nhật thông tin vận chuyển", securityUtil.getCurrentUsername());
        return Response.ok(orderMapper.toDto(orderRepository.save(order)));
    }

    @Override
    @Transactional
    public Response<OrderDto> updateDelivery(UpdateDeliveryRequest request) {
        Order order = orderRepository.findById(convertStringToLong(request.getOrderId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (request.getEstimatedDeliveryDate() != null) {
            order.setEstimatedDeliveryDate(request.getEstimatedDeliveryDate());
        }
        if (request.getActualDeliveryDate() != null) {
            order.setActualDeliveryDate(request.getActualDeliveryDate());
        }

        order.getAuditInfo().addUpdateEntry("Cập nhật thông tin giao hàng", securityUtil.getCurrentUsername());
        return Response.ok(orderMapper.toDto(orderRepository.save(order)));
    }

    @Override
    @Transactional
    public Response<OrderDto> updateAdminNotes(UpdateAdminNotesRequest request) {
        Order order = orderRepository.findById(convertStringToLong(request.getOrderId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        if (request.getAdminNotes() != null) {
            order.setAdminNotes(request.getAdminNotes());
        }

        order.getAuditInfo().addUpdateEntry("Cập nhật ghi chú quản trị", securityUtil.getCurrentUsername());
        return Response.ok(orderMapper.toDto(orderRepository.save(order)));
    }

    @Override
    @Transactional
    public Response<OrderDto> confirmOrder(ConfirmOrderRequest request) {
        Order order = orderRepository.findById(convertStringToLong(request.getOrderId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        orderStatusHandler.transitionTo(order, OrderStatus.CONFIRMED,
                "Đơn hàng đã được xác nhận bởi người bán. Đang chờ xử lý");
        order.setConfirmedAt(request.getConfirmedAt() != null ? request.getConfirmedAt() : LocalDateTime.now());

        String confirmedBy = request.getConfirmedBy() != null ? request.getConfirmedBy()
                : securityUtil.getCurrentUser().map(u -> u.getId().toString()).orElse(null);
        order.setConfirmedBy(confirmedBy);

        return Response.ok(orderMapper.toDto(orderRepository.save(order)));
    }

    @Override
    @Transactional
    public Response<OrderDto> cancelOrder(CancelOrderRequest request) {
        Order order = orderRepository.findById(convertStringToLong(request.getOrderId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        String reason = request.getCancellationReason() != null ? request.getCancellationReason() : "Không có lý do";
        orderStatusHandler.transitionTo(order, OrderStatus.CANCELLED, "Đơn hàng đã bị hủy. Lý do: " + reason);
        order.setCancellationReason(reason);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelledBy(securityUtil.getCurrentUser().map(u -> u.getId().toString()).orElse(null));

        return Response.ok(orderMapper.toDto(orderRepository.save(order)));
    }

    @Override
    @Transactional
    public Response<OrderDto> completeOrder(CompleteOrderRequest request) {
        Order order = orderRepository.findById(convertStringToLong(request.getOrderId()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));

        orderStatusHandler.transitionTo(order, OrderStatus.COMPLETED, null);
        order.setCompletedAt(request.getCompletedAt() != null ? request.getCompletedAt() : LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);
        updateProductAnalytics(savedOrder);
        return Response.ok(orderMapper.toDto(savedOrder));
    }

    /* ==================== QUERIES & STATISTICS ==================== */

    @Override
    public Response<List<OrderDto>> getPendingOrders() {
        List<Order> orders = orderRepository.findPendingOrders();
        return Response.ok(orders.stream().map(orderMapper::toDto).collect(Collectors.toList()));
    }

    @Override
    public Response<List<OrderDto>> getInProgressOrders() {
        List<Order> orders = orderRepository.findInProgressOrders();
        return Response.ok(orders.stream().map(orderMapper::toDto).collect(Collectors.toList()));
    }

    @Override
    public Response<?> getOrderStatistics(String startDate, String endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime start = LocalDateTime.parse(startDate, formatter);
        LocalDateTime end = LocalDateTime.parse(endDate, formatter);
        List<Object[]> statistics = orderRepository.getOrderStatisticsByDate(start, end);
        return Response.ok(statistics);
    }

    @Transactional
    public void setStatus(String orderNumber, OrderStatus status) {
        orderStatusHandler.transitionTo(orderNumber, status);
    }

    /* ==================== PRIVATE HELPER METHODS ==================== */

    private Order buildOrderFromRequest(CreateOrderRequest request, User customer, Address address) {
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());

        List<OrderStatus> initialStatus = new ArrayList<>();
        initialStatus.add(OrderStatus.PENDING);
        order.setStatus(initialStatus);

        order.setCustomer(customer);
        order.setCustomerName(customer.getFullName());
        order.setCustomerEmail(customer.getEmail());
        order.setCustomerPhone(customer.getPhoneNumber());
        order.setShippingMethod(request.getShippingMethod());
        order.setShippingInfo(address); // TODO: fetch address by ID
        order.setCustomerNotes(request.getCustomerNotes());
        order.setDiscountCode(request.getDiscountCode());
        order.setShippingFee(30000.0);
        order.setAuditInfo(new AuditInfo());

        // Ghi audit entry ban đầu theo phương thức thanh toán (kiểu Shopee)
        String initialAudit = resolveInitialAuditMessage(request.getPaymentMethod());
        order.getAuditInfo().addUpdateEntry(initialAudit, "Hệ thống");

        return order;
    }

    private List<OrderItem> createOrderItems(List<CreateOrderRequest.OrderItemRequest> itemRequests, Order order) {
        return itemRequests.stream()
                .map(itemRequest -> {
                    Attributes attributes = attributesRepository
                            .findAttributesBySku_sku(itemRequest.getAttributesSku())
                            .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND,
                                    "Không tìm thấy sản phẩm với ID: " + itemRequest.getAttributesSku()));

                    if (itemRequest.getQuantity() == null || itemRequest.getQuantity() <= 0) {
                        throw new BusinessException(ErrorCode.INVALID_QUANTITY, "Số lượng sản phẩm phải lớn hơn 0");
                    }
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
                .variantOptions(new ArrayList<>(attributes.getVariantOptions()))
                .quantity(quantity)
                .unitPrice(attributes.getPrice())
                .salePrice(attributes.getSalePrice())
                .discountAmount(0.0)
                .discountPercentage(0.0)
                .taxAmount(0.0)
                .build();

        if (product.getMediaItems() != null && !product.getMediaItems().isEmpty()) {
            orderItem.setImageUrl(product.getMediaItems().getFirst().getUrl());
        }

        orderItem.calculateSubtotal();
        return orderItem;
    }

    private void calculateOrderTotals(Order order) {
        double subtotal = order.getOrderItems().stream()
                .mapToDouble(item -> item.getSubtotal() != null ? item.getSubtotal() : 0.0)
                .sum();
        double taxAmount = order.getOrderItems().stream()
                .mapToDouble(item -> item.getTaxAmount() != null ? item.getTaxAmount() : 0.0)
                .sum();

        double discountAmount = order.getDiscountAmount() != null ? order.getDiscountAmount() : 0.0;
        double shippingFee = order.getShippingFee() != null ? order.getShippingFee() : 0.0;

        double totalAmount = subtotal - discountAmount + shippingFee + taxAmount;

        order.setSubtotal(subtotal);
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(Math.max(0, totalAmount));
    }

    private String generateOrderNumber() {
        String prefix = "ORD";
        String datePart = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String randomPart = String.format("%04d", (int) (Math.random() * 10000));
        String orderNumber = prefix + "-" + datePart + "-" + randomPart;

        while (orderRepository.existsByOrderNumber(orderNumber)) {
            randomPart = String.format("%04d", (int) (Math.random() * 10000));
            orderNumber = prefix + "-" + datePart + "-" + randomPart;
        }

        return orderNumber;
    }

    /**
     * Xác định message audit ban đầu theo phương thức thanh toán.
     * - Online (VNPAY, CARD, PAYPAL...): "Đang chờ thanh toán"
     * - COD: "Thanh toán khi nhận hàng"
     * - Mặc định: thông báo chung
     */
    private String resolveInitialAuditMessage(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return "Đơn hàng đã được đặt thành công";
        }
        return switch (paymentMethod) {
            case COD -> "Đơn hàng đã được đặt thành công. Thanh toán khi nhận hàng";
            case VNPAY, CARD, PAYPAL, GOOGLE_PAY, APPLE_PAY -> "Đơn hàng đã được đặt thành công. Đang chờ thanh toán";
        };
    }

    private void checkOrderAccess(Order order) {
        User currentUser = securityUtil.getCurrentUser().orElse(null);
        if (currentUser != null && !order.getCustomer().getId().equals(currentUser.getId())) {
            if (!securityUtil.hasRole("ADMIN")) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED, "Bạn không có quyền xem đơn hàng này");
            }
        }
    }

    private void sendKafkaEvent(Order order, CreateOrderRequest request) {
        String language = request.getLanguage() != null ? request.getLanguage().toLowerCase() : "vn";

        OrderEventDto eventDto = OrderEventDto.builder()
                .paymentProvider(order.getShippingMethod())
                .amount(order.getTotalAmount())
                .currency(!"PAYPAL".equals(order.getShippingMethod()) ? "VND" : "USD")
                .orderId(order.getOrderNumber())
                .orderDescription(order.getCustomerNotes() != null ? order.getCustomerNotes()
                        : "Thanh toan don hang " + order.getOrderNumber())
                .customerInfo(CustomerInfo.builder()
                        .appUserId(securityUtil.getCurrentUserId())
                        .ipAddress(securityUtil.getIpAddress())
                        .language(language)
                        .build())
                .paymentOptions(PaymentOptions.builder()
                        .paymentMethod(
                                request.getPaymentMethod() != null ? request.getPaymentMethod().toString().toUpperCase()
                                        : "COD")
                        .bankCode(request.getBankCode())
                        .extraData("Đơn hàng: " + order.getOrderNumber())
                        .build())
                .build();

        log.info("===> Đang chuẩn bị gửi Kafka cho đơn hàng: {}", eventDto.getOrderId());
        try {
            orderKafkaProducer.sendOrderCreatedEvent(eventDto);
            log.info("===> Đã gọi lệnh gửi Kafka xong!");
        } catch (Exception e) {
            log.error("Lỗi gửi event kafka", e);
        }
    }

    private void updateProductAnalytics(Order order) {
        log.info("📊 Đang xử lý phân tích dữ liệu cho Đơn hàng: {}", order.getOrderNumber());
        for (OrderItem item : order.getOrderItems()) {
            try {
                Long productId = item.getProduct().getId();
                Long attributesId = item.getAttributes().getId();
                productRepository.updateTotalSoldQuantity(productId, item.getQuantity());
                productRepository.updateTotalRevenue(productId, BigDecimal.valueOf(item.getSubtotal()));
                productRepository.updateTotalOrders(productId);
                attributesRepository.updateSoldQuantity(attributesId, item.getQuantity());
                attributesRepository.updateTotalOrders(attributesId);
            } catch (Exception e) {
                log.error("❌ Lỗi khi cập nhật phân tích dữ liệu cho Sản phẩm {}: {}", item.getId(), e.getMessage());
            }
        }
    }

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
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
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
                predicates.add(
                        cb.like(root.get("status").as(String.class), "%\"" + request.getOrderStatus().name() + "\"%"));
            }
            if (request.getStartDate() != null && request.getEndDate() != null) {
                predicates.add(cb.between(root.get("auditInfo").get("createdAt"), request.getStartDate(),
                        request.getEndDate()));
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
        List<OrderDto> orderDtos = orderPage.getContent().stream().map(orderMapper::toDto).collect(Collectors.toList());
        PageableData pageableData = PageableData.builder()
                .pageNumber(orderPage.getNumber())
                .totalPages(orderPage.getTotalPages())
                .totalElements(orderPage.getTotalElements())
                .pageSize(orderPage.getSize())
                .build();
        return PagingResponse.<OrderDto>builder().contents(orderDtos).paging(pageableData).build();
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
}
