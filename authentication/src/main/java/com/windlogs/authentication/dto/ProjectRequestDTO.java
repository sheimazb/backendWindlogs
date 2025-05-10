package com.windlogs.authentication.dto;

import com.windlogs.authentication.entity.Project;
import com.windlogs.authentication.entity.ProjectType;
import com.windlogs.authentication.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Simplified DTO for handling project creation and update including logo upload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRequestDTO {
    // Basic project info
    private String name;
    private String description;
    private String technologies; // Can be string directly from frontend
    private List<String> technologiesArray; // Or can be array that will be joined
    private String repositoryLink;
    private String primaryTag;
    private Float progressPercentage;
    
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate deadlineDate;
    
    private Boolean payed;
    
    // Project architecture type
    private ProjectType projectType = ProjectType.MONOLITHIC;
    private Long parentProjectId; // For microservices projects
    
    // Collections
    private List<String> documentationUrls;
    private Set<String> tags;
    
    // Changed from Set<Role> to List<String> for easier binding
    private List<String> allowedRoleNames;
    
    // File upload
    private MultipartFile logo;
    
    /**
     * Converts this DTO to a Project entity
     * Handles technology array conversion automatically
     */
    public Project toProject() {
        Project project = new Project();
        project.setName(name);
        project.setDescription(description);
        
        // Set project type
        if (projectType != null) {
            project.setProjectType(projectType);
        }
        
        // Set parent project reference if provided
        if (parentProjectId != null) {
            Project parentProject = new Project();
            parentProject.setId(parentProjectId);
            project.setParentProject(parentProject);
        }
        
        // Handle technologies (either direct string or array)
        if (technologies != null) {
            project.setTechnologies(technologies);
        } else if (technologiesArray != null && !technologiesArray.isEmpty()) {
            project.setTechnologies(String.join(", ", technologiesArray));
        }
        
        project.setRepositoryLink(repositoryLink);
        project.setPrimaryTag(primaryTag);
        
        if (progressPercentage != null) {
            project.setProgressPercentage(progressPercentage);
        }
        
        project.setDeadlineDate(deadlineDate);
        project.setPayed(payed);
        project.setDocumentationUrls(documentationUrls);
        project.setTags(tags);
        
        // Convert string role names to Role enum values
        if (allowedRoleNames != null && !allowedRoleNames.isEmpty()) {
            Set<Role> roles = allowedRoleNames.stream()
                .map(roleName -> {
                    try {
                        return Role.valueOf(roleName.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(role -> role != null)
                .collect(Collectors.toSet());
            
            project.setAllowedRoles(roles);
        } else {
            project.setAllowedRoles(new HashSet<>());
        }
        
        return project;
    }
} 