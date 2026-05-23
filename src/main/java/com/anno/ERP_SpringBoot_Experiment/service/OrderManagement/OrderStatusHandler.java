package com.anno.ERP_SpringBoot_Experiment.service.OrderManagement;

import org.springframework.stereotype.Component;
import com.anno.ERP_SpringBoot_Experiment.model.entity.Order;
import com.anno.ERP_SpringBoot_Experiment.model.enums.OrderStatus;
import com.anno.ERP_SpringBoot_Experiment.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.ErrorCode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OrderStatusHandler {

    private final OrderRepository orderRepository;

    public void process(Order order) {}

    public void transitionTo(Order order, OrderStatus status, String note) {
        order.getStatus().add(status);
    }

    public void transitionFromDashboard(String orderId, OrderStatus targetStatus, String note) {
        Order order = orderRepository.findById(Long.valueOf(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));
        transitionTo(order, targetStatus, note);
        orderRepository.save(order);
    }

    public String transitionToShipped(String orderId, String shipperId, String note) {
        Order order = orderRepository.findById(Long.valueOf(orderId))
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND, "Không tìm thấy đơn hàng"));
        transitionTo(order, OrderStatus.SHIPPED, note);
        orderRepository.save(order);
        return UUID.randomUUID().toString();
    }

    public Map<String, Object> getDeliveryPin(String orderNumber) {
        Map<String, Object> pinInfo = new LinkedHashMap<>();
        pinInfo.put("orderNumber", orderNumber);
        pinInfo.put("pin", "123456");
        return pinInfo;
    }

    public void clearDeliveryPin(String orderNumber) {
        // Clear delivery pin logic
    }
}
