package com.anno.ERP_SpringBoot_Experiment.service;

import com.anno.ERP_SpringBoot_Experiment.event.producer.VideoProducer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    private final VideoProducer videoProducer;

    @Scheduled(fixedRate = 5000)
    public void sendNotification() {
        videoProducer.syncVideos("SlowV Hello AE nhé!!!!");
    }
}
