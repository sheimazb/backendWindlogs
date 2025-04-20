package com.windlogs.authentication.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectResponse {
    private Long id;
    private String nom;
    private String description;
    private String technologies;
    private String repositoryLink;
    private int progressPercentage;
    private LocalDateTime deadlineAlert;
    private int membersCount;
    private String tags;
    private String primaryTag;
    private String documentationLink;
    private boolean isPayed;
    private LocalDateTime createdDate;
    private String logo;
}