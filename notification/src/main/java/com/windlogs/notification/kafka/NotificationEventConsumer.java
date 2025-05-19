package com.windlogs.notification.kafka;

import com.windlogs.notification.notification.Notification;
import com.windlogs.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Consumer for domain-specific notification events.
 * Processes comment and solution notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @Autowired
    SimpMessagingTemplate template;

    /**
     * Generic listener for all notification events
     */
    @KafkaListener(
        topics = "notification-events-topic", 
        groupId = "notification-group",
        containerFactory = "notificationKafkaListenerContainerFactory"
    )
    public void consumeNotificationEvent(NotificationEvent event) {
        log.info("==================== START NOTIFICATION EVENT PROCESSING ====================");
        log.info("CONSUMER DEBUG: Received raw event class: {}", event.getClass().getName());
        log.info("CONSUMER DEBUG: Event type from payload: {}", event.getEventType());
        log.info("Received notification event: type={}, sourceId={}, relatedEntityId={}, recipientEmail={}, class={}", 
                event.getEventType(), event.getSourceId(), event.getRelatedEntityId(), 
                event.getRecipientEmail(), event.getClass().getName());
        log.info("CONSUMER DEBUG: Full event details: {}", event);
        
        try {
            validateNotificationEvent(event);
            
            // Process based on event type
            if (event instanceof CommentEvent) {
                log.info("CONSUMER DEBUG: Processing as CommentEvent");
                processCommentEvent((CommentEvent) event);
            } else if (event instanceof SolutionEvent) {
                log.info("CONSUMER DEBUG: Processing as SolutionEvent, class: {}", event.getClass().getName());
                processSolutionEvent((SolutionEvent) event);
            } else {
                log.warn("CONSUMER DEBUG: Unknown event type: {}, class: {}", 
                         event.getEventType(), event.getClass().getName());
                log.warn("Unknown notification event type: {}, class: {}", 
                         event.getEventType(), event.getClass().getName());
            }
            
            log.info("==================== END NOTIFICATION EVENT PROCESSING ====================");
        } catch (Exception e) {
            log.error("CONSUMER DEBUG: Error processing event: {}, class: {}", e.getMessage(), e.getClass().getName());
            log.error("Error processing notification event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process notification event", e);
        }
    }

    private void validateNotificationEvent(NotificationEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Notification event cannot be null");
        }
        if (event.getSourceId() == null) {
            throw new IllegalArgumentException("Source ID cannot be null");
        }
        if (event.getRecipientEmail() == null || event.getRecipientEmail().isEmpty()) {
            throw new IllegalArgumentException("Recipient email cannot be null or empty");
        }
        if (event.getEventType() == null || event.getEventType().isEmpty()) {
            throw new IllegalArgumentException("Event type cannot be null or empty");
        }
    }

    /**
     * Process a comment event to create a notification
     */
    private void processCommentEvent(CommentEvent event) {
        log.info("Processing comment event: commentId={}, ticketId={}, author={}",
                event.getSourceId(), event.getRelatedEntityId(), event.getAuthorName());
        
        try {
            // Create notification
            String subject = "New Comment by " + event.getAuthorName();
            String message = event.getContent();
            
            Notification notification = Notification.builder()
                    .subject(subject.length() > 200 ? subject.substring(0, 200) : subject)
                    .message(message.length() > 200 ? message.substring(0, 200) : message)
                    .sourceType("COMMENT")
                    .sourceId(event.getRelatedEntityId())
                    .tenant(event.getTenant())
                    .senderEmail(event.getSenderEmail())
                    .recipientEmail(event.getRecipientEmail())
                    .actionType("COMMENT_ADDED")
                    .read(false)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            // Save notification
            notificationService.createNotification(notification);
            log.info("Created comment notification for recipient: {}", event.getRecipientEmail());
            
            // Send WebSocket notification
            try {
                template.convertAndSendToUser(
                    event.getRecipientEmail(),
                    "/queue/notifications",
                    notification
                );
                log.info("Sent WebSocket notification to user: {}", event.getRecipientEmail());
            } catch (Exception e) {
                log.error("Failed to send WebSocket message: {}", e.getMessage());
            }
        } catch (Exception e) {
            log.error("Error creating comment notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Process a solution event to create a notification
     */
    private void processSolutionEvent(SolutionEvent event) {
        log.info("Processing solution event: solutionId={}, ticketId={}, author={}, status={}",
                event.getSourceId(), event.getRelatedEntityId(), event.getAuthorName(), 
                event.getStatus());
        
        log.info("SOLUTION DEBUG: Processing solution event with content: {}", 
                event.getContent() != null ? (event.getContent().length() > 50 ? 
                event.getContent().substring(0, 50) + "..." : event.getContent()) : "null");
        
        try {
            // Create notification
            String subject = "Solution Added by " + event.getAuthorName();
            String message = event.getContent();
            
            log.info("SOLUTION DEBUG: Building notification with subject: {} and event content: {}", 
                    subject, event.getContent());
            
            if (message == null || message.isEmpty()) {
                message = "A solution has been added to ticket #" + event.getRelatedEntityId();
                log.info("SOLUTION DEBUG: Using default message for empty content");
            }
            
            try {
                Notification notification = Notification.builder()
                        .subject(subject.length() > 200 ? subject.substring(0, 200) : subject)
                        .message(message.length() > 200 ? message.substring(0, 200) : message)
                        .sourceType("SOLUTION")
                        .sourceId(event.getRelatedEntityId())
                        .tenant(event.getTenant())
                        .senderEmail(event.getSenderEmail())
                        .recipientEmail(event.getRecipientEmail())
                        .actionType("SOLUTION_ADDED")
                        .read(false)
                        .createdAt(LocalDateTime.now())
                        .build();
                
                log.info("SOLUTION DEBUG: Built notification: {}", notification);
                
                // Save notification
                log.info("SOLUTION DEBUG: Saving solution notification to database...");
                Notification savedNotification = notificationService.createNotification(notification);
                log.info("SOLUTION DEBUG: Successfully saved solution notification with ID: {}", savedNotification.getId());
                
                // Send WebSocket notification
                try {
                    log.info("SOLUTION DEBUG: Attempting to send WebSocket notification to user: {}", 
                            event.getRecipientEmail());
                    template.convertAndSendToUser(
                        event.getRecipientEmail(),
                        "/queue/notifications",
                        notification
                    );
                    log.info("SOLUTION DEBUG: Successfully sent WebSocket notification to user: {}", event.getRecipientEmail());
                } catch (Exception e) {
                    log.error("SOLUTION DEBUG: WebSocket error: {}, {}", e.getClass().getName(), e.getMessage());
                    log.error("Failed to send WebSocket notification: {}", e.getMessage(), e);
                    // Don't throw here - we still want to keep the notification in the database
                }
            } catch (Exception e) {
                log.error("SOLUTION DEBUG: Error building or saving notification: {}, {}", 
                         e.getClass().getName(), e.getMessage());
                throw e;
            }
        } catch (Exception e) {
            log.error("SOLUTION DEBUG: Fatal error in solution processing: {}, {}", 
                     e.getClass().getName(), e.getMessage(), e);
            log.error("Error processing solution notification: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process solution notification", e);
        }
    }
} 