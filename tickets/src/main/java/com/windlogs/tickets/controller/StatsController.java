package com.windlogs.tickets.controller;

import com.windlogs.tickets.dto.ActivityStatsDTO;
import com.windlogs.tickets.dto.LogStatsDTO;
import com.windlogs.tickets.repository.LogRepository;
import com.windlogs.tickets.repository.SolutionRepository;
import com.windlogs.tickets.repository.TicketRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/statistics")
@AllArgsConstructor
public class StatsController {

    private final LogRepository logRepository;
    private final TicketRepository ticketRepository;
    private final SolutionRepository solutionRepository;

    @GetMapping("/errors-by-day/project/{projectId}")
    public List<LogStatsDTO> getErrorsByDayForProject(@PathVariable Long projectId) {
        return logRepository.getLogStatsByDayForProject(projectId);
    }

    @GetMapping("/activity-by-day/project/{projectId}")
    public List<ActivityStatsDTO> getActivitiesByDayForProject(@PathVariable Long projectId) {
        return logRepository.getActivityByDayForProject(projectId);
    }

    @GetMapping("/total-errors/project/{projectId}")
    public ResponseEntity<Map<String, Object>> getTotalErrorsForProject(@PathVariable Long projectId) {
        long errorCount = logRepository.countAllErrorsByProject(projectId);
        return ResponseEntity.ok(Map.of(
            "projectId", projectId,
            "totalErrors", errorCount
        ));
    }
    
    /**
     * Get the count of tickets assigned to a specific user within a tenant
     * @param userId The ID of the user
     * @param tenant The tenant identifier
     * @return Response with user ID, tenant, and ticket count
     */
    @GetMapping("/tickets-count/user/{userId}/tenant/{tenant}")
    public ResponseEntity<Map<String, Object>> getTicketCountByUser(
            @PathVariable Long userId,
            @PathVariable String tenant) {
        
        Long ticketCount = ticketRepository.countByAssignedToUserIdAndTenant(userId, tenant);
        
        return ResponseEntity.ok(Map.of(
            "userId", userId,
            "tenant", tenant,
            "assignedTicketsCount", ticketCount
        ));
    }
    
    /**
     * Get the count of solutions created by a specific developer within a tenant
     * @param developerId The ID of the developer
     * @param tenant The tenant identifier
     * @return Response with developer ID, tenant, and solution count
     */
    @GetMapping("/solutions-count/developer/{developerId}/tenant/{tenant}")
    public ResponseEntity<Map<String, Object>> getSolutionCountByDeveloper(
            @PathVariable Long developerId,
            @PathVariable String tenant) {
        
        Long solutionCount = solutionRepository.countByAuthorUserIdAndTenant(developerId, tenant);
        
        return ResponseEntity.ok(Map.of(
            "developerId", developerId,
            "tenant", tenant,
            "createdSolutionsCount", solutionCount
        ));
    }
}
