package com.anno.ERP_SpringBoot_Experiment.service.interfaces;

import com.anno.ERP_SpringBoot_Experiment.service.dto.NotificationDto;

import java.util.List;

public interface iNotification {

    void broadcastToAllEmployees(NotificationDto notification, String senderUsername);

    void sendToGroup(NotificationDto notification, String senderUsername);

    void sendToSpecificUser(NotificationDto notification, String targetUserId);

    void sendToMultipleUsers(NotificationDto notification, List<String> targetUserIds, String senderUsername);

    void sendUrgentNotification(NotificationDto notification, String targetUserId);
}
