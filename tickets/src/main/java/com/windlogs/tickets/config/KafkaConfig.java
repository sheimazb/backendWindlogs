package com.windlogs.tickets.config;

import com.windlogs.tickets.kafka.LogEvent;
import com.windlogs.tickets.kafka.TicketP;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.producer.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    // Common producer configuration
    private Map<String, Object> getCommonProducerConfig() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return configProps;
    }

    @Bean
    public ProducerFactory<String, LogEvent> logEventProducerFactory() {
        Map<String, Object> configProps = getCommonProducerConfig();
        configProps.put(JsonSerializer.TYPE_MAPPINGS, "logEvent:com.windlogs.tickets.kafka.LogEvent");
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, LogEvent> logEventKafkaTemplate() {
        return new KafkaTemplate<>(logEventProducerFactory());
    }

    @Bean
    public ProducerFactory<String, TicketP> ticketProducerFactory() {
        Map<String, Object> configProps = getCommonProducerConfig();
        configProps.put(JsonSerializer.TYPE_MAPPINGS, "ticketP:com.windlogs.tickets.kafka.TicketP");
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, TicketP> kafkaTemplate() {
        return new KafkaTemplate<>(ticketProducerFactory());
    }
} 