package com.windlogs.tickets.service;

import com.windlogs.tickets.dto.LogDTO;
import com.windlogs.tickets.dto.ProjectResponseDTO;
import com.windlogs.tickets.entity.Log;
import com.windlogs.tickets.kafka.LogEvent;
import com.windlogs.tickets.kafka.LogProducer;
import com.windlogs.tickets.repository.LogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.ZoneId;

@Service
public class LogService {
    private static final Logger logger = LoggerFactory.getLogger(LogService.class);
    private final LogRepository logRepository;
    private final LogProducer logProducer;
    private final ProjectService projectService;

    public LogService(LogRepository logRepository, LogProducer logProducer, ProjectService projectService) {
        this.logRepository = logRepository;
        this.logProducer = logProducer;
        this.projectService = projectService;
    }

    /**
     * Get a log by ID
     * @param logId The log ID
     * @return The log if found
     */
    public Log getLogById(Long logId) {
        logger.info("Getting log by ID: {}", logId);
        return logRepository.findById(logId)
                .orElseThrow(() -> {
                    logger.error("Log not found with ID: {}", logId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Log not found");
                });
    }

    /**
     * Get a log by Tenant
     * @param logTenant The log Tenant
     * @return The log if found
     */
    public List<Log> getLogByTenant(String logTenant) {
        logger.info("Getting logs By Tenant: {}", logTenant);
        return logRepository.findByTenant(logTenant);

    }

    /**
     * Get logs by project ID
     * @param projectId The project ID
     * @return List of logs for the project
     */
    public List<Log> getLogsByProjectId(Long projectId) {
        logger.info("Getting logs for project ID: {}", projectId);
        return logRepository.findByProjectId(projectId);
    }

    /**
     * Get logs by project IDs
     * @param projectIds List of project IDs
     * @return List of logs for the projects
     */
    public List<Log> getLogsByProjectIds(List<Long> projectIds) {
        logger.info("Getting logs for project IDs: {}", projectIds);
        return logRepository.findByProjectIdIn(projectIds);
    }

    /**
     * Find a project by tag (public endpoint - no auth required)
     * @param tag The tag to search for
     * @return The complete project if found
     */
    public Optional<ProjectResponseDTO> findProjectByTagPublic(String tag) {
        logger.info("Finding complete project for tag: {}", tag);
        
        if (tag == null || tag.isEmpty()) {
            logger.warn("Empty tag provided, returning empty result");
            return Optional.empty();
        }
        
        try {
            // Use the public endpoint to find a project by primary tag
            return projectService.findProjectByPrimaryTag(tag, "");
        } catch (Exception e) {
            logger.error("Error finding project with tag {}: {}", tag, e.getMessage());
            return Optional.empty();
        }
    }
    
     
    /**
     * Create a new log from DTO
     * @param logDTO The log DTO to create
     * @param authorizationHeader The authorization header (can be null for simplified version)
     * @return The created log
     */
    public Log createLogFromDTO(LogDTO logDTO, String authorizationHeader) {
        logger.info("Creating log from DTO with type: {}, severity: {}, tenant: {}", 
            logDTO.getType(), logDTO.getSeverity(), logDTO.getTenant());
        
        // Skip project tenant lookup for simplicity
        
        // Use default tenant if not provided
        if (logDTO.getTenant() == null || logDTO.getTenant().isEmpty()) {
            logDTO.setTenant("default");
            logger.info("Using default tenant");
        }
        
        Log log = new Log();
        log.setType(logDTO.getType());
        log.setTimestamp(logDTO.getTimestamp());
        log.setDescription(logDTO.getDescription());
        log.setSource(logDTO.getSource());
        log.setErrorCode(logDTO.getErrorCode());
        log.setCustomMessage(logDTO.getCustomMessage());
        log.setSeverity(logDTO.getSeverity());
        log.setTenant(logDTO.getTenant());
        log.setProjectId(logDTO.getProjectId());
        
        // Set new Fluentd fields if they exist
        log.setPid(logDTO.getPid());
        log.setThread(logDTO.getThread());
        log.setClassName(logDTO.getClassName());
        log.setContainerId(logDTO.getContainerId());
        log.setContainerName(logDTO.getContainerName());
        log.setOriginalTimestamp(logDTO.getOriginalTimestamp());
        
        Log savedLog = logRepository.save(log);
        logger.info("Saved log with ID: {}", savedLog.getId());
        
        // Send notification via Kafka
       // sendLogCreatedEvent(savedLog, logDTO.getUserEmail());
        
        return savedLog;
    }
    
    /**
     * Public method to send a log notification
     * Can be called externally to ensure notifications are sent for logs
     * @param log The log to send a notification for
     * @param tenant The tenant from the ticket
     * @param userEmail The user email from the ticket
     */
    public void sendLogNotification(Log log, String tenant, String userEmail) {
        sendLogCreatedEvent(log, tenant, userEmail);
    }
    
    /**
     * Send a log created event to Kafka
     * @param log The log that was created
     * @param tenant The tenant from the ticket
     * @param userEmail The user email from the ticket
     */
    private void sendLogCreatedEvent(Log log, String tenant, String userEmail) {
        try {
            // Get timestamp
            double timestamp = log.getOriginalTimestamp() != null 
                ? log.getOriginalTimestamp() 
                : log.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000.0;

            // Get essential log information
            String className = log.getClassName() != null ? log.getClassName() : log.getSource();
            if (className == null) className = "Unknown";
            
            // Simple message
            String message = "Error in " + className;
            
            // Create a simple log event with essential information
            LogEvent logEvent = new LogEvent(
                    log.getTimestamp().toString(),
                    log.getType() != null ? log.getType().name() : "INFO",
                    log.getPid() != null ? log.getPid() : "1",
                    log.getThread() != null ? log.getThread() : "",
                    className,
                    message,
                    tenant, // Use the tenant from the ticket
                    log.getId().toString(),
                    log.getContainerName() != null ? log.getContainerName() : "windlogs",
                    timestamp,
                    userEmail,  // Send the user email from the ticket
                    log.getId(), // sourceId - using log ID
                    "LOG",      // sourceType - this is a log notification
                    "system@windlogs.com" // senderEmail - system generated logs
            );

            // Send the event
            logProducer.sendLogEvent(logEvent);
            logger.info("Log notification sent for log ID: {}, tenant: {}, userEmail: {}", log.getId(), tenant, userEmail);
        } catch (Exception e) {
            logger.error("Error sending log notification: {}", e.getMessage(), e);
        }
    }
} 