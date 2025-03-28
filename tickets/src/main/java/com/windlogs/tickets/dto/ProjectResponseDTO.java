package com.windlogs.tickets.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * DTO for Project data from the authentication service
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectResponseDTO {
    private Long id;
    private String name;
    private String description;
    private String technologies;
    private String repositoryLink;
    private String primaryTag;
    private float progressPercentage;
    private LocalDate deadlineDate;
    private Integer membersCount;
    private Boolean payed;
    private String tenant;
    private List<String> documentationUrls;
    private Set<String> tags;
} 