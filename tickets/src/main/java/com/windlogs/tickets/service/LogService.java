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
     * Create a new log from DTO
     * @param logDTO The log DTO to create
     * @param authorizationHeader The authorization header
     * @return The created log
     */
    public Log createLogFromDTO(LogDTO logDTO, String authorizationHeader) {
        logger.info("Creating log from DTO with type: {}, severity: {}, tenant: {}", 
            logDTO.getType(), logDTO.getSeverity(), logDTO.getTenant());
        
        // get the tenant from the project
        if (logDTO.getProjectId() != null) {
            try {
                ProjectResponseDTO project = projectService.getProjectById(logDTO.getProjectId(), authorizationHeader);
                logDTO.setTenant(project.getTenant());
                logger.info("Set tenant from project: {}", project.getTenant());
            } catch (Exception e) {
                logger.error("Error getting project tenant: {}", e.getMessage());
               
            }
        }
        
        // Validate tenant
        if (logDTO.getTenant() == null || logDTO.getTenant().isEmpty()) {
            logger.error("Attempt to create log with null or empty tenant");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant must be specified when creating a log");
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
        
        Log savedLog = logRepository.save(log);
        
        // Send Kafka event for the new log
        sendLogCreatedEvent(savedLog, logDTO.getUserEmail());
        
        return savedLog;
    }
    
    /**
     * Send a log created event to Kafka
     * @param log The log that was created
     * @param userEmail The email of the user who created the log (optional)
     */
    private void sendLogCreatedEvent(Log log, String userEmail) {
        try {
            LogEvent logEvent = new LogEvent(
                log.getId(),
                log.getType(),
                log.getSeverity(),
                log.getDescription(),
                log.getSource(),
                log.getTenant(),
                log.getProjectId(),
                log.getTimestamp(),
                userEmail
            );
            
            logProducer.sendLogEvent(logEvent);
            logger.info("Log created event sent for log ID: {}", log.getId());
        } catch (Exception e) {
            // Log the error but don't fail the operation
            logger.error("Failed to send log created event for log ID: {}", log.getId(), e);
        }
    }
} 