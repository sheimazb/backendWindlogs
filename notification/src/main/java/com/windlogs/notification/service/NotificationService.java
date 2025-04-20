package com.windlogs.notification.service;

import com.windlogs.notification.notification.Notification;
import com.windlogs.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final WebsocketService websocketService;

    /**
     * Create a new notification
     * @param notification The notification to create
     * @return The created notification
     */
    public Notification createNotification(Notification notification) {
        log.info("Creating notification for tenant: {}, recipient: {}",
                notification.getTenant(), notification.getRecipientEmail());

        try {
            log.info("Notification details - Subject: {}, Message: {}, SourceType: {}, SourceId: {}",
                    notification.getSubject(), notification.getMessage(),
                    notification.getSourceType(), notification.getSourceId());

            Notification savedNotification = notificationRepository.save(notification);
            log.info("Notification saved successfully with ID: {}", savedNotification.getId());

            // Send the saved notification with ID via WebSocket
            log.info("Notification sent via WebSocket to recipient: {}", notification.getRecipientEmail());
            websocketService.sendNotification(
                notification.getRecipientEmail(),
                savedNotification
            );
            return savedNotification;
        } catch (Exception e) {
            log.error("Error saving notification: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Get notifications by tenant
     * @param tenant The tenant
     * @return List of notifications for the tenant
     */
    public List<Notification> getNotificationsByTenant(String tenant) {
        log.info("Getting notifications for tenant: {}", tenant);
        List<Notification> notifications = notificationRepository.findByTenant(tenant);
        log.info("Found {} notifications for tenant: {}", notifications.size(), tenant);
        return notifications;
    }

    /**
     * Get notifications by recipient email
     * @param recipientEmail The recipient email
     * @return List of notifications for the recipient
     */
    public List<Notification> getNotificationsByRecipientEmail(String recipientEmail) {
        log.info("Getting notifications for recipient: {}", recipientEmail);
        List<Notification> notifications = notificationRepository.findByRecipientEmail(recipientEmail);
        log.info("Found {} notifications for recipient: {}", notifications.size(), recipientEmail);
        return notifications;
    }



    /**
     * Get unread notifications by tenant and recipient email
     * @param tenant The tenant
     * @param recipientEmail The recipient email
     * @return List of unread notifications for the tenant and recipient
     */
    public List<Notification> getUnreadNotificationsByTenantAndRecipientEmail(String tenant, String recipientEmail) {
        log.info("Getting unread notifications for tenant: {}, recipient: {}", tenant, recipientEmail);
        List<Notification> notifications = notificationRepository.findByTenantAndRecipientEmailAndRead(tenant, recipientEmail, false);
        log.info("Found {} unread notifications for tenant: {} and recipient: {}",
                notifications.size(), tenant, recipientEmail);
        return notifications;
    }
    /**
     * Mark all notifications as read for a specific recipient
     * @param recipientEmail The recipient email
     * @return List of updated notifications
     */
    public List<Notification> markAllNotificationsAsRead(String recipientEmail) {
        log.info("Marking all notifications as read for recipient: {}", recipientEmail);
        try {
            List<Notification> unreadNotifications = notificationRepository
                    .findByRecipientEmailAndRead(recipientEmail, false);

            unreadNotifications.forEach(notification -> notification.setRead(true));
            List<Notification> updatedNotifications = notificationRepository.saveAll(unreadNotifications);

            log.info("Marked {} notifications as read for recipient: {}",
                    updatedNotifications.size(), recipientEmail);
            return updatedNotifications;
        } catch (Exception e) {
            log.error("Error marking all notifications as read: {}", e.getMessage(), e);
            throw e;
        }
    }
    /**
     * Mark a notification as read
     * @param id The notification ID
     * @return The updated notification
     */
    public Notification markNotificationAsRead(Long id) {
        log.info("Marking notification as read: {}", id);
        try {
            Notification notification = notificationRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Notification not found"));
            notification.setRead(true);
            Notification updatedNotification = notificationRepository.save(notification);
            log.info("Notification marked as read: {}", id);
            return updatedNotification;
        } catch (Exception e) {
            log.error("Error marking notification as read: {}", e.getMessage(), e);
            throw e;
        }
    }
}