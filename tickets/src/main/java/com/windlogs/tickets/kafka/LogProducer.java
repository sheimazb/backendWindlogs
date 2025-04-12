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
        log.info("Sending log notification: level={}, message={}, log_id={}, userEmail={}", 
                logEvent.level(), logEvent.message(), logEvent.container_id(), logEvent.userEmail());
        
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
} 