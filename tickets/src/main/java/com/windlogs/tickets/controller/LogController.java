package com.windlogs.tickets.controller;

import com.windlogs.tickets.dto.LogDTO;
import com.windlogs.tickets.dto.UserResponseDTO;
import com.windlogs.tickets.entity.Log;
import com.windlogs.tickets.service.AuthService;
import com.windlogs.tickets.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/logs")
public class LogController {
    private static final Logger logger = LoggerFactory.getLogger(LogController.class);
    private final LogService logService;
    private final AuthService authService;

    public LogController(LogService logService, AuthService authService) {
        this.logService = logService;
        this.authService = authService;
    }

    /**
     * Get logs for a project
     * @param projectId The project ID
     * @param authorizationHeader The authorization header
     * @return List of logs for the project
     */
    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<LogDTO>> getLogsByProjectId(
            @PathVariable Long projectId,
            @RequestHeader("Authorization") String authorizationHeader) {
        
        // Get the authenticated user
        UserResponseDTO user = authService.getAuthenticatedUser(authorizationHeader);
        logger.info("Getting logs for project ID: {}, requested by: {}", projectId, user.getEmail());
        
        // Get logs for the project
        List<Log> logs = logService.getLogsByProjectId(projectId);
        
        // Convert to DTOs
        List<LogDTO> logDTOs = logs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(logDTOs);
    }

    /**
     * Get a log by ID
     * @param logId The log ID
     * @param authorizationHeader The authorization header
     * @return The log if found
     */
    @GetMapping("/{logId}")
    public ResponseEntity<LogDTO> getLogById(
            @PathVariable Long logId,
            @RequestHeader("Authorization") String authorizationHeader) {
        
        // Get the authenticated user
        UserResponseDTO user = authService.getAuthenticatedUser(authorizationHeader);
        logger.info("Getting log with ID: {}, requested by: {}", logId, user.getEmail());
        
        // Get the log
        Log log = logService.getLogById(logId);
        
        return ResponseEntity.ok(convertToDTO(log));
    }

    /**
     * Convert a Log entity to a LogDTO
     * @param log The log entity
     * @return The log DTO
     */
    private LogDTO convertToDTO(Log log) {
        LogDTO logDTO = new LogDTO();
        logDTO.setId(log.getId());
        logDTO.setType(log.getType());
        logDTO.setTimestamp(log.getTimestamp());
        logDTO.setDescription(log.getDescription());
        logDTO.setSource(log.getSource());
        logDTO.setErrorCode(log.getErrorCode());
        logDTO.setCustomMessage(log.getCustomMessage());
        logDTO.setSeverity(log.getSeverity());
        logDTO.setProjectId(log.getProjectId());
        return logDTO;
    }
} 