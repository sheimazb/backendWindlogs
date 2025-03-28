package com.windlogs.notification.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Event format that matches what the tickets service sends
 * Used for proper deserialization of incoming log events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TicketsLogEvent {
    private String time;
    private String level;
    private String pid;
    private String thread;
    private String class_name;
    private String message;
    private String source;
    private String container_id;
    private String container_name;
    private double timestamp;
    
    /**
     * Convert to the notification service's LogEvent format
     */
    public LogEvent toLogEvent() {
        Long logId = System.currentTimeMillis(); // Placeholder since we don't have the actual ID
        return new LogEvent(
                logId,
                level != null ? level.toUpperCase() : "INFO",
                determineSeverity(level),
                message,
                source,
                extractTenant(container_name),
                1L, // Default project ID
                null, // We'll let LogEventConsumer handle the timestamp conversion
                null  // No user email in this format
        );
    }
    
    /**
     * Determine severity based on log level
     */
    private String determineSeverity(String level) {
        if (level == null) return "LOW";
        
        switch (level.toUpperCase()) {
            case "ERROR":
                return "HIGH";
            case "WARN":
                return "MEDIUM";
            default:
                return "LOW";
        }
    }
    
    /**
     * Extract tenant from container name
     */
    private String extractTenant(String containerName) {
        if (containerName != null && containerName.startsWith("/")) {
            String[] parts = containerName.substring(1).split("_");
            if (parts.length > 0) {
                return parts[0];
            }
        }
        return "default";
    }
} 