package com.anno.ERP_SpringBoot_Experiment.web.rest;

import com.anno.ERP_SpringBoot_Experiment.service.NotificationService;
import com.anno.ERP_SpringBoot_Experiment.service.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Gửi thông báo đến tất cả nhân viên
     */
    @PostMapping("/broadcast")
    public ResponseEntity<?> broadcastNotification(
            @RequestBody NotificationDto notification,
            Authentication authentication
    ) {
        String senderUsername = authentication.getName();
        notificationService.broadcastToAllEmployees(notification, senderUsername);
        return ResponseEntity.ok("Thông báo đã được gửi đến tất cả nhân viên.");
    }

    /**
     * Gửi thông báo đến một nhóm cụ thể
     */
    @PostMapping("/group")
    public ResponseEntity<?> sendToGroup(
            @RequestBody NotificationDto notification,
            Authentication authentication
    ) {
        String senderUsername = authentication.getName();
        notificationService.sendToGroup(notification, senderUsername);
        return ResponseEntity.ok("Thông báo đã được gửi đến nhóm " + notification.getTargetAudience());
    }

    /**
     * Gửi thông báo đến một user cụ thể
     */
    @PostMapping("/user/{userId}")
    public ResponseEntity<?> sendToUser(
            @PathVariable String userId,
            @RequestBody NotificationDto notification
    ) {
        notificationService.sendToSpecificUser(notification, userId);
        return ResponseEntity.ok("Thông báo đã được gửi đến user.");
    }

    /**
     * Gửi thông báo đến nhiều users
     */
    @PostMapping("/users")
    public ResponseEntity<?> sendToMultipleUsers(
            @RequestBody NotificationDto notification,
            @RequestParam List<String> userIds,
            Authentication authentication
    ) {
        String senderUsername = authentication.getName();
        notificationService.sendToMultipleUsers(notification, userIds, senderUsername);
        return ResponseEntity.ok("Thông báo đã được gửi đến " + userIds.size() + " users.");
        }

        /**
         * Gửi thông báo khẩn cấp
     */
    @PostMapping("/urgent/{userId}")
    public ResponseEntity<?> sendUrgentNotification(
            @PathVariable String userId,
            @RequestBody NotificationDto notification
    ) {
        notificationService.sendUrgentNotification(notification, userId);
        return ResponseEntity.ok("Thông báo khẩn cấp đã được gửi.");
    }

    /**
     * Gửi thông báo hệ thống
     */
    @PostMapping("/system/{userId}")
    public ResponseEntity<?> sendSystemNotification(
            @PathVariable String userId,
            @RequestParam String title,
            @RequestParam String message
    ) {
        notificationService.sendSystemNotification(title, message, userId);
        return ResponseEntity.ok("Thông báo hệ thống đã được gửi.");
    }

    /**
     * Gửi thông báo thành công
     */
    @PostMapping("/success/{userId}")
    public ResponseEntity<?> sendSuccessNotification(
            @PathVariable String userId,
            @RequestParam String title,
            @RequestParam String message
    ) {
        notificationService.sendSuccessNotification(title, message, userId);
        return ResponseEntity.ok("Thông báo thành công đã được gửi.");
    }

    /**
     * Gửi thông báo cảnh báo
     */
    @PostMapping("/warning/{userId}")
    public ResponseEntity<?> sendWarningNotification(
            @PathVariable String userId,
            @RequestParam String title,
            @RequestParam String message
    ) {
        notificationService.sendWarningNotification(title, message, userId);
        return ResponseEntity.ok("Thông báo cảnh báo đã được gửi.");
    }

    /**
     * Lấy danh sách thông báo của người dùng hiện tại
     * TODO: Implement khi có database persistence
     */
    @GetMapping
    public ResponseEntity<List<NotificationDto>> getNotifications(Authentication authentication) {
        // List<NotificationDto> notifications = notificationService.getNotificationsForUser(authentication);
        // return ResponseEntity.ok(notifications);
        return ResponseEntity.ok(List.of());
    }

    /**
     * Đánh dấu thông báo là đã đọc
     * TODO: Implement khi có database persistence
     */
    @PatchMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable String id, Authentication authentication) {
        // notificationService.markAsRead(id, authentication);
        return ResponseEntity.ok("Thông báo đã được đánh dấu là đã đọc.");
    }

    /**
     * Đánh dấu tất cả thông báo là đã đọc
     * TODO: Implement khi có database persistence
     */
    @PatchMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(Authentication authentication) {
        // notificationService.markAllAsRead(authentication);
        return ResponseEntity.ok("Tất cả thông báo đã được đánh dấu là đã đọc.");
    }

    /**
     * Xóa thông báo
     * TODO: Implement khi có database persistence
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable String id, Authentication authentication) {
        // notificationService.deleteNotification(id, authentication);
        return ResponseEntity.ok("Thông báo đã được xóa.");
    }

    /**
     * Lấy số lượng thông báo chưa đọc
     * TODO: Implement khi có database persistence
     */
    @GetMapping("/unread-count")
    public ResponseEntity<Long> getUnreadCount(Authentication authentication) {
        // Long count = notificationService.getUnreadCount(authentication);
        // return ResponseEntity.ok(count);
        return ResponseEntity.ok(0L);
    }
}
