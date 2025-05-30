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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/statistics")
@AllArgsConstructor
public class StatsController {
    private static final Logger logger = LoggerFactory.getLogger(StatsController.class);
    private final LogRepository logRepository;
    private final TicketRepository ticketRepository;
    private final SolutionRepository solutionRepository;
    private final AuthService authService;

    @GetMapping("/dashboard/developer")
    public ResponseEntity<Map<String, Object>> getDashboardDeveloperStats(
            @RequestHeader("Authorization") String authorization) {
        try {
            UserResponseDTO user = authService.getAuthenticatedUser(authorization);
            Map<String, Object> stats = new HashMap<>();
            String tenant = user.getTenant();

            // Log for debugging
            logger.info("Loading developer stats for user: {} with tenant: {}", user.getId(), tenant);

            stats.putAll(getDeveloperStats(user.getId(), tenant));

            // Log the stats being returned
            logger.info("Developer stats loaded successfully: {}", stats);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error loading developer statistics", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to load developer statistics",
                    "message", e.getMessage()
            ));
        }
    }

    /**
     * Get dashboard statistics based on user role
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats(
            @RequestHeader("Authorization") String authorization) {
        try {
            UserResponseDTO user = authService.getAuthenticatedUser(authorization);
            Map<String, Object> stats = new HashMap<>();
            String tenant = user.getTenant();

            logger.info("Loading dashboard stats for role: {} and tenant: {}", user.getRole(), tenant);

            switch (user.getRole().toUpperCase()) {
                case "MANAGER":
                    stats.putAll(getManagerStats(tenant));
                    break;
                case "DEVELOPER":
                    // Add the missing DEVELOPER case
                    stats.putAll(getDeveloperStats(user.getId(), tenant));
                    break;
                case "TESTER":
                    stats.putAll(getTesterStats(user.getId(), tenant));
                    break;
                case "PARTNER":
                    stats.putAll(getPartnerStats(tenant));
                    break;
                default:
                    logger.warn("Unknown role: {}", user.getRole());
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Unknown user role",
                            "role", user.getRole()
                    ));
            }

            logger.info("Dashboard stats loaded successfully for role: {}", user.getRole());
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            logger.error("Error loading dashboard statistics", e);
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Failed to load dashboard statistics",
                    "message", e.getMessage()
            ));
        }
    }

    private Map<String, Object> getManagerStats(String tenant) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minus(7, ChronoUnit.DAYS);

        try {
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

            logger.info("Manager stats loaded successfully for tenant: {}", tenant);
        } catch (Exception e) {
            logger.error("Error loading manager stats for tenant: {}", tenant, e);
            throw e;
        }

        return stats;
    }

    private Map<String, Object> getDeveloperStats(Long userId, String tenant) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minus(7, ChronoUnit.DAYS);

        try {
            logger.info("Loading developer stats for userId: {} and tenant: {}", userId, tenant);

            // Personal Performance
            Long assignedTickets = ticketRepository.countByAssignedToUserIdAndTenant(userId, tenant);
            Long resolvedTickets = ticketRepository.countByAssignedToUserIdAndTenantAndStatus(
                    userId, tenant, Status.RESOLVED);
            Long inProgressTickets = ticketRepository.countByAssignedToUserIdAndTenantAndStatus(
                    userId, tenant, Status.IN_PROGRESS);

            stats.put("assignedTickets", assignedTickets != null ? assignedTickets : 0L);
            stats.put("resolvedTickets", resolvedTickets != null ? resolvedTickets : 0L);
            stats.put("inProgressTickets", inProgressTickets != null ? inProgressTickets : 0L);

            // Weekly Progress
            Long ticketsResolvedThisWeek = ticketRepository.countByAssignedToUserIdAndTenantAndStatusAndUpdatedAtBetween(
                    userId, tenant, Status.RESOLVED, weekAgo, now);
            stats.put("ticketsResolvedThisWeek", ticketsResolvedThisWeek != null ? ticketsResolvedThisWeek : 0L);

            // Solutions
            Long totalSolutions = solutionRepository.countByAuthorUserIdAndTenant(userId, tenant);
            stats.put("totalSolutions", totalSolutions != null ? totalSolutions : 0L);

            // Recent Solutions - handle potential null
            try {
                Object recentSolutions = solutionRepository.findRecentByAuthorUserIdAndTenant(userId, tenant);
                stats.put("recentSolutions", recentSolutions != null ? recentSolutions : List.of());
            } catch (Exception e) {
                logger.warn("Could not load recent solutions for user {} and tenant {}: {}", userId, tenant, e.getMessage());
                stats.put("recentSolutions", List.of());
            }

            // Error Resolution
            try {
                Long errorsResolved = logRepository.countResolvedErrorsByDeveloper(userId, tenant);
                stats.put("errorsResolved", errorsResolved != null ? errorsResolved : 0L);
            } catch (Exception e) {
                logger.warn("Could not load errors resolved for user {} and tenant {}: {}", userId, tenant, e.getMessage());
                stats.put("errorsResolved", 0L);
            }

            logger.info("Developer stats loaded successfully: {}", stats);
        } catch (Exception e) {
            logger.error("Error loading developer stats for userId: {} and tenant: {}", userId, tenant, e);
            throw e;
        }

        return stats;
    }

    private Map<String, Object> getTesterStats(Long userId, String tenant) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minus(7, ChronoUnit.DAYS);

        try {
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

            logger.info("Tester stats loaded successfully for userId: {} and tenant: {}", userId, tenant);
        } catch (Exception e) {
            logger.error("Error loading tester stats for userId: {} and tenant: {}", userId, tenant, e);
            throw e;
        }

        return stats;
    }

    private Map<String, Object> getPartnerStats(String tenant) {
        Map<String, Object> stats = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime weekAgo = now.minus(7, ChronoUnit.DAYS);
        LocalDateTime monthAgo = now.minus(30, ChronoUnit.DAYS);

        try {
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

            logger.info("Partner stats loaded successfully for tenant: {}", tenant);
        } catch (Exception e) {
            logger.error("Error loading partner stats for tenant: {}", tenant, e);
            throw e;
        }

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

    /**
     * Get detailed log statistics for a specific project
     */
    @GetMapping("/logs/project/{projectId}/details")
    public ResponseEntity<Map<String, Object>> getProjectLogStatistics(@PathVariable Long projectId) {
        Map<String, Object> statistics = new HashMap<>();

        // Error statistics
        statistics.put("totalErrors", logRepository.countAllErrorsByProject(projectId));
        statistics.put("criticalErrors", logRepository.countCriticalErrorsByProject(projectId));
        statistics.put("errorsByDay", logRepository.getLogStatsByDayForProject(projectId));
        statistics.put("errorsByType", logRepository.getErrorTypeDistributionByProject(projectId));

        // Time-based statistics
        Map<String, Object> timeBasedStats = new HashMap<>();
        timeBasedStats.put("totalErrors", logRepository.countAllErrorsByProject(projectId));
        statistics.put("timeBasedStatistics", timeBasedStats);

        // Activity statistics
        statistics.put("activityByDay", logRepository.getActivityByDayForProject(projectId));

        return ResponseEntity.ok(statistics);
    }

    /**
     * Get ticket statistics for a specific developer
     * @param developerId The ID of the developer
     * @return Map containing ticket counts by status
     */
    @GetMapping("/tickets-count/developer/{developerId}")
    public ResponseEntity<Map<String, Object>> getTicketStatsForDeveloper(
            @PathVariable Long developerId) {
        
        Map<String, Object> stats = new HashMap<>();
        
        // Get total tickets assigned to the developer
        Long totalAssigned = ticketRepository.countByAssignedToUserId(developerId);
        stats.put("totalAssignedTickets", totalAssigned);
        
        // Get tickets by status
        stats.put("todoTickets", ticketRepository.countByAssignedToUserIdAndStatus(developerId, Status.TO_DO));
        stats.put("inProgressTickets", ticketRepository.countByAssignedToUserIdAndStatus(developerId, Status.IN_PROGRESS));
        stats.put("resolvedTickets", ticketRepository.countByAssignedToUserIdAndStatus(developerId, Status.RESOLVED));
        stats.put("doneTickets", ticketRepository.countByAssignedToUserIdAndStatus(developerId, Status.DONE));
        
        return ResponseEntity.ok(stats);
    }
}