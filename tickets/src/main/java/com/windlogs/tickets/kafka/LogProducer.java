package com.windlogs.tickets.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

/**
 * Producer for sending log events to Kafka
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LogProducer {

    private final KafkaTemplate<String, LogEvent> logEventKafkaTemplate;
    private static final String TOPIC = "log-events-topic";

    /**
     * Send a log event to Kafka
     * @param logEvent The log event to send
     */
    public void sendLogEvent(LogEvent logEvent) {
        log.info("Sending log event to Kafka: time={}, level={}, source={}, container={}", 
                logEvent.time(), logEvent.level(), logEvent.source(), logEvent.container_name());
        
        Message<LogEvent> message = MessageBuilder
                .withPayload(logEvent)
                .setHeader(KafkaHeaders.TOPIC, TOPIC)
                .setHeader("type", "logEvent")
                .build();
        
        try {
            logEventKafkaTemplate.send(message);
            log.info("Log event sent successfully");
        } catch (Exception e) {
            log.error("Error sending log event to Kafka: {}", e.getMessage(), e);
            throw e;
        }
    }
} 