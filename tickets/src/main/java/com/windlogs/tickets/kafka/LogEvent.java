package com.windlogs.tickets.kafka;

/**
 * Event record for log notification via Kafka
 * Simplified to include only essential fields
 */
public record LogEvent(
    String time,
    String level,
    String pid,
    String thread,
    String class_name,
    String message,
    String source,
    String container_id,
    String container_name,
    double timestamp,
    String userEmail,
    Long sourceId,
    String sourceType,
    String senderEmail
) {} 