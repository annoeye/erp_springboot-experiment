package com.anno.ERP_SpringBoot_Experiment.service.BillService;

import com.anno.ERP_SpringBoot_Experiment.mapper.PaymentMapper;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Bill;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Order;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Payment;
import com.anno.ERP_SpringBoot_Experiment.model.enums.PaymentType;
import com.anno.ERP_SpringBoot_Experiment.repository.BillRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.OrderRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.PaymentRepository;
import com.anno.ERP_SpringBoot_Experiment.service.dto.PaymentDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateBillRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iBill;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillService implements iBill {

    private final BillRepository billRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    @Override
    @Transactional
    public Response<?> addBill(CreateBillRequest request, PaymentDto paymentDto) {
        UUID orderId = convertStringToUUID(request.getOrderId());

        // ✅ IDEMPOTENCY CHECK - Tránh tạo trùng Bill
        if (billRepository.existsByOrder_Id(orderId)) {
            log.warn("Bill already exists for Order ID: {}", orderId);
            Bill existingBill = billRepository.findByOrder_Id(orderId)
                    .orElseThrow(() -> new BusinessException("Lỗi hệ thống: Bill tồn tại nhưng không tìm thấy"));
            return Response.ok(existingBill);
        }

        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException("Order không tồn tại với ID: " + orderId));

        Payment payment = null;
        if (isOnlinePayment(request.getPaymentType())) {
            if (paymentDto != null) {
                payment = paymentMapper.toEntity(paymentDto);
                payment.setOrder(order);
                log.info("Creating Payment record for online payment: {}", request.getPaymentType());
            }
        }

        Bill bill = Bill.builder()
                .invoiceDate(LocalDateTime.now())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .address(request.getAddress() != null ? request.getAddress() : order.getShippingInfo().getAddress())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .grandTotal(order.getTotalAmount())
                .payment(payment)
                .paymentType(request.getPaymentType())
                .idAddress(request.getIdAddress())
                .order(order)
                .build();

        Bill savedBill = billRepository.save(bill);
        log.info("Bill created successfully. Bill ID: {}, Order ID: {}", savedBill.getId(), orderId);

        return Response.ok(savedBill);
    }

    /**
     * Tạo Bill cho đơn hàng COD sau khi giao thành công
     * Được gọi từ OrderService.markAsDelivered()
     * 
     * @param orderId ID của Order đã giao hàng thành công
     * @return Bill đã tạo
     */
    @Transactional
    public Bill createBillForCODOrder(String orderId) {
        UUID orderUuid = convertStringToUUID(orderId);

        // ✅ IDEMPOTENCY CHECK
        if (billRepository.existsByOrder_Id(orderUuid)) {
            log.warn("Bill already exists for COD Order ID: {}", orderId);
            return billRepository.findByOrder_Id(orderUuid)
                    .orElseThrow(() -> new BusinessException("Lỗi hệ thống: Bill tồn tại nhưng không tìm thấy"));
        }

        // Fetch Order
        Order order = orderRepository.findById(orderUuid)
                .orElseThrow(() -> new BusinessException("Order không tồn tại với ID: " + orderId));

        // ✅ BUSINESS RULE: Chỉ tạo Bill cho COD và BUY_NOW_PAY_LATER
        PaymentType paymentType = order.getPaymentInfo() != null
                ? PaymentType.valueOf(String.valueOf(order.getPaymentInfo().getPaymentMethod()))
                : PaymentType.PAYMENT_UPON_DELIVERY;

        if (!isCODOrBNPL(paymentType)) {
            throw new BusinessException(
                    "Chỉ tạo Bill cho COD/BNPL khi giao hàng. PaymentType hiện tại: " + paymentType);
        }

        Bill bill = Bill.builder()
                .invoiceDate(LocalDateTime.now())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .address(order.getShippingInfo() != null ? order.getShippingInfo().getAddress() : "")
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .grandTotal(order.getTotalAmount())
                .payment(null) // COD không có Payment entity
                .paymentType(paymentType)
                .idAddress(order.getShippingInfo() != null ? order.getShippingInfo().getAddress() : "")
                .order(order)
                .build();

        Bill savedBill = billRepository.save(bill);
        log.info("COD Bill created successfully. Bill ID: {}, Order ID: {}", savedBill.getId(), orderId);

        return savedBill;
    }

    /**
     * Kiểm tra PaymentType có phải Online Payment không
     */
    private boolean isOnlinePayment(PaymentType type) {
        return type == PaymentType.NCB || type == PaymentType.MOMO;
    }

    /**
     * Kiểm tra PaymentType có phải COD hoặc BNPL không
     */
    private boolean isCODOrBNPL(PaymentType type) {
        return type == PaymentType.PAYMENT_UPON_DELIVERY || type == PaymentType.BUY_NOW_PAY_LATER;
    }

    /**
     * Convert String ID (32 chars) sang UUID
     */
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
