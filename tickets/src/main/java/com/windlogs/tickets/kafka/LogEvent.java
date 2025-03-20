package com.windlogs.tickets.kafka;

import com.windlogs.tickets.enums.LogSeverity;
import com.windlogs.tickets.enums.LogType;

import java.time.LocalDateTime;

/**
 * Event sent to Kafka when a log is created or updated
 */
public record LogEvent(
        Long logId,
        LogType type,
        LogSeverity severity,
        String description,
        String source,
        String tenant,
        Long projectId,
        LocalDateTime timestamp,
        String userEmail
) {
} 