package com.windlogs.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NotificationDTO {
    private Long id;
    private String message;
    private String subject;
    private String sourceType;
    private Long sourceId;
    private String tenant;
    private String recipientEmail;
    private boolean read;
    private LocalDateTime createdAt;
} 