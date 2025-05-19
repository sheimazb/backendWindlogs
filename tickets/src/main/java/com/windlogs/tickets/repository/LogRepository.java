package com.windlogs.tickets.repository;

import com.windlogs.tickets.dto.ActivityStatsDTO;
import com.windlogs.tickets.dto.LogStatsDTO;
import com.windlogs.tickets.entity.Log;
import com.windlogs.tickets.enums.LogType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public interface LogRepository extends JpaRepository<Log, Long> {
    

    List<Log> findByProjectIdIn(List<Long> projectIds);
    List<Log> findByProjectId(Long projectId);
    List<Log> findByTenant(String tenant);

    @Query(nativeQuery = true, 
           value = "SELECT DATE(timestamp) as day, type, COUNT(*) as count " +
           "FROM logs WHERE project_id = :projectId " +
           "GROUP BY DATE(timestamp), type ORDER BY DATE(timestamp)")
    List<Object[]> getLogStatsByDayForProjectRaw(@Param("projectId") Long projectId);
    
    @Query(nativeQuery = true,
           value = "SELECT DATE(timestamp) as day, COUNT(*) as count " +
           "FROM logs WHERE project_id = :projectId " +
           "GROUP BY DATE(timestamp) ORDER BY DATE(timestamp)")
    List<Object[]> getActivityByDayForProjectRaw(@Param("projectId") Long projectId);
    
    default List<LogStatsDTO> getLogStatsByDayForProject(Long projectId) {
        List<Object[]> results = getLogStatsByDayForProjectRaw(projectId);
        return results.stream()
            .map(row -> new LogStatsDTO(
                (Date) row[0],
                (String) row[1],
                ((Number) row[2]).longValue()))
            .collect(Collectors.toList());
    }
    
    default List<ActivityStatsDTO> getActivityByDayForProject(Long projectId) {
        List<Object[]> results = getActivityByDayForProjectRaw(projectId);
        return results.stream()
            .map(row -> new ActivityStatsDTO(
                (Date) row[0],
                ((Number) row[1]).longValue()))
            .collect(Collectors.toList());
    }

    @Query("SELECT COUNT(l) FROM Log l WHERE l.projectId = :projectId AND l.type = com.windlogs.tickets.enums.LogType.ERROR")
    long countAllErrorsByProject(@Param("projectId") Long projectId);
}