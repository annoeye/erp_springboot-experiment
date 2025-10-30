package com.anno.ERP_SpringBoot_Experiment.service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationDto {

    private String id;
    private String senderId;
    private String senderName;
    private String title;
    private String message;
    private String type; // INFO, SUCCESS, WARNING, ERROR, SYSTEM
    private String targetAudience; // ALL_EMPLOYEES, SPECIFIC_USER, GROUP, MULTIPLE_USERS
    private String link;
    private Boolean isRead;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
    
    private String metadata;
    private String priority; // LOW, MEDIUM, HIGH, URGENT
    private String icon;
}
