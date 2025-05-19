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
        log.info("Received log data from Fluentd via Kafka: level={}, message={}, container_id={}, container_name={}, sourceType={}, sourceId={}", 
                ticketsLogEvent.getLevel(), ticketsLogEvent.getMessage(), 
                ticketsLogEvent.getContainer_id(), ticketsLogEvent.getContainer_name(),
                ticketsLogEvent.getSourceType(), ticketsLogEvent.getSourceId());
        
        try {
            // Convert to our internal LogEvent format
            LogEvent logEvent = ticketsLogEvent.toLogEvent();
            
            log.info("Converted to LogEvent: logId={}, type={}, severity={}, projectId={}, tenant={}, class_name={}, recipientEmail={}, sourceType={}, sourceId={}, senderEmail={}", 
                    logEvent.logId(), logEvent.type(), logEvent.severity(), 
                    logEvent.projectId(), logEvent.tenant(), logEvent.class_name(), logEvent.recipientEmail(),
                    logEvent.sourceType(), logEvent.sourceId(), logEvent.senderEmail());
            
            // Use the recipient email directly from the LogEvent
            String recipientEmail = logEvent.recipientEmail();
            
            if (recipientEmail == null || recipientEmail.isEmpty()) {
                log.warn("No recipient email in LogEvent, will try to find a suitable recipient");
                
                // If projectId is available, try to get the project manager's email
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
                
                //If no project manager, try tenant managers
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
                
                // Fallback to default admin email if no recipient found
                if (recipientEmail == null || recipientEmail.isEmpty()) {
                    recipientEmail = "admin@windlogs.com";
                    log.warn("No suitable recipient found, using default admin email: {}", recipientEmail);
                }
            } else {
                log.info("Using recipient email from LogEvent: {}", recipientEmail);
            }
            
            // Create notification based on source type
            if ("COMMENT".equals(logEvent.sourceType())) {
                createCommentNotification(logEvent, recipientEmail);
            } else if ("SOLUTION".equals(logEvent.sourceType())) {
                createSolutionNotification(logEvent, recipientEmail);
            } else {
                // Default to log notification for other types or when sourceType is null
                createLogNotification(logEvent, recipientEmail);
            }
            
            log.info("==================== END LOG EVENT PROCESSING ====================");
            
        } catch (Exception e) {
            log.error("Error processing log event: {}", e.getMessage(), e);
            // Print stack trace for debugging
            e.printStackTrace();
            log.info("==================== END LOG EVENT PROCESSING WITH ERROR ====================");
        }
    }
    
    /**
     * Create a notification for a comment
     * @param logEvent The log event
     * @param recipientEmail The recipient's email
     */
    private void createCommentNotification(LogEvent logEvent, String recipientEmail) {
        try {
            // Create a notification for a new comment
            String subject = "New Comment Added";
            String message = logEvent.description();
            
            log.info("Creating comment notification for recipient: {}", recipientEmail);
            
            // Use logId as ticketId - logId is extracted from container_id in TicketsLogEvent.toLogEvent()
            Long ticketId = logEvent.logId();
            
            Notification notification = Notification.builder()
                    .subject(subject.length() > 200 ? subject.substring(0, 200) : subject)
                    .message(message.length() > 200 ? message.substring(0, 200) : message)
                    .sourceType("COMMENT")
                    .sourceId(ticketId) // Use ticket ID instead of comment ID
                    .tenant(logEvent.tenant())
                    .recipientEmail(recipientEmail)
                    .senderEmail(logEvent.senderEmail())
                    .actionType("COMMENT_ADDED")
                    .read(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            // Save the notification
            notificationService.createNotification(notification);
            log.info("Comment notification created for ticket ID: {}, tenant: {}, recipient: {}", 
                    ticketId, logEvent.tenant(), recipientEmail);
        } catch (Exception e) {
            log.error("Error creating comment notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Create a notification for a solution
     * @param logEvent The log event
     * @param recipientEmail The recipient's email
     */
    private void createSolutionNotification(LogEvent logEvent, String recipientEmail) {
        try {
            // Create a notification for a new solution
            String subject = "Solution Added to Ticket";
            String message = logEvent.description();
            
            log.info("Creating solution notification for recipient: {}", recipientEmail);
            
            // Use logId as ticketId - logId is extracted from container_id in TicketsLogEvent.toLogEvent()
            Long ticketId = logEvent.logId();
            
            Notification notification = Notification.builder()
                    .subject(subject.length() > 200 ? subject.substring(0, 200) : subject)
                    .message(message.length() > 200 ? message.substring(0, 200) : message)
                    .sourceType("SOLUTION")
                    .sourceId(ticketId) // Use ticket ID instead of solution ID
                    .tenant(logEvent.tenant())
                    .recipientEmail(recipientEmail)
                    .senderEmail(logEvent.senderEmail())
                    .actionType("SOLUTION_ADDED")
                    .read(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            // Save the notification
            notificationService.createNotification(notification);
            log.info("Solution notification created for ticket ID: {}, tenant: {}, recipient: {}", 
                    ticketId, logEvent.tenant(), recipientEmail);
        } catch (Exception e) {
            log.error("Error creating solution notification: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Create a notification for a log event
     * @param logEvent The log event
     * @param recipientEmail The recipient's email
     */
    private void createLogNotification(LogEvent logEvent, String recipientEmail) {
        try {
            // Create a very simple notification
            String subject = "Alert: Error in " + logEvent.class_name();
            String message = "Error detected in " + logEvent.class_name();
            
            log.info("Creating log notification for recipient: {}", recipientEmail);
            
            // Create notification with short strings to avoid exceeding the varchar(255) limit
            Notification notification = Notification.builder()
                    .subject(subject.length() > 200 ? subject.substring(0, 200) : subject)
                    .message(message.length() > 200 ? message.substring(0, 200) : message)
                    .sourceType("LOG")
                    .sourceId(logEvent.logId())
                    .tenant(logEvent.tenant())
                    .recipientEmail(recipientEmail)
                    .senderEmail(logEvent.senderEmail())
                    .read(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            // Save the notification
            notificationService.createNotification(notification);
            log.info("Log notification created for log ID: {}, tenant: {}, recipient: {}", 
                    logEvent.logId(), logEvent.tenant(), recipientEmail);
        } catch (Exception e) {
            log.error("Error creating notification: {}", e.getMessage(), e);
        }
    }
} 