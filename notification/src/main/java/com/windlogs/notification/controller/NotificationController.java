package com.windlogs.notification.controller;

import com.windlogs.notification.dto.NotificationDTO;
import com.windlogs.notification.mapper.NotificationMapper;
import com.windlogs.notification.notification.Notification;
import com.windlogs.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {
    
    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;
    
    /**
     * Get notifications for the current user
     * @param email The user's email
     * @return List of notifications for the user
     */
    @GetMapping
    public ResponseEntity<List<NotificationDTO>> getNotifications(@RequestParam String email) {
        log.info("Getting notifications for user: {}", email);
        
        List<Notification> notifications = notificationService.getNotificationsByRecipientEmail(email);
        List<NotificationDTO> notificationDTOs = notifications.stream()
                .map(notificationMapper::toDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(notificationDTOs);
    }
    
    /**
     * Mark all notifications as read for a user
     * @param payload Map containing the user's email
     */
    @MessageMapping("/notification.markAllAsRead")
    public void markAllAsRead(@Payload Map<String, String> payload) {
        String email = payload.get("email");
        log.info("Received WebSocket request to mark all notifications as read for user: {}", email);
        
        log.info("Marked all notifications as read for user: {}", email);
    }
    
    /**
     * Get unread notifications for the current user
     * @param email The user's email
     * @param tenant The user's tenant
     * @return List of unread notifications for the user
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications(
            @RequestParam String email,
            @RequestParam String tenant) {
        log.info("Getting unread notifications for user: {}, tenant: {}", email, tenant);
        
        List<Notification> notifications = notificationService.getUnreadNotificationsByTenantAndRecipientEmail(tenant, email);
        List<NotificationDTO> notificationDTOs = notifications.stream()
                .map(notificationMapper::toDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(notificationDTOs);
    }
    
    /**
     * Mark a notification as read
     * @param id The notification ID
     * @return The updated notification
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationDTO> markNotificationAsRead(@PathVariable Long id) {
        log.info("Marking notification as read: {}", id);
        
        Notification notification = notificationService.markNotificationAsRead(id);
        NotificationDTO notificationDTO = notificationMapper.toDTO(notification);
        
        return ResponseEntity.ok(notificationDTO);
    }
    
    /**
     * Create a new notification
     * @param notificationDTO The notification to create
     * @return The created notification
     */
    @PostMapping
    public ResponseEntity<NotificationDTO> createNotification(@RequestBody NotificationDTO notificationDTO) {
        log.info("Creating notification for recipient: {}", notificationDTO.getRecipientEmail());
        
        Notification notification = notificationMapper.toEntity(notificationDTO);
        Notification createdNotification = notificationService.createNotification(notification);
        NotificationDTO createdDTO = notificationMapper.toDTO(createdNotification);
        
        return ResponseEntity.ok(createdDTO);
    }
    
    /**
     * Count unread notifications for a user
     * @param email The user's email
     * @param tenant The user's tenant
     * @return The count of unread notifications
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Long> countUnreadNotifications(
            @RequestParam String email,
            @RequestParam String tenant) {
        log.info("Counting unread notifications for user: {}, tenant: {}", email, tenant);
        
        List<Notification> notifications = notificationService.getUnreadNotificationsByTenantAndRecipientEmail(tenant, email);
        long count = notifications.size();
        
        return ResponseEntity.ok(count);
    }
    
    /**
     * Test endpoint to send a notification to a user
     * @param email The recipient's email
     * @param tenant The tenant
     * @return The created notification
     */
    @PostMapping("/test")
    public ResponseEntity<NotificationDTO> sendTestNotification(
            @RequestParam String email,
            @RequestParam String tenant) {
        log.info("Sending test notification to user: {}, tenant: {}", email, tenant);
        
        // Create a test notification
        Notification notification = Notification.builder()
                .subject("Test Notification")
                .message("This is a test notification sent at " + LocalDateTime.now())
                .sourceType("TEST")
                .sourceId(1L)
                .tenant(tenant)
                .recipientEmail(email)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build();
        
        // Save and send the notification
        Notification createdNotification = notificationService.createNotification(notification);
        NotificationDTO notificationDTO = notificationMapper.toDTO(createdNotification);
        
        return ResponseEntity.ok(notificationDTO);
    }
} 