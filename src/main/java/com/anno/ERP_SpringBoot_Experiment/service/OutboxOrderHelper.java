package com.anno.ERP_SpringBoot_Experiment.service;

import com.anno.ERP_SpringBoot_Experiment.model.entity.OutboxEvent;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Order;
import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;
import com.anno.ERP_SpringBoot_Experiment.service.dto.request.CreateOrderRequest;
import com.anno.ERP_SpringBoot_Experiment.repository.OutboxEventRepository;
import com.anno.ERP_SpringBoot_Experiment.service.dto.kafkaDtos.CustomerInfo;
import com.anno.ERP_SpringBoot_Experiment.service.dto.kafkaDtos.OrderEventDto;
import com.anno.ERP_SpringBoot_Experiment.service.dto.kafkaDtos.PaymentOptions;
import com.anno.ERP_SpringBoot_Experiment.common.constants.KafkaTopics;
import com.anno.ERP_SpringBoot_Experiment.util.SecurityUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Helper để ghi order events vào Outbox table thay vì gửi trực tiếp đến Kafka.
 * Đảm bảo event được lưu cùng transaction với order data.
 * 
 * @en Helper for writing order events to outbox table
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxOrderHelper {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final SecurityUtil securityUtil;

    /**
     * Ghi ORDER_CREATED event vào outbox.
     * Được gọi trong cùng transaction với OrderService.createOrder().
     * 
     * @en Write ORDER_CREATED event to outbox
     * @param order Order đã được tạo
     * @param paymentMethod Phương thức thanh toán
     * @param bankCode Mã ngân hàng (nếu có)
     */
    public void saveOrderCreatedEvent(Order order, String paymentMethod, String bankCode) {
        try {
            String correlationId = UUID.randomUUID().toString();
            
            OrderEventDto eventDto = OrderEventDto.builder()
                    .paymentProvider(order.getShippingMethod())
                    .amount(order.getTotalAmount())
                    .currency("VND")
                    .orderId(order.getOrderNumber())
                    .orderDescription("Thanh toan don hang " + order.getOrderNumber())
                    .customerInfo(CustomerInfo.builder()
                            .appUserId(order.getCustomer() != null ? String.valueOf(order.getCustomer().getId()) : null)
                            .ipAddress(securityUtil.getIpAddress())
                            .language("vn")
                            .build())
                    .paymentOptions(PaymentOptions.builder()
                            .paymentMethod(paymentMethod != null ? paymentMethod.toUpperCase() : "COD")
                            .bankCode(bankCode)
                            .extraData("Don hang: " + order.getOrderNumber())
                            .build())
                    .build();

            String payload = objectMapper.writeValueAsString(eventDto);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("ORDER")
                    .aggregateId(order.getId())
                    .eventType("ORDER_CREATED")
                    .topic(KafkaTopics.ORDER_TOPIC)
                    .messageKey(order.getOrderNumber())
                    .payload(payload)
                    .correlationId(correlationId)
                    .status("PENDING")
                    .build();

            outboxEventRepository.save(outboxEvent);
            
            log.info("Saved ORDER_CREATED event to outbox: orderId={}, correlationId={}", 
                    order.getOrderNumber(), correlationId);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize OrderEventDto for order: {}", order.getOrderNumber(), e);
            throw new RuntimeException("Failed to create order event", e);
        }
    }

    /**
     * Ghi ORDER_CREATED event vào outbox (overload với CreateOrderRequest).
     *
     * @en Write ORDER_CREATED event to outbox (overload with CreateOrderRequest)
     * @param order Order đã được tạo
     * @param request CreateOrderRequest chứa thông tin thanh toán
     */
    public void saveOrderCreatedEvent(Order order, CreateOrderRequest request) {
        String paymentMethod = request.getPaymentMethod() != null ? request.getPaymentMethod().toString().toUpperCase() : "COD";
        String bankCode = request.getBankCode();
        saveOrderCreatedEvent(order, paymentMethod, bankCode);
    }

    /**
     * Ghi ORDER_STATUS_CHANGED event vào outbox.
     * 
     * @en Write ORDER_STATUS_CHANGED event to outbox
     * @param order Order có trạng thái mới
     * @param previousStatus Trạng thái trước đó
     * @param newStatus Trạng thái mới
     * @param note Ghi chú thay đổi
     */
    public void saveOrderStatusChangedEvent(Order order, OrderStatus previousStatus, 
                                            OrderStatus newStatus, String note) {
        try {
            String correlationId = UUID.randomUUID().toString();
            
            var eventDto = java.util.Map.of(
                    "orderId", order.getOrderNumber(),
                    "previousStatus", previousStatus != null ? previousStatus.name() : "NONE",
                    "newStatus", newStatus.name(),
                    "note", note != null ? note : "",
                    "changedAt", java.time.LocalDateTime.now().toString(),
                    "changedBy", securityUtil.getCurrentUsername() != null ? 
                            securityUtil.getCurrentUsername() : "SYSTEM"
            );

            String payload = objectMapper.writeValueAsString(eventDto);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("ORDER")
                    .aggregateId(order.getId())
                    .eventType("ORDER_STATUS_CHANGED")
                    .topic(KafkaTopics.ORDER_TOPIC)
                    .messageKey(order.getOrderNumber())
                    .payload(payload)
                    .correlationId(correlationId)
                    .status("PENDING")
                    .build();

            outboxEventRepository.save(outboxEvent);
            
            log.info("Saved ORDER_STATUS_CHANGED event to outbox: orderId={}, status={}", 
                    order.getOrderNumber(), newStatus);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize status change event for order: {}", order.getOrderNumber(), e);
            // Không throw để không block status transition
        }
    }

    /**
     * Ghi ORDER_CANCELLED event vào outbox.
     * 
     * @en Write ORDER_CANCELLED event to outbox
     * @param order Order đã bị hủy
     * @param reason Lý do hủy
     */
    public void saveOrderCancelledEvent(Order order, String reason) {
        try {
            String correlationId = UUID.randomUUID().toString();
            
            var eventDto = java.util.Map.of(
                    "orderId", order.getOrderNumber(),
                    "cancelledAt", java.time.LocalDateTime.now().toString(),
                    "cancelledBy", securityUtil.getCurrentUsername() != null ? 
                            securityUtil.getCurrentUsername() : "SYSTEM",
                    "reason", reason != null ? reason : "No reason provided",
                    "refundRequired", true
            );

            String payload = objectMapper.writeValueAsString(eventDto);

            OutboxEvent outboxEvent = OutboxEvent.builder()
                    .aggregateType("ORDER")
                    .aggregateId(order.getId())
                    .eventType("ORDER_CANCELLED")
                    .topic(KafkaTopics.ORDER_TOPIC)
                    .messageKey(order.getOrderNumber())
                    .payload(payload)
                    .correlationId(correlationId)
                    .status("PENDING")
                    .build();

            outboxEventRepository.save(outboxEvent);
            
            log.info("Saved ORDER_CANCELLED event to outbox: orderId={}", order.getOrderNumber());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize cancelled event for order: {}", order.getOrderNumber(), e);
        }
    }

    /**
     * Check xem event đã tồn tại chưa (idempotency).
     * 
     * @en Check if event already exists (idempotency)
     */
    public boolean eventExists(Long orderId, String eventType) {
        return outboxEventRepository.existsByAggregateTypeAndAggregateIdAndEventType(
                "ORDER", orderId, eventType);
    }
}
