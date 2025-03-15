package com.windlogs.tickets.dto;

import com.windlogs.tickets.enums.Priority;
import com.windlogs.tickets.enums.Status;
import lombok.Data;

import java.util.List;

@Data
public class TicketDTO{
    private Long id;
    private Status status;
    private List<String> attachments;
    private Priority priority;
    private String assignedTo;
    private String tenant;
    private Long userId;
    private String userEmail;
    private Long logId;
    private Boolean hasSolution;
}
