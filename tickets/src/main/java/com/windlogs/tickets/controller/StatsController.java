package com.windlogs.tickets.controller;

import com.windlogs.tickets.dto.ActivityStatsDTO;
import com.windlogs.tickets.dto.LogStatsDTO;
import com.windlogs.tickets.dto.UserResponseDTO;
import com.windlogs.tickets.enums.Status;
import com.windlogs.tickets.repository.LogRepository;
import com.windlogs.tickets.repository.SolutionRepository;
import com.windlogs.tickets.repository.TicketRepository;
import com.windlogs.tickets.service.AuthService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/statistics")
@AllArgsConstructor
public class StatsController {
    private static final Logger logger = LoggerFactory.getLogger(StatsController.class);
    private final LogRepository logRepository;
    private final TicketRepository ticketRepository;
    private final SolutionRepository solutionRepository;
    private final AuthService authService;

    /**
     * Get dashboard statistics based on user role
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats(
            @RequestHeader("Authorization") String authorization) {
        UserResponseDTO user = authService.getAuthenticatedUser(authorization);
        Map<String, Object> stats = new HashMap<>();
        String tenant = user.getTenant();

        switch (user.getRole().toUpperCase()) {
            case "MANAGER":
                // Manager Statistics
                stats.putAll(getManagerStats(tenant));
                break;

            case "DEVELOPER":
                // Developer Statistics
                stats.putAll(getDeveloperStats(user.getId(), tenant));
                break;

            case "TESTER":
                // Tester Statistics
                stats.putAll(getTesterStats(user.getId(), tenant));
                break;

            case "PARTNER":
                // Partner Statistics
                stats.putAll(getPartnerStats(tenant));
                break;
        }

        return ResponseEntity.ok(stats);
    }

    private Map<String, Object> getManagerStats(String tenant) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minus(7, ChronoUnit.DAYS);

        // Team Performance
        stats.put("totalTickets", ticketRepository.countByTenant(tenant));
        stats.put("openTickets", ticketRepository.countByTenantAndStatus(tenant, Status.TO_DO));
        stats.put("inProgressTickets", ticketRepository.countByTenantAndStatus(tenant, Status.IN_PROGRESS));
        stats.put("resolvedTickets", ticketRepository.countByTenantAndStatus(tenant, Status.RESOLVED));
        stats.put("doneTickets", ticketRepository.countByTenantAndStatus(tenant, Status.DONE));

        // Weekly Statistics
        stats.put("newTicketsThisWeek", ticketRepository.countByTenantAndCreatedAtBetween(tenant, weekAgo, now));
        stats.put("resolvedTicketsThisWeek", ticketRepository.countByTenantAndStatusAndUpdatedAtBetween(
                tenant, Status.RESOLVED, weekAgo, now));

        // Error Tracking
        stats.put("criticalErrors", logRepository.countCriticalErrorsByTenant(tenant));
        stats.put("errorsByDay", logRepository.getErrorStatsByTenant(tenant));

        // Team Activity
        stats.put("teamActivity", logRepository.getActivityStatsByTenant(tenant));
        
        return stats;
    }

    private Map<String, Object> getDeveloperStats(Long userId, String tenant) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minus(7, ChronoUnit.DAYS);

        // Personal Performance
        stats.put("assignedTickets", ticketRepository.countByAssignedToUserIdAndTenant(userId, tenant));
        stats.put("resolvedTickets", ticketRepository.countByAssignedToUserIdAndTenantAndStatus(
                userId, tenant, Status.RESOLVED));
        stats.put("inProgressTickets", ticketRepository.countByAssignedToUserIdAndTenantAndStatus(
                userId, tenant, Status.IN_PROGRESS));

        // Weekly Progress
        stats.put("ticketsResolvedThisWeek", ticketRepository.countByAssignedToUserIdAndTenantAndStatusAndUpdatedAtBetween(
                userId, tenant, Status.RESOLVED, weekAgo, now));

        // Solutions
        stats.put("totalSolutions", solutionRepository.countByAuthorUserIdAndTenant(userId, tenant));
        stats.put("recentSolutions", solutionRepository.findRecentByAuthorUserIdAndTenant(userId, tenant));

        // Error Resolution
        stats.put("errorsResolved", logRepository.countResolvedErrorsByDeveloper(userId, tenant));
        
        return stats;
    }

    private Map<String, Object> getTesterStats(Long userId, String tenant) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minus(7, ChronoUnit.DAYS);

        // Testing Performance
        stats.put("ticketsToTest", ticketRepository.countByTenantAndStatus(tenant, Status.MERGED_TO_TEST));
        stats.put("ticketsTested", ticketRepository.countByTenantAndStatus(tenant, Status.DONE));
        stats.put("ticketsTestedThisWeek", ticketRepository.countByTenantAndStatusAndUpdatedAtBetween(
                tenant, Status.DONE, weekAgo, now));

        // Error Discovery
        stats.put("errorsFound", logRepository.countErrorsFoundByTester(userId, tenant));
        stats.put("errorsByType", logRepository.getErrorTypeDistribution(tenant));

        // Testing Progress
        stats.put("testingProgress", ticketRepository.getTestingProgressStats(tenant));
        
        return stats;
    }

    private Map<String, Object> getPartnerStats(String tenant) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minus(7, ChronoUnit.DAYS);
        LocalDateTime monthAgo = now.minus(30, ChronoUnit.DAYS);

        // Overall Project Health
        stats.put("totalProjects", logRepository.countProjectsByTenant(tenant));
        stats.put("activeProjects", logRepository.countActiveProjectsByTenant(tenant));
        stats.put("totalTickets", ticketRepository.countByTenant(tenant));
        stats.put("resolvedTickets", ticketRepository.countByTenantAndStatus(tenant, Status.DONE));

        // Project Performance Metrics
        Map<String, Object> projectPerformance = new HashMap<>();
        projectPerformance.put("completionRate", 
            ticketRepository.countByTenant(tenant) > 0 ?
            (double) ticketRepository.countByTenantAndStatus(tenant, Status.DONE) / 
            ticketRepository.countByTenant(tenant) * 100 : 0);
        projectPerformance.put("inProgressRate",
            ticketRepository.countByTenant(tenant) > 0 ?
            (double) ticketRepository.countByTenantAndStatus(tenant, Status.IN_PROGRESS) /
            ticketRepository.countByTenant(tenant) * 100 : 0);
        stats.put("projectPerformance", projectPerformance);

        // Quality Metrics
        Map<String, Object> qualityMetrics = new HashMap<>();
        qualityMetrics.put("criticalErrors", logRepository.countCriticalErrorsByTenant(tenant));
        qualityMetrics.put("errorTrends", logRepository.getErrorTrendsByTenant(tenant));
        qualityMetrics.put("errorsByType", logRepository.getErrorTypeDistribution(tenant));
        stats.put("qualityMetrics", qualityMetrics);

        // Team Performance
        Map<String, Object> teamPerformance = new HashMap<>();
        teamPerformance.put("overallStats", ticketRepository.getTeamPerformanceStats(tenant));
        teamPerformance.put("testingProgress", ticketRepository.getTestingProgressStats(tenant));
        stats.put("teamPerformance", teamPerformance);

        // Project Health Indicators
        Map<String, Object> projectHealth = new HashMap<>();
        projectHealth.put("projectHealthStats", logRepository.getProjectHealthStats(tenant));
        projectHealth.put("activityTrends", logRepository.getActivityStatsByTenant(tenant));
        stats.put("projectHealth", projectHealth);

        // Time-based Analysis
        Map<String, Object> timeAnalysis = new HashMap<>();
        timeAnalysis.put("newIssuesThisWeek", 
            ticketRepository.countByTenantAndCreatedAtBetween(tenant, weekAgo, now));
        timeAnalysis.put("resolvedIssuesThisWeek", 
            ticketRepository.countByTenantAndStatusAndUpdatedAtBetween(tenant, Status.DONE, weekAgo, now));
        timeAnalysis.put("newIssuesThisMonth", 
            ticketRepository.countByTenantAndCreatedAtBetween(tenant, monthAgo, now));
        timeAnalysis.put("resolvedIssuesThisMonth", 
            ticketRepository.countByTenantAndStatusAndUpdatedAtBetween(tenant, Status.DONE, monthAgo, now));
        stats.put("timeAnalysis", timeAnalysis);
        
        return stats;
    }

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
