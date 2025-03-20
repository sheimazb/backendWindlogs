package com.windlogs.tickets.feign;

import com.windlogs.tickets.dto.ProjectResponseDTO;
import com.windlogs.tickets.dto.UserResponseDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "authentication-service", url = "${authentication.service.url:http://localhost:8088}", configuration = FeignConfig.class, fallback = AuthenticationFeignFallback.class)
public interface AuthenticationFeignClient {

    @GetMapping("/api/v1/auth/user")
    UserResponseDTO getAuthenticatedUser(@RequestHeader("Authorization") String token);
    
    @GetMapping("/api/v1/auth/users/{userId}")
    UserResponseDTO getUserById(@PathVariable("userId") Long userId, @RequestHeader("Authorization") String token);
    
    @GetMapping("/api/v1/projects/{projectId}")
    ProjectResponseDTO getProjectById(@PathVariable("projectId") Long projectId, @RequestHeader("Authorization") String token);
}
