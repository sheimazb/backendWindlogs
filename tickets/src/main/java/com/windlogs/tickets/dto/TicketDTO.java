package com.windlogs.tickets.dto;

import com.windlogs.tickets.enums.Priority;
import com.windlogs.tickets.enums.Status;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TicketDTO {
    private Long id;
    private String title;
    private String description;
    private Status status;
    private List<String> attachments;
    private Priority priority;
    private Long assignedToUserId;
    private String tenant;
    private Long creatorUserId;
    private String userEmail;
    private Long logId;
    private Boolean hasSolution;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

