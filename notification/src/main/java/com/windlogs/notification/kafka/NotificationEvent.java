package com.windlogs.notification.kafka;

/**
 * Record representing a notification event from Kafka
 */
public record NotificationEvent(
    String message,
    String subject,
    String sourceType,
    Long sourceId,
    String tenant,
    String recipientEmail
) {} 