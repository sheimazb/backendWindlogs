package com.windlogs.notification.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a log event received from the tickets service via Kafka
 * Simplified to focus only on essentials for notification
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Slf4j
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
    private Double timestamp;
    private String userEmail;
    private Long sourceId;
    private String sourceType;
    private String senderEmail;

    /**
     * Convert to internal LogEvent format with extracted info
     * @return LogEvent with extracted log ID, type, etc.
     */
    public LogEvent toLogEvent() {
        // Extract log ID from container_id field
        Long logId = extractLogId(container_id);
        
        // Extract type from level with null safety
        String type = "INFO";
        if (level != null) {
            try {
                type = level.toUpperCase();
            } catch (Exception e) {
                log.warn("Could not convert level to uppercase: {}", e.getMessage());
            }
        }
        
        // Extract severity based on log level
        String severity = determineSeverity(level);
        
        // Extract tenant from source field or message
        String tenant = "default";
        try {
            if (source != null && !source.isEmpty() && !"null".equals(source)) {
                tenant = source;
            } else if (message != null) {
                String extractedTenant = extractTenant(message);
                if (extractedTenant != null && !extractedTenant.isEmpty()) {
                    tenant = extractedTenant;
                }
            }
        } catch (Exception e) {
            log.warn("Error extracting tenant: {}", e.getMessage());
        }
        
        // Extract project ID from message if possible
        Long projectId = null;
        try {
            if (message != null) {
                projectId = extractProjectId(message);
            }
        } catch (Exception e) {
            log.warn("Error extracting project ID: {}", e.getMessage());
        }
        
        // Process message to clean up any unwanted patterns
        String description = "No message available";
        if (message != null) {
            description = message.length() > 200 ? message.substring(0, 200) : message;
        }
        
        // Ensure class_name is not null
        String className = "";
        if (class_name != null) {
            className = class_name.length() > 50 ? class_name.substring(0, 50) : class_name;
        }
        
        // Try to extract recipient email from message
        String recipientEmail = userEmail;
        if (recipientEmail == null || recipientEmail.isEmpty()) {
            // Try to extract email from message as fallback
            recipientEmail = extractRecipientEmail(message);
            
            if (recipientEmail == null || recipientEmail.isEmpty()) {
                recipientEmail = "admin@windlogs.com";
                log.warn("No recipient email found in userEmail field or message, using default admin email");
            } else {
                log.info("Extracted recipient email from message: {}", recipientEmail);
            }
        } else {
            log.info("Using recipient email from userEmail field: {}", recipientEmail);
        }

        // Use provided source ID and type if available, or extract from message
        Long finalSourceId = sourceId != null ? sourceId : logId;
        String finalSourceType = sourceType != null ? sourceType : "LOG";
        
        // Use provided sender email or default if not available
        String finalSenderEmail = senderEmail;
        if (finalSenderEmail == null || finalSenderEmail.isEmpty()) {
            if ("COMMENT".equals(finalSourceType) || "SOLUTION".equals(finalSourceType)) {
                finalSenderEmail = userEmail; // For comments and solutions, use the userEmail as sender
            } else {
                finalSenderEmail = "system@windlogs.com"; // Default system email for logs
            }
        }
        
        // Create LogEvent with the extracted data
        return new LogEvent(
                logId,
                type,
                severity,
                source != null ? (source.length() > 50 ? source.substring(0, 50) : source) : "",
                tenant,
                projectId,
                time != null ? time : String.valueOf(System.currentTimeMillis()),
                description,
                className,
                container_name != null ? 
                    (container_name.length() > 50 ? container_name.substring(0, 50) : container_name) : "",
                recipientEmail,
                finalSourceId,
                finalSourceType,
                finalSenderEmail
        );
    }
    
    private Long extractLogId(String containerId) {
        if (containerId == null || containerId.isEmpty()) {
            return 0L;
        }
        
        try {
            return Long.parseLong(containerId);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }
    
    private String determineSeverity(String level) {
        if (level == null) return "MEDIUM";
        
        switch (level.toUpperCase()) {
            case "ERROR":
            case "FATAL":
                return "HIGH";
            case "WARN":
            case "WARNING":
                return "MEDIUM";
            default:
                return "LOW";
        }
    }
    
    private String extractTenant(String message) {
        if (message == null) return "default";
        
        Pattern pattern = Pattern.compile("tenant[=:](\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(message);
        
        return matcher.find() ? matcher.group(1) : "default";
    }
    
    private Long extractProjectId(String message) {
        if (message == null) return null;
        
        Pattern pattern = Pattern.compile("projectId[=:](\\d+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(message);
        
        if (matcher.find()) {
            try {
                return Long.parseLong(matcher.group(1));
            } catch (NumberFormatException e) {
                return null;
            }
        }
        
        return null;
    }
    
    private String extractRecipientEmail(String message) {
        if (message == null) return null;
        
        // Try to find a userEmail value in the message
        Pattern pattern = Pattern.compile("userEmail[=:](\\S+@\\S+\\.\\S+)", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(message);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        
        // Try to find any email pattern in the message as fallback
        pattern = Pattern.compile("([a-zA-Z0-9._-]+@[a-zA-Z0-9._-]+\\.[a-zA-Z0-9_-]+)", Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(message);
        
        return matcher.find() ? matcher.group(1) : null;
    }
} 