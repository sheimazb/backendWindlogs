package com.windlogs.notification.kafka;

import com.windlogs.notification.notification.Notification;
import com.windlogs.notification.service.NotificationService;
import com.windlogs.notification.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Consumer for log events from Fluentd via Kafka
 * Simplified to focus on the core notification flow
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogEventConsumer {

    private final NotificationService notificationService;
    private final UserService userService;

    @Autowired
    SimpMessagingTemplate template;
    /**
     * Listen for log events from Kafka
     * @param ticketsLogEvent The log event from Fluentd
     */
    @KafkaListener(
        topics = "log-events-topic", 
        groupId = "notification-group",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeLogEvent(TicketsLogEvent ticketsLogEvent) {
        log.info("==================== START LOG EVENT PROCESSING ====================");
        log.info("Received log data from Fluentd via Kafka: level={}, message={}, container_id={}, container_name={}", 
                ticketsLogEvent.getLevel(), ticketsLogEvent.getMessage(), 
                ticketsLogEvent.getContainer_id(), ticketsLogEvent.getContainer_name());
        
        try {
            // Convert to our internal LogEvent format
            LogEvent logEvent = ticketsLogEvent.toLogEvent();
            
            log.info("Converted to LogEvent: logId={}, type={}, severity={}, projectId={}, tenant={}, class_name={}, recipientEmail={}", 
                    logEvent.logId(), logEvent.type(), logEvent.severity(), 
                    logEvent.projectId(), logEvent.tenant(), logEvent.class_name(), logEvent.recipientEmail());
            
            // Use the recipient email directly from the LogEvent
            String recipientEmail = logEvent.recipientEmail();
            
            if (recipientEmail == null || recipientEmail.isEmpty()) {
                log.warn("No recipient email in LogEvent, will try to find a suitable recipient");
                
                // Approach 1: If projectId is available, try to get the project manager's email
                if (logEvent.projectId() != null && logEvent.projectId() > 0) {
                    log.info("Log is associated with project ID: {}", logEvent.projectId());
                    
                    Optional<String> managerEmail = userService.getProjectManagerEmail(logEvent.projectId());
                    
                    if (managerEmail.isPresent()) {
                        recipientEmail = managerEmail.get();
                        log.info("Found project manager email: {}", recipientEmail);
                    } else {
                        log.warn("Could not find project manager for project ID: {}", logEvent.projectId());
                    }
                }
                
                // Approach 2: If no project manager, try tenant managers
                if (recipientEmail == null && logEvent.tenant() != null && !logEvent.tenant().isEmpty() && !"default".equals(logEvent.tenant())) {
                    log.info("Getting managers for tenant: '{}'", logEvent.tenant());
                    List<String> managerEmails = userService.getUserEmailsByTenant(logEvent.tenant());
                    
                    if (!managerEmails.isEmpty()) {
                        recipientEmail = managerEmails.get(0);
                        log.info("Using tenant manager as recipient: {}", recipientEmail);
                    } else {
                        log.warn("No managers found for tenant: '{}'", logEvent.tenant());
                    }
                }
                
                // Approach 3: Fallback to default admin email if no recipient found
                if (recipientEmail == null || recipientEmail.isEmpty()) {
                    recipientEmail = "admin@windlogs.com";
                    log.warn("No suitable recipient found, using default admin email: {}", recipientEmail);
                }
            } else {
                log.info("Using recipient email from LogEvent: {}", recipientEmail);
            }
            
            // Always create the notification
            createNotificationForManager(logEvent, recipientEmail);
            log.info("==================== END LOG EVENT PROCESSING ====================");
            
        } catch (Exception e) {
            log.error("Error processing log event: {}", e.getMessage(), e);
            // Print stack trace for debugging
            e.printStackTrace();
            log.info("==================== END LOG EVENT PROCESSING WITH ERROR ====================");
        }
    }
    
    /**
     * Create a notification for a manager about a log
     * @param logEvent The log event
     * @param managerEmail The manager's email
     */
    private void createNotificationForManager(LogEvent logEvent, String managerEmail) {
        try {
            // Create a very simple notification
            String subject = "Alert: Error in " + logEvent.class_name();
            String message = "Error detected in " + logEvent.class_name();
            
            log.info("Creating notification for recipient: {}", managerEmail);
            
            // Create notification with short strings to avoid exceeding the varchar(255) limit
            Notification notification = Notification.builder()
                    .subject(subject.length() > 200 ? subject.substring(0, 200) : subject)
                    .message(message.length() > 200 ? message.substring(0, 200) : message)
                    .sourceType("LOG")
                    .sourceId(logEvent.logId())
                    .tenant(logEvent.tenant())
                    .recipientEmail(managerEmail)
                    .read(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            // Save the notification
            notificationService.createNotification(notification);
            log.info("Notification created for log ID: {}, tenant: {}, recipient: {}", 
                    logEvent.logId(), logEvent.tenant(), managerEmail);
        } catch (Exception e) {
            log.error("Error creating notification: {}", e.getMessage(), e);
        }
    }
} 