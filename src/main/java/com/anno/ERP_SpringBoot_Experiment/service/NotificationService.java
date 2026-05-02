
package com.anno.ERP_SpringBoot_Experiment.service;

import com.anno.ERP_SpringBoot_Experiment.model.entity.User;
import com.anno.ERP_SpringBoot_Experiment.repository.UserRepository;
import com.anno.ERP_SpringBoot_Experiment.service.dto.NotificationDto;
import com.anno.ERP_SpringBoot_Experiment.service.interfaces.iNotification;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.BusinessException;
import com.anno.ERP_SpringBoot_Experiment.web.rest.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService implements iNotification {

        private final SimpMessagingTemplate simpMessagingTemplate;
        private final UserRepository userRepository;

        @Override
        public void broadcastToAllEmployees(NotificationDto notification, String senderUsername) {
                final var sender = userRepository.findByName(senderUsername)
                                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                                                "Không tìm thấy người gửi"));

                notification.setId(UUID.randomUUID().toString());
                notification.setSenderId(String.valueOf(sender.getId()));
                notification.setSenderName(sender.getEmail());
                notification.setTimestamp(LocalDateTime.now());
                notification.setTargetAudience("ALL_EMPLOYEES");

                simpMessagingTemplate.convertAndSend("/topic/notifications", notification);

                log.info("Đã broadcast thông báo '{}' từ {} đến tất cả nhân viên", notification.getTitle(),
                                senderUsername);

                // kafka để lưu vào Active Log
        }

        @Override
        public void sendToGroup(NotificationDto notification, String senderUsername) {
                final var sender = userRepository.findByName(senderUsername)
                                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                                                "Không tìm thấy người gửi"));

                notification.setId(UUID.randomUUID().toString());
                notification.setSenderId(String.valueOf(sender.getId()));
                notification.setSenderName(sender.getName());
                notification.setTimestamp(LocalDateTime.now());

                // Gửi đến kênh nhóm cụ thể
                String groupChannel = "/topic/notifications/group/" + notification.getTargetAudience();
                simpMessagingTemplate.convertAndSend(groupChannel, notification);

                log.info("Đã gửi thông báo '{}' đến nhóm {}",
                                notification.getTitle(), notification.getTargetAudience());
        }

        @Override
        public void sendToSpecificUser(NotificationDto notification, String targetUserId) {
                notification.setId(UUID.randomUUID().toString());
                notification.setTimestamp(LocalDateTime.now());

                // Gửi đến user cụ thể qua kênh /topic/notifications/user/{userId}
                String userChannel = "/topic/notifications/user/" + targetUserId;
                simpMessagingTemplate.convertAndSend(userChannel, notification);

                log.info("Đã gửi thông báo '{}' đến user {}",
                                notification.getTitle(), targetUserId);
        }

        // ============= BỔ SUNG CÁC METHOD MỚI =============

        @Override
        public void sendToMultipleUsers(NotificationDto notification, List<String> targetUserIds,
                        String senderUsername) {
                User sender = userRepository.findByName(senderUsername)
                                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND,
                                                "Không tìm thấy người gửi"));

                notification.setSenderId(String.valueOf(sender.getId()));
                notification.setSenderName(sender.getName());
                notification.setTargetAudience("MULTIPLE_USERS");

                // Gửi đến từng user
                targetUserIds.forEach(userId -> {
                        NotificationDto userNotification = NotificationDto.builder()
                                        .id(UUID.randomUUID().toString())
                                        .senderId(notification.getSenderId())
                                        .senderName(notification.getSenderName())
                                        .title(notification.getTitle())
                                        .message(notification.getMessage())
                                        .type(notification.getType())
                                        .link(notification.getLink())
                                        .priority(notification.getPriority())
                                        .icon(notification.getIcon())
                                        .metadata(notification.getMetadata())
                                        .targetAudience(notification.getTargetAudience())
                                        .timestamp(LocalDateTime.now())
                                        .isRead(false)
                                        .build();

                        String userChannel = "/topic/notifications/user/" + userId;
                        simpMessagingTemplate.convertAndSend(userChannel, userNotification);
                });

                log.info("Đã gửi thông báo '{}' đến {} users",
                                notification.getTitle(), targetUserIds.size());
        }

        @Override
        public void sendUrgentNotification(NotificationDto notification, String targetUserId) {
                notification.setId(UUID.randomUUID().toString());
                notification.setTimestamp(LocalDateTime.now());
                notification.setPriority("URGENT");
                notification.setType("ERROR");

                // Gửi đến kênh urgent riêng
                String urgentChannel = "/topic/notifications/urgent/" + targetUserId;
                simpMessagingTemplate.convertAndSend(urgentChannel, notification);

                log.warn("🚨 Đã gửi thông báo KHẨN CẤP '{}' đến user {}",
                                notification.getTitle(), targetUserId);
        }

        /**
         * Gửi thông báo hệ thống
         */
        public void sendSystemNotification(String title, String message, String targetUserId) {
                NotificationDto notification = NotificationDto.builder()
                                .id(UUID.randomUUID().toString())
                                .title(title)
                                .message(message)
                                .type("SYSTEM")
                                .priority("HIGH")
                                .icon("system")
                                .timestamp(LocalDateTime.now())
                                .isRead(false)
                                .build();

                String userChannel = "/topic/notifications/user/" + targetUserId;
                simpMessagingTemplate.convertAndSend(userChannel, notification);

                log.info("⚙️ Đã gửi thông báo hệ thống '{}' đến user {}", title, targetUserId);
        }

        /**
         * Gửi thông báo thành công
         */
        public void sendSuccessNotification(String title, String message, String targetUserId) {
                NotificationDto notification = NotificationDto.builder()
                                .id(UUID.randomUUID().toString())
                                .title(title)
                                .message(message)
                                .type("SUCCESS")
                                .priority("MEDIUM")
                                .icon("check-circle")
                                .timestamp(LocalDateTime.now())
                                .isRead(false)
                                .build();

                String userChannel = "/topic/notifications/user/" + targetUserId;
                simpMessagingTemplate.convertAndSend(userChannel, notification);

                log.info("✅ Đã gửi thông báo thành công '{}' đến user {}", title, targetUserId);
        }

        /**
         * Gửi thông báo cảnh báo
         */
        public void sendWarningNotification(String title, String message, String targetUserId) {
                NotificationDto notification = NotificationDto.builder()
                                .id(UUID.randomUUID().toString())
                                .title(title)
                                .message(message)
                                .type("WARNING")
                                .priority("HIGH")
                                .icon("exclamation-triangle")
                                .timestamp(LocalDateTime.now())
                                .isRead(false)
                                .build();

                String userChannel = "/topic/notifications/user/" + targetUserId;
                simpMessagingTemplate.convertAndSend(userChannel, notification);

                log.warn("⚠️ Đã gửi thông báo cảnh báo '{}' đến user {}", title, targetUserId);
        }
}
