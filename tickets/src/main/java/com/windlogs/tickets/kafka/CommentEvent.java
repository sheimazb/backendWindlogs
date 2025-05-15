package com.windlogs.tickets.kafka;

/**
 * Event for comment notifications.
 * Contains specific fields related to comments.
 */
public class CommentEvent extends NotificationEvent {
    private String content;      // Comment content
    private String authorName;   // Name of the comment author
    
    // Default constructor for serialization
    public CommentEvent() {
        super();
        setEventType("COMMENT");
    }
    
    public CommentEvent(String time, String tenant, Long commentId, Long ticketId,
                        String senderEmail, String recipientEmail, 
                        String content, String authorName) {
        super("COMMENT", time, tenant, commentId, ticketId, senderEmail, recipientEmail);
        this.content = content;
        this.authorName = authorName;
    }
    
    // Getters and setters
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getAuthorName() {
        return authorName;
    }
    
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }
} 