package com.windlogs.tickets.feign;

import com.windlogs.tickets.dto.UserResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
} 