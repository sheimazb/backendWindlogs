package com.windlogs.tickets.dto;

import com.windlogs.tickets.enums.ComplexityLevel;
import com.windlogs.tickets.enums.SolutionStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SolutionDTO {
    private Long id;
    private String title;
    private ComplexityLevel complexity;
    private String content;
    private Long authorUserId;
    private String authorEmail;
    private SolutionStatus status;
    private Integer estimatedTime;
    private Double costEstimation;
    private String category;
    private String tenant;
    private Long ticketId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
} 