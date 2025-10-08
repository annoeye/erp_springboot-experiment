package com.anno.ERP_SpringBoot_Experiment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class StreamConfiguration {

    @Bean
    public Consumer<String> receiveVideos() {
        return content -> System.out.println("Video consumer: " + content);
    }
}
