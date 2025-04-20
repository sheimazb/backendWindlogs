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
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
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

    @MessageMapping("/notification.markAllAsRead")
    @SendToUser("/topic/notifications")
    public void markAllAsRead(@Payload Map<String, String> payload) {
        String email = payload.get("email");
        log.info("Received WebSocket request to mark all notifications as read for user: {}", email);

        // Actually mark notifications as read using the service
        List<Notification> updatedNotifications = notificationService.markAllNotificationsAsRead(email);

        // Convert to DTOs and send back through WebSocket
        List<NotificationDTO> notificationDTOs = updatedNotifications.stream()
                .map(notificationMapper::toDTO)
                .collect(Collectors.toList());

        log.info("Marked {} notifications as read for user: {}", notificationDTOs.size(), email);
    }
    /**
     * WebSocket endpoint for receiving new notifications
     */
    @MessageMapping("/notification.send")
    @SendTo("/topic/notifications")
    public NotificationDTO sendNotification(@Payload NotificationDTO notificationDTO) {
        log.info("Received WebSocket notification for recipient: {}", notificationDTO.getRecipientEmail());

        Notification notification = notificationMapper.toEntity(notificationDTO);
        Notification createdNotification = notificationService.createNotification(notification);
        return notificationMapper.toDTO(createdNotification);
    }

    /**
     * WebSocket endpoint for user-specific notifications
     */
    @MessageMapping("/notification.private")
    @SendToUser("/topic/notifications")
    public NotificationDTO sendPrivateNotification(
            @Payload NotificationDTO notificationDTO,
            Principal principal
    ) {
        log.info("Received private WebSocket notification for recipient: {}", notificationDTO.getRecipientEmail());

        Notification notification = notificationMapper.toEntity(notificationDTO);
        Notification createdNotification = notificationService.createNotification(notification);
        return notificationMapper.toDTO(createdNotification);
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

} 