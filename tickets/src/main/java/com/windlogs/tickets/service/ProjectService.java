package com.windlogs.tickets.service;

import com.windlogs.tickets.dto.ProjectResponseDTO;
import com.windlogs.tickets.dto.UserResponseDTO;
import com.windlogs.tickets.feign.AuthenticationFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Service for project-related operations
 */
@Service
public class ProjectService {
    private static final Logger logger = LoggerFactory.getLogger(ProjectService.class);
    private final AuthenticationFeignClient authenticationFeignClient;

    public ProjectService(AuthenticationFeignClient authenticationFeignClient) {
        this.authenticationFeignClient = authenticationFeignClient;
    }

    /**
     * Get a project by ID
     * @param projectId The project ID
     * @param authorizationHeader The authorization header
     * @return The project if found
     */
    public ProjectResponseDTO getProjectById(Long projectId, String authorizationHeader) {
        logger.info("Getting project with ID: {}", projectId);
        
        try {
            return authenticationFeignClient.getProjectById(projectId, authorizationHeader);
        } catch (Exception e) {
            logger.error("Error getting project with ID {}: {}", projectId, e.getMessage());
            throw new RuntimeException("Failed to get project by ID", e);
        }
    }
    
    /**
     * Get all projects
     * @param authorizationHeader The authorization header
     * @return List of all projects
     */
    public List<ProjectResponseDTO> getAllProjects(String authorizationHeader) {
        logger.info("Getting all projects");
        
        try {
            return authenticationFeignClient.getAllProjects(authorizationHeader);
        } catch (Exception e) {
            logger.error("Error getting all projects: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Find project by tag
     * @param tag The tag to search for
     * @param authorizationHeader The authorization header
     * @return The project if found, or null if not found
     */
    public Optional<ProjectResponseDTO> findProjectByPrimaryTag(String tag, String authorizationHeader) {
        logger.info("Finding project with tag: {}", tag);
        
        if (tag == null || tag.isEmpty()) {
            logger.warn("Empty tag provided, returning empty result");
            return Optional.empty();
        }
        
        try {
            // Only use the public endpoint
            List<ProjectResponseDTO> projects = authenticationFeignClient.findProjectsByPrimaryTagPublic(tag);
            return projects.stream().findFirst();
        } catch (Exception e) {
            logger.error("Error finding project with tag {}: {}", tag, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Find project's members
     * @param projectId The project ID
     * @return List of users for the project, or empty list if error occurs
     */
    public List<UserResponseDTO> getProjectUsers(Long projectId) {
        logger.info("Finding users of project id: {}", projectId);
        try {
            return authenticationFeignClient.getProjectMembers(projectId);
        } catch (Exception e) {
            logger.error("Error getting project members for project ID {}: {}", projectId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
} 