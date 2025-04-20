package com.windlogs.authentication.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectCreationRequest {
    private String nom;
    private String description;
    private String technologies;
    private String repositoryLink;
    private String tags;
    private String tag;
    private String documentationLink;
    private LocalDateTime deadlineAlert;
    private MultipartFile logo;
}

