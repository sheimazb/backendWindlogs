package com.windlogs.notification.config;

import com.windlogs.notification.kafka.TicketsLogEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka configuration for log event consumption
 * Simplified to focus only on log events from Fluentd
 */
@EnableKafka
@Configuration
public class KafkaConfig {
    private static final Logger logger = LoggerFactory.getLogger(KafkaConfig.class);

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:notification-group}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, TicketsLogEvent> logEventConsumerFactory() {
        logger.info("Configuring Kafka consumer with bootstrap servers: {}", bootstrapServers);
        
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        
        // Configure ErrorHandlingDeserializer as main deserializers
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        
        // Set the delegate deserializers 
        props.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class);
        props.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class.getName());
        
        // Configure the JSON deserializer
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");  // Trust all packages for debugging
        props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, "com.windlogs.notification.kafka.TicketsLogEvent");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        props.put("spring.json.value.default.type", "com.windlogs.notification.kafka.TicketsLogEvent");
        
        // Set auto-offset-reset to earliest to not miss messages
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        // More detailed logging
        logger.info("Kafka consumer configuration: {}", props);
        
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TicketsLogEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TicketsLogEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(logEventConsumerFactory());
        
        // Configure error handler with proper backoff
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
            (record, exception) -> {
                logger.error("Error processing Kafka message: {}", exception.getMessage(), exception);
                logger.error("Record: topic={}, partition={}, offset={}", 
                    record.topic(), record.partition(), record.offset());
                
                // Log the received data for debugging
                if (record.value() != null) {
                    try {
                        logger.error("Record value: {}", record.value().toString());
                    } catch (Exception e) {
                        logger.error("Could not convert record value to string", e);
                    }
                }
            },
            new FixedBackOff(1000L, 3) // Retry 3 times with 1 second interval
        );
        
        factory.setCommonErrorHandler(errorHandler);
        return factory;
    }
} 