package com.windlogs.notification.kafka;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Event for comment notifications.
 * Contains specific fields related to comments.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CommentEvent extends NotificationEvent {
    private String content;      // Comment content
    private String authorName;   // Name of the comment author
    
    // Default constructor for serialization handled by @NoArgsConstructor
    
    // Constructor with all fields
    public CommentEvent(String time, String tenant, Long commentId, Long ticketId,
                        String senderEmail, String recipientEmail, 
                        String content, String authorName) {
        super("COMMENT", time, tenant, commentId, ticketId, senderEmail, recipientEmail);
        this.content = content;
        this.authorName = authorName;
    }
} 