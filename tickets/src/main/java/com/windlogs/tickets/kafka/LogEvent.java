package com.windlogs.tickets.kafka;

/**
 * Event record matching Fluentd log format
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
    double timestamp
) {} 