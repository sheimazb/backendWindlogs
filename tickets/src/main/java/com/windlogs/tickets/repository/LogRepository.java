package com.windlogs.tickets.repository;

import com.windlogs.tickets.entity.Log;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LogRepository extends JpaRepository<Log, Long> {
    
    /**
     * Find all logs for a specific tenant
     * @param tenant The tenant identifier
     * @return List of logs for the tenant
     */
    List<Log> findByProjectIdIn(List<Long> projectIds);
    
    /**
     * Find logs by project ID
     * @param projectId The project ID
     * @return List of logs for the project
     */
    List<Log> findByProjectId(Long projectId);
} 