package com.windlogs.tickets.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class LogProducer {

    private final KafkaTemplate<String, LogEvent> logEventKafkaTemplate;
    private static final String TOPIC = "log-events-topic";

    public void sendLogEvent(LogEvent logEvent) {
        log.info("Sending log notification: level={}, message={}, log_id={}, userEmail={}, sourceType={}, sourceId={}", 
                logEvent.level(), logEvent.message(), logEvent.container_id(), logEvent.userEmail(), 
                logEvent.sourceType(), logEvent.sourceId());
        
        Message<LogEvent> message = MessageBuilder
                .withPayload(logEvent)
                .setHeader(KafkaHeaders.TOPIC, TOPIC)
                .build();
        
        try {
            log.info("Sending Kafka message to topic: {} with headers: {}", TOPIC, message.getHeaders());
            logEventKafkaTemplate.send(message);
            log.info("Log notification successfully sent to Kafka broker");
        } catch (Exception e) {
            log.error("Error sending log notification to Kafka: {}", e.getMessage(), e);
            // Print stack trace for debugging
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Send a comment notification event
     * @param commentId ID of the comment
     * @param message Comment message
     * @param ticketId ID of the ticket
     * @param userEmail Email of the comment author
     * @param tenant Tenant of the user
     */
    public void sendCommentNotification(Long commentId, String message, Long ticketId, String userEmail, String tenant) {
        log.info("Sending comment notification: comment_id={}, ticket_id={}, userEmail={}", 
                commentId, ticketId, userEmail);
        
        LogEvent commentEvent = new LogEvent(
                java.time.Instant.now().toString(), // time
                "INFO",                            // level
                String.valueOf(ProcessHandle.current().pid()),     // pid
                Thread.currentThread().getName(),  // thread
                "CommentService",                  // class_name
                message,                           // message
                tenant,                            // source (tenant)
                String.valueOf(ticketId),          // container_id (ticket ID)
                "Ticket",                          // container_name
                System.currentTimeMillis() / 1000.0, // timestamp
                userEmail,                         // userEmail (recipient)
                commentId,                         // sourceId
                "COMMENT",                         // sourceType
                userEmail                          // senderEmail
        );
        
        sendLogEvent(commentEvent);
    }
    
    /**
     * Send a solution notification event
     * @param solutionId ID of the solution
     * @param message Solution message/description
     * @param ticketId ID of the ticket
     * @param userEmail Email of the solution author
     * @param tenant Tenant of the user
     */
    public void sendSolutionNotification(Long solutionId, String message, Long ticketId, String userEmail, String tenant) {
        log.info("Sending solution notification: solution_id={}, ticket_id={}, userEmail={}", 
                solutionId, ticketId, userEmail);
        
        LogEvent solutionEvent = new LogEvent(
                java.time.Instant.now().toString(), // time
                "INFO",                            // level
                String.valueOf(ProcessHandle.current().pid()),     // pid
                Thread.currentThread().getName(),  // thread
                "SolutionService",                 // class_name
                message,                           // message
                tenant,                            // source (tenant)
                String.valueOf(ticketId),          // container_id (ticket ID)
                "Ticket",                          // container_name
                System.currentTimeMillis() / 1000.0, // timestamp
                userEmail,                         // userEmail (recipient)
                solutionId,                        // sourceId
                "SOLUTION",                        // sourceType
                userEmail                          // senderEmail
        );
        
        sendLogEvent(solutionEvent);
    }
} 