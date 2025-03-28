package com.windlogs.tickets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.windlogs.tickets.dto.LogDTO;
import com.windlogs.tickets.dto.UserResponseDTO;
import com.windlogs.tickets.entity.Log;
import com.windlogs.tickets.repository.LogRepository;
import com.windlogs.tickets.service.AuthService;
import com.windlogs.tickets.service.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.windlogs.tickets.dto.FluentdLogRequest;
import com.windlogs.tickets.enums.LogSeverity;
import com.windlogs.tickets.enums.LogType;

@RestController
@RequestMapping("/api/v1/logs")
public class LogController {
    private static final Logger logger = LoggerFactory.getLogger(LogController.class);
    private final LogService logService;
    private final AuthService authService;
    private final LogRepository logRepository;
    private final ObjectMapper objectMapper;

    public LogController(LogService logService, AuthService authService, LogRepository logRepository, ObjectMapper objectMapper) {
        this.logService = logService;
        this.authService = authService;
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;
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
     * Get a log by Tenant
     * @param logTenant The log Tenant
     * @param authorizationHeader The authorization header
     * @return The log if found
     */
    @GetMapping("/logs-by-tenant/{logTenant}")
    public ResponseEntity<List<LogDTO>> getLogByTenant(
            @PathVariable String logTenant,
            @RequestHeader("Authorization") String authorizationHeader) {

        // Get the authenticated user
        UserResponseDTO user = authService.getAuthenticatedUser(authorizationHeader);
        logger.info("Getting logs with Tenant: {}, requested by: {}", logTenant, user.getEmail());

        // Get the logs
        List<Log> logs = logService.getLogByTenant(logTenant);

        // Convert logs to DTOs
        List<LogDTO> logDTOs = logs.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        return ResponseEntity.ok(logDTOs);
    }

    /**
     * Create a new log using fluentd data
     * @param rawJsonBody The log to create
     * @return The created log
     */
    @PostMapping(
            value = "/fluentd",
            consumes = {"application/json", "application/x-ndjson", "application/x-ndjson;charset=UTF-8", "text/plain", "*/*"}
    )
    public ResponseEntity<?> receiveFluentdLog(@RequestBody String rawJsonBody) {
        try {
            // Log the raw request for debugging
            logger.debug("Received log from Fluentd: {}", rawJsonBody);

            // Parse the JSON to our DTO
            FluentdLogRequest fluentdLog = objectMapper.readValue(rawJsonBody, FluentdLogRequest.class);

            // Create a new log entity
            Log log = new Log();

            // Set timestamp (use current time as fallback)
            if (fluentdLog.getTimestamp() != 0) {
                try {
                    log.setTimestamp(LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(fluentdLog.getTimestamp()),
                            ZoneId.systemDefault()
                    ));
                    // Store original timestamp for reference
                    log.setOriginalTimestamp((double) fluentdLog.getTimestamp());
                } catch (Exception e) {
                    logger.warn("Failed to parse timestamp: {}, using current time", fluentdLog.getTimestamp());
                    log.setTimestamp(LocalDateTime.now());
                    log.setOriginalTimestamp((double) System.currentTimeMillis() / 1000);
                }
            } else {
                log.setTimestamp(LocalDateTime.now());
                log.setOriginalTimestamp((double) System.currentTimeMillis() / 1000);
            }

            // Set log type with safe conversion
            log.setType(determineLogType(fluentdLog.getLevel()));

            // Set required text fields with null checks
            log.setDescription(fluentdLog.getMessage() != null ? fluentdLog.getMessage() : "No message");
            log.setCustomMessage(fluentdLog.getMessage() != null ? fluentdLog.getMessage() : "No message");
            log.setSource(fluentdLog.getSource() != null ? fluentdLog.getSource() : "fluentd");

            // Set severity based on log level
            log.setSeverity(determineLogSeverity(fluentdLog.getLevel()));

            // Extract tenant from container name, default to 'default'
            String containerTenant = extractTenant(fluentdLog.getContainer_name());
            log.setTenant(containerTenant); // Initial value

            // Set tag value
            String logTag = fluentdLog.getTag() != null ? fluentdLog.getTag() : "";
            log.setTag(logTag);
            
            // Find matching project by tag and set project ID
            if (logTag != null && !logTag.isEmpty()) {
                try {
                    // Match tag with a project's primaryTag and get project details
                    logService.findProjectByTagPublic(logTag)
                        .ifPresentOrElse(
                            project -> {
                                log.setProjectId(project.getId());
                                
                                // Use project tenant instead of container tenant
                                if (project.getTenant() != null && !project.getTenant().isEmpty()) {
                                    log.setTenant(project.getTenant());
                                    logger.info("Using project tenant: {} for log", project.getTenant());
                                }
                                
                                logger.info("Matched log with tag {} to project {} (tenant: {})", 
                                    logTag, project.getId(), project.getTenant());
                            },
                            () -> {
                                log.setProjectId(1L); // Default project ID if no match found
                                logger.info("No project found with tag {}, using default project ID and container tenant: {}", 
                                    logTag, containerTenant);
                            }
                        );
                } catch (Exception e) {
                    logger.error("Error finding project by tag: {}", e.getMessage());
                    log.setProjectId(1L); // Default project ID on error
                }
            } else {
                // Default project ID if no tag
                log.setProjectId(1L);
            }

            // Create a unique error code
            log.setErrorCode("FL_" + System.currentTimeMillis());

            // Set Fluentd-specific metadata
            log.setPid(fluentdLog.getPid());
            log.setThread(fluentdLog.getThread());
            log.setClassName(fluentdLog.getClass_name());
            log.setContainerId(fluentdLog.getContainer_id());
            log.setContainerName(fluentdLog.getContainer_name());

            // Save to database
            Log savedLog = logRepository.save(log);
            logger.info("Successfully saved log with ID: {}", savedLog.getId());

            return ResponseEntity.ok().body(Map.of(
                    "status", "success",
                    "id", savedLog.getId(),
                    "message", "Log saved successfully"
            ));

        } catch (Exception e) {
            logger.error("Error processing log: {}", e.getMessage(), e);

            // Return 200 OK even on error to prevent Fluentd from retrying
            return ResponseEntity.ok().body(Map.of(
                    "status", "error",
                    "message", "Error processing log: " + e.getMessage()
            ));
        }
    }

    /**
     * Determine LogType from string level
     */
    private LogType determineLogType(String level) {
        if (level == null) return LogType.INFO;

        try {
            return LogType.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown log level: {}, defaulting to INFO", level);
            return LogType.INFO;
        }
    }

    /**
     * Determine LogSeverity from string level
     */
    private LogSeverity determineLogSeverity(String level) {
        if (level == null) return LogSeverity.LOW;

        switch (level.toUpperCase()) {
            case "ERROR":
            case "FATAL":
                return LogSeverity.HIGH;
            case "WARN":
            case "WARNING":
                return LogSeverity.MEDIUM;
            default:
                return LogSeverity.LOW;
        }
    }

    /**
     * Extract tenant from container name
     */
    private String extractTenant(String containerName) {
        if (containerName == null || !containerName.startsWith("/")) {
            return "default";
        }

        String[] parts = containerName.substring(1).split("_");
        return parts.length > 0 ? parts[0] : "default";
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
        logDTO.setTenant(log.getTenant());
        logDTO.setProjectId(log.getProjectId());
        return logDTO;
    }
}