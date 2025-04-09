package com.windlogs.tickets.feign;

import com.windlogs.tickets.dto.ProjectResponseDTO;
import com.windlogs.tickets.dto.UserResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
public class AuthenticationFeignFallback implements AuthenticationFeignClient {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFeignFallback.class);

    @Override
    public UserResponseDTO getAuthenticatedUser(String token) {
        logger.error("Fallback for getAuthenticatedUser called with token: {}", token);
        throw new RuntimeException("Authentication service is not available");
    }
    
    @Override
    public UserResponseDTO getUserById(Long userId, String token) {
        logger.error("Fallback for getUserById called with userId: {}", userId);
        throw new RuntimeException("Authentication service is not available");
    }
    
    @Override
    public ProjectResponseDTO getProjectById(Long projectId, String token) {
        logger.error("Fallback for getProjectById called with projectId: {}", projectId);
        throw new RuntimeException("Authentication service is not available");
    }
    
    @Override
    public List<ProjectResponseDTO> getAllProjects(String token) {
        logger.error("Fallback for getAllProjects called");
        return Collections.emptyList();
    }
    
    @Override
    public List<ProjectResponseDTO> findProjectsByPrimaryTagPublic(String tag) {
        logger.error("Fallback for findProjectsByPrimaryTagPublic called with tag: {}", tag);
        return Collections.emptyList();
    }

    @Override
    public List<UserResponseDTO> getProjectMembers(Long projectId) {
        logger.error("Fallback for getProjectMembers called for projectId: {}", projectId);
        return Collections.emptyList();
    }
} 