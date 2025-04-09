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
        
        // Skip Kafka event for simplicity
        
        return savedLog;
    }
    
    /**
     * Send a log created event to Kafka
     * @param log The log that was created
     * @param userEmail The email of the user who created the log (optional)
     */
    private void sendLogCreatedEvent(Log log, String userEmail) {
        try {
            // Convert LocalDateTime to Unix timestamp
            double timestamp = log.getOriginalTimestamp() != null 
                ? log.getOriginalTimestamp() 
                : log.getTimestamp().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() / 1000.0;

            // Get values with fallbacks for null fields
            String pid = log.getPid() != null ? log.getPid() : "1";
            String thread = log.getThread() != null ? log.getThread() : log.getErrorCode();
            String className = log.getClassName() != null ? log.getClassName() : log.getSource();
            String containerId = log.getContainerId() != null ? log.getContainerId() : "";
            String containerName = log.getContainerName() != null ? log.getContainerName() : "default_container";

            LogEvent logEvent = new LogEvent(
                log.getTimestamp().toString(), // time
                log.getType().toString(),      // level
                pid,                           // pid
                thread,                        // thread
                className,                     // class_name
                log.getCustomMessage(),        // message
                log.getSource(),               // source
                containerId,                   // container_id
                containerName,                 // container_name
                timestamp                      // timestamp
            );
            
            logProducer.sendLogEvent(logEvent);
            logger.info("Log created event sent for log ID: {}", log.getId());
        } catch (Exception e) {
            // Log the error but don't fail the operation
            logger.error("Failed to send log created event for log ID: {}", log.getId(), e);
        }
    }
} 