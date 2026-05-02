package com.anno.ERP_SpringBoot_Experiment.service.BillService;

import com.anno.ERP_SpringBoot_Experiment.model.entity.Bill;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Order;
import com.anno.ERP_SpringBoot_Experiment.repository.BillRepository;
import com.anno.ERP_SpringBoot_Experiment.repository.OrderRepository;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateBillRequest;
import com.anno.ERP_SpringBoot_Experiment.service.dto.response.ResponseConfig.Response;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iBill;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillService implements iBill {

        private final BillRepository billRepository;
        private final OrderRepository orderRepository;

        @Override
        @Transactional
        public Response<?> addBill(CreateBillRequest request) {
                Long orderId = convertStringToLong(request.getOrderId());

                // ✅ IDEMPOTENCY CHECK - Tránh tạo trùng Bill
                if (billRepository.existsByOrder_Id(orderId)) {
                        log.warn("Bill already exists for Order ID: {}", orderId);
                        Bill existingBill = billRepository.findByOrder_Id(orderId)
                                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR,
                                                        "Lỗi hệ thống: Bill tồn tại nhưng không tìm thấy"));
                        return Response.ok(existingBill);
                }

                var order = orderRepository.findById(orderId)
                                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND,
                                                "Order không tồn tại với ID: " + orderId));

                Bill bill = Bill.builder()
                                .invoiceDate(LocalDateTime.now())
                                .customerName(order.getCustomerName())
                                .customerPhone(order.getCustomerPhone())
                                .address(request.getAddress() != null ? request.getAddress()
                                                : order.getShippingInfo().getAddress())
                                .subtotal(order.getSubtotal())
                                .shippingFee(order.getShippingFee())
                                .grandTotal(order.getTotalAmount())
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
                Long orderUuid = convertStringToLong(orderId);

                // ✅ IDEMPOTENCY CHECK
                if (billRepository.existsByOrder_Id(orderUuid)) {
                        log.warn("Bill already exists for COD Order ID: {}", orderId);
                        return billRepository.findByOrder_Id(orderUuid)
                                        .orElseThrow(() -> new BusinessException(ErrorCode.INTERNAL_ERROR,
                                                        "Lỗi hệ thống: Bill tồn tại nhưng không tìm thấy"));
                }

                // Fetch Order
                Order order = orderRepository.findById(orderUuid)
                                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND,
                                                "Order không tồn tại với ID: " + orderId));

                Bill bill = Bill.builder()
                                .invoiceDate(LocalDateTime.now())
                                .customerName(order.getCustomerName())
                                .customerPhone(order.getCustomerPhone())
                                .address(order.getShippingInfo() != null ? order.getShippingInfo().getAddress() : "")
                                .subtotal(order.getSubtotal())
                                .shippingFee(order.getShippingFee())
                                .grandTotal(order.getTotalAmount())
                                .idAddress(order.getShippingInfo() != null ? order.getShippingInfo().getAddress() : "")
                                .order(order)
                                .build();

                Bill savedBill = billRepository.save(bill);
                log.info("COD Bill created successfully. Bill ID: {}, Order ID: {}", savedBill.getId(), orderId);

                return savedBill;
        }

        /**
         * Convert String ID (32 chars) sang Long
         */
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
