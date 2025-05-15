package com.windlogs.tickets.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ExecutionException;

/**
 * Producer for domain-specific notification events.
 * Handles sending comment and solution events to Kafka.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer {

    private final KafkaTemplate<String, NotificationEvent> notificationKafkaTemplate;
    private static final String TOPIC = "notification-events-topic";

    /**
     * Generic method to send any notification event
     * @param event The notification event to send
     */
    public void sendNotificationEvent(NotificationEvent event) {
        log.info("KAFKA DEBUG: Sending {} notification: sourceId={}, relatedEntityId={}, recipient={}, eventType={}, class={}", 
                event.getEventType(), event.getSourceId(), event.getRelatedEntityId(), 
                event.getRecipientEmail(), event.getClass().getSimpleName(), event.getClass().getName());
        
        Message<NotificationEvent> message = MessageBuilder
                .withPayload(event)
                .setHeader(KafkaHeaders.TOPIC, TOPIC)
                .build();
        
        try {
            log.info("KAFKA DEBUG: Sending Kafka message to topic: {} with headers: {}", TOPIC, message.getHeaders());
            log.info("KAFKA DEBUG: Message payload type: {}", event.getClass().getName());
            log.info("KAFKA DEBUG: Message full content: {}", event);
            
            // Send the message and get the result
            var sendResult = notificationKafkaTemplate.send(message);
            // Wait for the result but handle potential exceptions
            try {
                log.info("KAFKA DEBUG: Waiting for send result...");
                var result = sendResult.get();
                log.info("KAFKA DEBUG: {} notification successfully sent to Kafka broker - topic: {}, partition: {}, offset: {}", 
                        event.getEventType(), result.getRecordMetadata().topic(), 
                        result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            } catch (InterruptedException | ExecutionException e) {
                log.error("KAFKA DEBUG: Error getting send result: {}", e.getMessage(), e);
                // Restore the interrupted status if needed
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception e) {
            log.error("KAFKA DEBUG: Error class: {}", e.getClass().getName());
            log.error("KAFKA DEBUG: Error sending {} notification to Kafka: {}", event.getEventType(), e.getMessage(), e);
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Send a comment notification event
     * @param commentId ID of the comment
     * @param content Comment content
     * @param ticketId ID of the ticket
     * @param authorName Name of comment author
     * @param senderEmail Email of the comment author (sender)
     * @param recipientEmail Email of the recipient
     * @param tenant Tenant of the user
     */
    public void sendCommentNotification(Long commentId, String content, Long ticketId, 
                                        String authorName, String senderEmail, 
                                        String recipientEmail, String tenant) {
        log.info("Preparing comment notification: comment_id={}, ticket_id={}, recipient={}", 
                commentId, ticketId, recipientEmail);
        
        CommentEvent commentEvent = new CommentEvent(
                Instant.now().toString(),
                tenant,
                commentId,
                ticketId,
                senderEmail,
                recipientEmail,
                content,
                authorName
        );
        
        sendNotificationEvent(commentEvent);
    }
    
    /**
     * Send a solution notification event
     * @param solutionId ID of the solution
     * @param content Solution content
     * @param ticketId ID of the ticket
     * @param authorName Name of solution author
     * @param status Status of the solution as a string
     * @param senderEmail Email of the solution author (sender)
     * @param recipientEmail Email of the recipient
     * @param tenant Tenant of the user
     */
    public void sendSolutionNotification(Long solutionId, String content, Long ticketId,
                                         String authorName, String status,
                                         String senderEmail, String recipientEmail, String tenant) {
        log.info("Preparing solution notification: solution_id={}, ticket_id={}, recipient={}", 
                solutionId, ticketId, recipientEmail);
        
        SolutionEvent solutionEvent = new SolutionEvent(
                String.valueOf(System.currentTimeMillis()),
                tenant,
                solutionId,
                ticketId,
                senderEmail,
                recipientEmail,
                content,
                authorName,
                status
        );
        
        // Use the common send method instead of custom implementation
        sendNotificationEvent(solutionEvent);
    }

    /**
     * Test method to send a test solution notification (for debugging)
     * Can be called from a controller or service method for testing
     */
    public void sendTestSolutionNotification() {
        log.info("Sending TEST solution notification");
        
        SolutionEvent solutionEvent = new SolutionEvent(
                Instant.now().toString(),
                "test-tenant",
                999L, // Test solution ID
                888L, // Test ticket ID
                "test-sender@example.com", // Sender email
                "test-recipient@example.com", // Recipient email
                "This is a test solution notification", // Content
                "Test User", // Author name
                "DRAFT" // Status
        );
        
        sendNotificationEvent(solutionEvent);
    }
} 