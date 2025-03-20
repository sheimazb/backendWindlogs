package com.windlogs.notification.mapper;

import com.windlogs.notification.dto.NotificationDTO;
import com.windlogs.notification.notification.Notification;
import org.springframework.stereotype.Component;

@Component
public class NotificationMapper {
    
    /**
     * Convert a Notification entity to a NotificationDTO
     * @param notification The notification entity
     * @return The notification DTO
     */
    public NotificationDTO toDTO(Notification notification) {
        if (notification == null) {
            return null;
        }
        
        return NotificationDTO.builder()
                .id(notification.getId())
                .message(notification.getMessage())
                .subject(notification.getSubject())
                .sourceType(notification.getSourceType())
                .sourceId(notification.getSourceId())
                .tenant(notification.getTenant())
                .recipientEmail(notification.getRecipientEmail())
                .read(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
    
    /**
     * Convert a NotificationDTO to a Notification entity
     * @param notificationDTO The notification DTO
     * @return The notification entity
     */
    public Notification toEntity(NotificationDTO notificationDTO) {
        if (notificationDTO == null) {
            return null;
        }
        
        return Notification.builder()
                .id(notificationDTO.getId())
                .message(notificationDTO.getMessage())
                .subject(notificationDTO.getSubject())
                .sourceType(notificationDTO.getSourceType())
                .sourceId(notificationDTO.getSourceId())
                .tenant(notificationDTO.getTenant())
                .recipientEmail(notificationDTO.getRecipientEmail())
                .read(notificationDTO.isRead())
                .createdAt(notificationDTO.getCreatedAt())
                .build();
    }
} 