package com.windlogs.authentication.dto;

import com.windlogs.authentication.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectMultipartRequest {
    private String name;
    private String description;
    private String technologies;
    private String repositoryLink;
    private String primaryTag;
    private String tenant;
    private MultipartFile logo;
    private Float progressPercentage;
    private LocalDate deadlineDate;
    private Boolean payed;
    private List<String> documentationUrls;
    private Set<String> tags;
    private Set<Role> allowedRoles;
} 