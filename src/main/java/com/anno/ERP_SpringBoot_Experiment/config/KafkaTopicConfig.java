package com.anno.ERP_SpringBoot_Experiment.config;

import com.anno.ERP_SpringBoot_Experiment.common.constants.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic activeLogTopic() {
        return new NewTopic(KafkaTopics.ACTIVE_LOG_TOPIC, 2, (short) 1);
    }
}
