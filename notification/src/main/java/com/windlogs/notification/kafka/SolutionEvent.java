package com.windlogs.notification.kafka;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Event for solution notifications.
 * Contains specific fields related to solutions.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@ToString(callSuper = true)
public class SolutionEvent extends NotificationEvent {
    private String content;        
    private String authorName;     
    private String status;          
    
    // Constructor with all fields
    public SolutionEvent(String time, String tenant, Long solutionId, Long ticketId,
                         String senderEmail, String recipientEmail, 
                         String content, String authorName, String status) {
        super("SOLUTION", time, tenant, solutionId, ticketId, senderEmail, recipientEmail);
        this.content = content;
        this.authorName = authorName;
        this.status = status;
    }
    
    // Ensure eventType is always set correctly
    @Override
    public void setEventType(String eventType) {
        super.setEventType("SOLUTION"); // Always force SOLUTION type
    }
} 