package com.windlogs.tickets.config;

import com.windlogs.tickets.repository.*;
import com.windlogs.tickets.service.AuthService;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;

/**
 * Configuration de test qui fournit des mocks pour tous les beans nécessaires
 * afin d'éviter les dépendances externes comme la base de données et Kafka.
 */
@Configuration
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
public class TestConfig {

    @Bean
    @Primary
    public CommentRepository commentRepository() {
        return mock(CommentRepository.class);
    }

    @Bean
    @Primary
    public SolutionRepository solutionRepository() {
        return mock(SolutionRepository.class);
    }

    @Bean
    @Primary
    public TicketRepository ticketRepository() {
        return mock(TicketRepository.class);
    }

    @Bean
    @Primary
    public AuthService authService() {
        return mock(AuthService.class);
    }

    @Bean
    @Primary
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return mock(KafkaTemplate.class);
    }
} 