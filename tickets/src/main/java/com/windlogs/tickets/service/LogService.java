package com.windlogs.tickets.service;

import com.windlogs.tickets.dto.LogDTO;
import com.windlogs.tickets.entity.Log;
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

    public LogService(LogRepository logRepository) {
        this.logRepository = logRepository;
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
     * Create a new log
     * @param log The log to create
     * @return The created log
     */
    public Log createLog(Log log) {
        logger.info("Creating log with type: {}, severity: {}", log.getType(), log.getSeverity());
        return logRepository.save(log);
    }
} 