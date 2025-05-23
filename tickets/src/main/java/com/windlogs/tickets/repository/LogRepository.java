package com.windlogs.tickets.repository;

import com.windlogs.tickets.dto.ActivityStatsDTO;
import com.windlogs.tickets.dto.LogStatsDTO;
import com.windlogs.tickets.entity.Log;
import com.windlogs.tickets.enums.LogType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
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

    @Query(value = "SELECT COUNT(l) FROM Log l WHERE l.tenant = ?1 AND l.severity = 'HIGH'")
    long countCriticalErrorsByTenant(String tenant);

    @Query(value = "SELECT COUNT(DISTINCT l.projectId) FROM Log l WHERE l.tenant = ?1")
    long countProjectsByTenant(String tenant);

    @Query(value = "SELECT COUNT(DISTINCT l.project_id) FROM logs l " +
           "WHERE l.tenant = ?1 AND l.created_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY)", 
           nativeQuery = true)
    long countActiveProjectsByTenant(String tenant);

    @Query("SELECT new map(" +
           "CAST(l.createdAt as date) as date, " +
           "COUNT(l) as count) " +
           "FROM Log l " +
           "WHERE l.tenant = ?1 AND l.severity = 'HIGH' " +
           "GROUP BY CAST(l.createdAt as date) " +
           "ORDER BY date DESC")
    List<Map<String, Object>> getErrorStatsByTenant(String tenant);

    @Query("SELECT new map(" +
           "l.type as type, " +
           "COUNT(l) as count) " +
           "FROM Log l " +
           "WHERE l.tenant = ?1 " +
           "GROUP BY l.type")
    List<Map<String, Object>> getErrorTypeDistribution(String tenant);

    @Query("SELECT new map(" +
           "CAST(l.createdAt as date) as date, " +
           "l.type as type, " +
           "COUNT(l) as count) " +
           "FROM Log l " +
           "WHERE l.tenant = ?1 " +
           "GROUP BY CAST(l.createdAt as date), l.type " +
           "ORDER BY date DESC")
    List<Map<String, Object>> getErrorTrendsByTenant(String tenant);

    @Query("SELECT new map(" +
           "l.projectId as projectId, " +
           "COUNT(l) as errorCount, " +
           "MAX(l.createdAt) as lastError) " +
           "FROM Log l " +
           "WHERE l.tenant = ?1 " +
           "GROUP BY l.projectId")
    List<Map<String, Object>> getProjectHealthStats(String tenant);

    @Query("SELECT new map(" +
           "CAST(l.createdAt as date) as date, " +
           "COUNT(l) as count) " +
           "FROM Log l " +
           "WHERE l.tenant = ?1 " +
           "GROUP BY CAST(l.createdAt as date) " +
           "ORDER BY date DESC")
    List<Map<String, Object>> getActivityStatsByTenant(String tenant);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.assignedToUserId = ?1 AND t.tenant = ?2 AND t.status = 'RESOLVED'")
    long countResolvedErrorsByDeveloper(Long userId, String tenant);

    @Query(value = "SELECT COUNT(*) FROM logs l " +
           "WHERE l.created_by_user_id = ?1 AND l.tenant = ?2 AND l.type = 'ERROR'",
           nativeQuery = true)
    long countErrorsFoundByTester(Long userId, String tenant);
}