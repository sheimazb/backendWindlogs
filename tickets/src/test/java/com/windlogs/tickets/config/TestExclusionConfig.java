package com.windlogs.tickets.config;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@ComponentScan(
    basePackages = "com.windlogs.tickets",
    excludeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.windlogs\\.tickets\\.controller\\.CommentController"),
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.windlogs\\.tickets\\.service\\.CommentService")
    }
)
public class TestExclusionConfig {
    // Cette classe exclut spécifiquement les beans qui causent des problèmes dans les tests
} 