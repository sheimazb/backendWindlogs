package com.windlogs.notification.kafka;

import java.time.LocalDateTime;

/**
 * Event received from Kafka when a log is created or updated
 */
public record LogEvent(
        Long logId,
        String type,
        String severity,
        String description,
        String source,
        String tenant,
        Long projectId,
        LocalDateTime timestamp,
        String userEmail
) {
} 