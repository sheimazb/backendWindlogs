package com.windlogs.notification.kafka;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Base class for all notification events.
 * Provides common fields needed for all notification types.
 */
@Data
@NoArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "eventType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CommentEvent.class, name = "COMMENT"),
        @JsonSubTypes.Type(value = SolutionEvent.class, name = "SOLUTION")
})
public abstract class NotificationEvent implements Serializable {
    private String eventType;
    private String time;
    private String tenant;
    private Long sourceId;
    private Long relatedEntityId;
    private String senderEmail;
    private String recipientEmail;
    
    // Default constructor handled by lombok

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
} 