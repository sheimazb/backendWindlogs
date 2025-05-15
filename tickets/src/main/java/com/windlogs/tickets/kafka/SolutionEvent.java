package com.windlogs.tickets.kafka;

import com.windlogs.tickets.enums.SolutionStatus;

/**
 * Event for solution notifications.
 * Contains specific fields related to solutions.
 */
public class SolutionEvent extends NotificationEvent {
    private String content;         // Solution content
    private String authorName;      // Name of the solution author
    private String status;          // Status of the solution as a string
    
    // Default constructor for serialization
    public SolutionEvent() {
        super();
        setEventType("SOLUTION");
    }
    
    public SolutionEvent(String time, String tenant, Long solutionId, Long ticketId,
                         String senderEmail, String recipientEmail, 
                         String content, String authorName, String status) {
        super("SOLUTION", time, tenant, solutionId, ticketId, senderEmail, recipientEmail);
        this.content = content;
        this.authorName = authorName;
        this.status = status;
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    @Override
    public String toString() {
        return "SolutionEvent{" +
                "eventType='" + getEventType() + '\'' +
                ", sourceId=" + getSourceId() +
                ", relatedEntityId=" + getRelatedEntityId() +
                ", senderEmail='" + getSenderEmail() + '\'' +
                ", recipientEmail='" + getRecipientEmail() + '\'' +
                ", content='" + (content != null ? (content.length() > 50 ? content.substring(0, 50) + "..." : content) : "null") + '\'' +
                ", authorName='" + authorName + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
} 