package com.windlogs.tickets.kafka;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.Serializable;

/**
 * Base class for all notification events.
 * Provides common fields needed for all notification types.
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "eventType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CommentEvent.class, name = "COMMENT"),
        @JsonSubTypes.Type(value = SolutionEvent.class, name = "SOLUTION")
})
public abstract class NotificationEvent implements Serializable {
    private String eventType; // The type of event (LOG, COMMENT, SOLUTION)
    private String time;      // Timestamp when the event occurred
    private String tenant;    // Tenant identifier
    private Long sourceId;    // ID of the source entity (log, comment, solution)
    private Long relatedEntityId; // ID of related entity (e.g., ticket ID)
    private String senderEmail;   // Email of the user who triggered the event
    private String recipientEmail; // Email of the intended recipient

    // Default constructor for serialization
    protected NotificationEvent() {}

    protected NotificationEvent(String eventType, String time, String tenant, 
                                Long sourceId, Long relatedEntityId,
                                String senderEmail, String recipientEmail) {
        this.eventType = eventType;
        this.time = time;
        this.tenant = tenant;
        this.sourceId = sourceId;
        this.relatedEntityId = relatedEntityId;
        this.senderEmail = senderEmail;
        this.recipientEmail = recipientEmail;
    }

    // Getters and setters
    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public Long getRelatedEntityId() {
        return relatedEntityId;
    }

    public void setRelatedEntityId(Long relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
    }

    public String getSenderEmail() {
        return senderEmail;
    }

    public void setSenderEmail(String senderEmail) {
        this.senderEmail = senderEmail;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }
} 