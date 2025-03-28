package com.windlogs.notification.kafka;

import com.windlogs.notification.notification.Notification;
import com.windlogs.notification.service.NotificationService;
import com.windlogs.notification.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consumer for log events from Kafka
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogEventConsumer {

    private final NotificationService notificationService;
    private final UserService userService;

    /**
     * Listen for log events from Kafka
     * @param ticketsLogEvent The log event
     */
    @KafkaListener(
        topics = "log-events-topic", 
        groupId = "notification-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeLogEvent(TicketsLogEvent ticketsLogEvent) {
        log.info("Received raw log event data: level={}, message={}, container={}", 
                ticketsLogEvent.getLevel(), ticketsLogEvent.getMessage(), ticketsLogEvent.getContainer_name());
        
        try {
            // Convert to our internal LogEvent format
            LogEvent logEvent = ticketsLogEvent.toLogEvent();
            
            log.info("Converted to LogEvent: logId={}, type={}, severity={}, tenant={}", 
                    logEvent.logId(), logEvent.type(), logEvent.severity(), logEvent.tenant());
            
            // Get managers with the same tenant
            log.info("Getting managers for tenant: {}", logEvent.tenant());
            List<String> managerEmails = userService.getUserEmailsByTenant(logEvent.tenant());
            
            if (managerEmails.isEmpty()) {
                log.warn("No managers found for tenant: {}", logEvent.tenant());
                // Create a notification for a test manager to ensure the notification system works
                createNotificationForManager(logEvent, "test-manager@example.com");
                return;
            }
            
            // Create a notification for each manager
            log.info("Creating notifications for {} managers", managerEmails.size());
            for (String managerEmail : managerEmails) {
                createNotificationForManager(logEvent, managerEmail);
            }
            
            log.info("Created notifications for {} managers for log ID: {}", managerEmails.size(), logEvent.logId());
        } catch (Exception e) {
            log.error("Error processing log event: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Create a notification for a manager
     * @param logEvent The log event
     * @param managerEmail The manager's email
     */
    private void createNotificationForManager(LogEvent logEvent, String managerEmail) {
        try {
            log.info("Creating notification for manager: {} for log ID: {}", managerEmail, logEvent.logId());
            
            String subject = "New Log: " + logEvent.type();
            String message = String.format("A new log has been created with severity %s: %s", 
                    logEvent.severity(), logEvent.description());
            
            log.info("Notification subject: {}", subject);
            log.info("Notification message: {}", message);
            
            Notification notification = Notification.builder()
                    .subject(subject)
                    .message(message)
                    .sourceType("LOG")
                    .sourceId(logEvent.logId())
                    .tenant(logEvent.tenant())
                    .recipientEmail(managerEmail)
                    .read(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            Notification savedNotification = notificationService.createNotification(notification);
            log.info("Notification created with ID: {}", savedNotification.getId());
        } catch (Exception e) {
            log.error("Error creating notification for manager {}: {}", managerEmail, e.getMessage(), e);
        }
    }
} 