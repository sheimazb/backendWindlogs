package com.windlogs.tickets.service;

import com.windlogs.tickets.dto.ProjectResponseDTO;
import com.windlogs.tickets.feign.AuthenticationFeignClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
} 