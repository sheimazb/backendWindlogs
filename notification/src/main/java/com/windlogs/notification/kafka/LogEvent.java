package com.windlogs.notification.kafka;

/**
 * Internal LogEvent representation for notification processing
 * Simplified to include only essential fields
 */
public record LogEvent(
    Long logId,
    String type,
    String severity,
    String source,
    String tenant,
    Long projectId,
    String timestamp,
    String description,
    String class_name,
    String container_name,
    String recipientEmail,
    Long sourceId,
    String sourceType,
    String senderEmail
) {} 