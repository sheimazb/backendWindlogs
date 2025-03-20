package com.windlogs.notification.controller;

import com.windlogs.notification.dto.NotificationDTO;
import com.windlogs.notification.mapper.NotificationMapper;
import com.windlogs.notification.notification.Notification;
import com.windlogs.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
} 