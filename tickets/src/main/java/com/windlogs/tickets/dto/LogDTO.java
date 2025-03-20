package com.windlogs.tickets.dto;

import com.windlogs.tickets.enums.LogSeverity;
import com.windlogs.tickets.enums.LogType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LogDTO {
    private Long id;
    private LogType type;
    private LocalDateTime timestamp;
    private String description;
    private String source;
    private String errorCode;
    private String tenant;
    private String customMessage;
    private LogSeverity severity;
    private Long projectId;
    private String userEmail;
} 