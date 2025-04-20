package com.windlogs.notification.config;

import com.windlogs.notification.kafka.NotificationEvent;
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
        return new DefaultKafkaConsumerFactory<>(getCommonConsumerConfig(), 
            new ErrorHandlingDeserializer<>(new StringDeserializer()),
            new ErrorHandlingDeserializer<>(new JsonDeserializer<>(TicketsLogEvent.class, false)));
    }

    @Bean
    public ConsumerFactory<String, NotificationEvent> notificationEventConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(getCommonConsumerConfig(),
            new ErrorHandlingDeserializer<>(new StringDeserializer()),
            new ErrorHandlingDeserializer<>(new JsonDeserializer<>(NotificationEvent.class, false)));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, TicketsLogEvent> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, TicketsLogEvent> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(logEventConsumerFactory());
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 2L)));
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> notificationKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, NotificationEvent> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(notificationEventConsumerFactory());
        factory.setCommonErrorHandler(new DefaultErrorHandler(new FixedBackOff(1000L, 2L)));
        return factory;
    }

    private Map<String, Object> getCommonConsumerConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        return props;
    }
} 