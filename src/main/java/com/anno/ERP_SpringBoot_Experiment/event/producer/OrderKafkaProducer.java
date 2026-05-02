package com.anno.ERP_SpringBoot_Experiment.event.producer;

import com.anno.ERP_SpringBoot_Experiment.common.constants.KafkaTopics;
import com.anno.ERP_SpringBoot_Experiment.service.dto.kafkaDtos.OrderEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderKafkaProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendOrderCreatedEvent(OrderEventDto orderEventDto) {
        kafkaTemplate.send(KafkaTopics.ORDER_CREATED_TOPIC, orderEventDto.getOrderId(), orderEventDto);
    }
}
