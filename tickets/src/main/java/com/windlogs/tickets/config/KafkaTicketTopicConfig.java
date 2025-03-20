package com.windlogs.tickets.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTicketTopicConfig {
    @Bean
    public NewTopic ticketTopic() {
        return TopicBuilder
                .name("ticket-topic")
                .build();
    }

    @Bean
    public NewTopic logEventsTopic() {
        return TopicBuilder
                .name("log-events-topic")
                .partitions(3)  // Multiple partitions for better parallelism
                .replicas(1)    // Set to higher value in production
                .build();
    }
}
