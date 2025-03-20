package com.windlogs.tickets.service;

import com.windlogs.tickets.dto.UserResponseDTO;
import com.windlogs.tickets.feign.AuthenticationFeignClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final AuthenticationFeignClient authenticationFeignClient;

    /**
     * Get the authenticated user from the authentication service
     * @param authorizationHeader Authorization header from the request
     * @return UserResponseDTO containing user information
     */
    public UserResponseDTO getAuthenticatedUser(String authorizationHeader) {
        logger.info("Getting authenticated user with authorization header");
        

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            logger.error("Invalid authorization header format");
            throw new RuntimeException("Invalid authorization header format");
        }
        
        try {
            return authenticationFeignClient.getAuthenticatedUser(authorizationHeader);
        } catch (Exception e) {
            logger.error("Error getting authenticated user: {}", e.getMessage());
            throw new RuntimeException("Failed to get authenticated user", e);
        }
    }
    
    /**
     * Get a user by ID from the authentication service
     * @param userId The ID of the user to retrieve
     * @param authorizationHeader Authorization header from the request
     * @return UserResponseDTO containing user information
     */
    public UserResponseDTO getUserById(Long userId, String authorizationHeader) {
        logger.info("Getting user by ID: {}", userId);
        
        // Make sure the header is properly formatted
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            logger.error("Invalid authorization header format");
            throw new RuntimeException("Invalid authorization header format");
        }
        
        try {
            return authenticationFeignClient.getUserById(userId, authorizationHeader);
        } catch (Exception e) {
            logger.error("Error getting user by ID {}: {}", userId, e.getMessage());
            throw new RuntimeException("Failed to get user by ID", e);
        }
    }
} 