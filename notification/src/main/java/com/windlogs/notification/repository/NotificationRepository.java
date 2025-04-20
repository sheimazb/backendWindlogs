package com.windlogs.notification.repository;

import com.windlogs.notification.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    
    /**
     * Find notifications by tenant
     * @param tenant The tenant
     * @return List of notifications for the tenant
     */
    List<Notification> findByTenant(String tenant);
    
    /**
     * Find notifications by recipient email
     * @param recipientEmail The recipient email
     * @return List of notifications for the recipient
     */
    List<Notification> findByRecipientEmail(String recipientEmail);
    

    List<Notification> findByRecipientEmailAndRead(String recipientEmail, boolean isRead);
    
    /**
     * Find unread notifications by tenant and recipient email
     * @param tenant The tenant
     * @param recipientEmail The recipient email
     * @param read Whether the notification has been read
     * @return List of unread notifications for the tenant and recipient
     */
    List<Notification> findByTenantAndRecipientEmailAndRead(String tenant, String recipientEmail, boolean read);
} 