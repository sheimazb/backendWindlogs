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
        
        // Make sure the header is properly formatted
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
} 