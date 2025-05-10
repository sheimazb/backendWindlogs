package com.windlogs.tickets.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.windlogs.tickets.dto.*;
import com.windlogs.tickets.entity.Log;
import com.windlogs.tickets.repository.LogRepository;
import com.windlogs.tickets.service.AuthService;
import com.windlogs.tickets.service.LogService;
import com.windlogs.tickets.service.ProjectService;
import com.windlogs.tickets.service.TicketService;
import com.windlogs.tickets.service.ExceptionAnalyzerService;
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
import com.windlogs.tickets.enums.LogSeverity;
import com.windlogs.tickets.enums.LogType;
import com.windlogs.tickets.service.ExceptionAnalyzerService.StackTraceAnalysisResponse;

@RestController
@RequestMapping("/api/v1/logs")
public class LogController {
    private static final Logger logger = LoggerFactory.getLogger(LogController.class);
    private final LogService logService;
    private final AuthService authService;
    private final LogRepository logRepository;
    private final ObjectMapper objectMapper;
    private final TicketService ticketService;
    private final ProjectService projectService;
    private final ExceptionAnalyzerService exceptionAnalyzerService;

    public LogController(
            LogService logService, 
            TicketService ticketService, 
            AuthService authService, 
            LogRepository logRepository, 
            ObjectMapper objectMapper, 
            ProjectService projectService,
            ExceptionAnalyzerService exceptionAnalyzerService) {
        this.logService = logService;
        this.authService = authService;
        this.logRepository = logRepository;
        this.objectMapper = objectMapper;
        this.ticketService = ticketService;
        this.projectService = projectService;
        this.exceptionAnalyzerService = exceptionAnalyzerService;
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

    @PostMapping(
            value = "/fluentd",
            consumes = {"application/json", "application/x-ndjson", "application/x-ndjson;charset=UTF-8", "text/plain", "*/*"}
    )
    public ResponseEntity<?> receiveFluentdLog(@RequestBody String rawJsonBody) {
        try {
            logger.debug("Received log from Fluentd: {}", rawJsonBody);

            FluentdLogRequest fluentdLog = objectMapper.readValue(rawJsonBody, FluentdLogRequest.class);
            Log log = new Log();

            // Set timestamp
            if (fluentdLog.getTimestamp() != 0) {
                try {
                    log.setTimestamp(LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(fluentdLog.getTimestamp()),
                            ZoneId.systemDefault()
                    ));
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

            log.setType(determineLogType(fluentdLog.getLevel()));
            log.setDescription(fluentdLog.getMessage() != null ? fluentdLog.getMessage() : "No message");
            log.setCustomMessage(fluentdLog.getMessage() != null ? fluentdLog.getMessage() : "No message");
            log.setSource(fluentdLog.getSource() != null ? fluentdLog.getSource() : "fluentd");
            log.setSeverity(determineLogSeverity(fluentdLog.getLevel()));

            String containerTenant = extractTenant(fluentdLog.getContainer_name());
            log.setTenant(containerTenant);

            String logTag = fluentdLog.getTag() != null ? fluentdLog.getTag() : "";
            log.setTag(logTag);

            // Extract and analyze exception if the log contains one
            if (fluentdLog.getMessage() != null && fluentdLog.getThrown() != null) {
                // Extract stack trace from thrown.extendedStackTrace
                String extendedStackTrace = fluentdLog.getThrown().getExtendedStackTrace();
                if (extendedStackTrace != null) {
                    StackTraceAnalysisResponse analysisResponse = exceptionAnalyzerService.analyzeStackTrace(extendedStackTrace);

                    if (analysisResponse != null) {
                        // Set the stack trace
                        if (analysisResponse.getStackTrace() != null) {
                            log.setStackTrace(analysisResponse.getStackTrace());
                        }
                        if (analysisResponse.getAnalysis() != null) {
                            // Set individual analysis fields
                            try {
                                String analysisJson = objectMapper.writeValueAsString(analysisResponse.getAnalysis());
                                log.setAnalysis(analysisJson);
                            } catch (Exception e) {
                                logger.warn("Failed to convert analysis to JSON: {}", e.getMessage());  }
                        }
                    }
                }

                // Set exception type from thrown object if available
                String exceptionType = fluentdLog.getThrown().getName();
                if (exceptionType == null || exceptionType.isEmpty()) {
                    exceptionType = exceptionAnalyzerService.analyzeException(fluentdLog.getMessage());
                }
                log.setExceptionType(exceptionType);
                logger.info("Analyzed exception type: {}", exceptionType);
            }

            if (logTag != null && !logTag.isEmpty()) {
                try {
                    logService.findProjectByTagPublic(logTag)
                            .ifPresentOrElse(
                                    project -> {
                                        log.setProjectId(project.getId());
                                        if (project.getTenant() != null && !project.getTenant().isEmpty()) {
                                            log.setTenant(project.getTenant());
                                            logger.info("Using project tenant: {} for log", project.getTenant());
                                        }
                                        logger.info("Matched log with tag {} to project {} (tenant: {})",
                                                logTag, project.getId(), project.getTenant());
                                    },
                                    () -> {
                                        log.setProjectId(1L);
                                        logger.info("No project found with tag {}, using default project ID and container tenant: {}",
                                                logTag, containerTenant);
                                    }
                            );
                } catch (Exception e) {
                    logger.error("Error finding project by tag: {}", e.getMessage());
                    log.setProjectId(1L);
                }
            } else {
                log.setProjectId(1L);
            }

            log.setErrorCode("FL_" + System.currentTimeMillis());
            log.setPid(fluentdLog.getPid());
            log.setThread(fluentdLog.getThread());
            log.setClassName(fluentdLog.getClass_name());
            log.setContainerId(fluentdLog.getContainer_id());
            log.setContainerName(fluentdLog.getContainer_name());

            Log savedLog = logRepository.save(log);
            logger.info("Successfully saved log with ID: {}", savedLog.getId());
            
            TicketDTO ticketDTO = new TicketDTO();

            List<UserResponseDTO> projectMembers = projectService.getProjectUsers(log.getProjectId());
            logger.info("Found {} project members for project ID: {}", projectMembers.size(), log.getProjectId());

            UserResponseDTO assignee = projectMembers.stream()
                    .filter(user -> "MANAGER".equalsIgnoreCase(user.getRole()))
                    .findFirst()
                    .orElse(projectMembers.isEmpty() ? null : projectMembers.get(0));

            if (assignee != null) {
                ticketDTO.setCreatorUserId(assignee.getId());
                ticketDTO.setUserEmail(assignee.getEmail());
                logger.info("Assigned ticket to user: {} ({})", assignee.getEmail(), assignee.getRole());
            } else {
                ticketDTO.setCreatorUserId(1L);
                ticketDTO.setUserEmail("admin@windlogs.com");
                logger.warn("No suitable assignee found, using default admin account");
            }

            // Automatically create a ticket
            ticketDTO.setLogId(savedLog.getId());
            ticketDTO.setTitle("Auto-generated ticket for log " + savedLog.getId());
            ticketDTO.setDescription(savedLog.getDescription());
            ticketDTO.setTenant(savedLog.getTenant());

            // Create a ticket with appropriate data
            TicketDTO createdTicket = ticketService.createTicket(ticketDTO);
            logger.info("Ticket created with ID: {}, tenant: {}", createdTicket.getId(), createdTicket.getTenant());
            
            // Send log notification with ticket data including userEmail
            logService.sendLogNotification(savedLog, createdTicket.getTenant(), ticketDTO.getUserEmail());
            logger.info("Notification sent for log ID: {}, tenant: {}, userEmail: {}", 
                    savedLog.getId(), createdTicket.getTenant(), ticketDTO.getUserEmail());
            
            return ResponseEntity.ok().body(Map.of(
                    "status", "success",
                    "id", savedLog.getId(),
                    "ticketId", createdTicket.getId(),
                    "message", "Log and ticket saved successfully"
            ));

        } catch (Exception e) {
            logger.error("Error processing log: {}", e.getMessage(), e);
            return ResponseEntity.ok().body(Map.of(
                    "status", "error",
                    "message", "Error processing log: " + e.getMessage()
            ));
        }
    }

    private LogType determineLogType(String level) {
        if (level == null) return LogType.INFO;

        try {
            return LogType.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            logger.warn("Unknown log level: {}, defaulting to INFO", level);
            return LogType.INFO;
        }
    }

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

    private String extractTenant(String containerName) {
        if (containerName == null || !containerName.startsWith("/")) {
            return "default";
        }

        String[] parts = containerName.substring(1).split("_");
        return parts.length > 0 ? parts[0] : "default";
    }

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
        logDTO.setPid(log.getPid());
        logDTO.setThread(log.getThread());
        logDTO.setClassName(log.getClassName());
        logDTO.setContainerId(log.getContainerId());
        logDTO.setContainerName(log.getContainerName());
        logDTO.setOriginalTimestamp(log.getOriginalTimestamp());
        logDTO.setTag(log.getTag());
        logDTO.setStackTrace(log.getStackTrace());
        logDTO.setExceptionType(log.getExceptionType());
        if (log.getAnalysis() != null && !log.getAnalysis().isEmpty()) {
            try {
                logger.info("Converting analysis for log ID: {}. Raw analysis: {}", log.getId(), log.getAnalysis());
                AnalysisInfo analysisInfo = objectMapper.readValue(log.getAnalysis(), AnalysisInfo.class);
                logDTO.setAnalysis(analysisInfo);
                logger.info("Successfully converted analysis for log ID: {}. Converted object: {}", log.getId(), analysisInfo);
            } catch (Exception e) {
                logger.error("Failed to parse analysis JSON for log ID: {}. Error: {}. Raw analysis: {}", 
                    log.getId(), e.getMessage(), log.getAnalysis(), e);
            }
        } else {
            logger.debug("No analysis found for log ID: {}", log.getId());
        }
        return logDTO;
    }
}