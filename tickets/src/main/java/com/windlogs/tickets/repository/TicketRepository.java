package com.windlogs.tickets.repository;

import com.windlogs.tickets.entity.Ticket;
import com.windlogs.tickets.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
    /**
     * Find all tickets for a specific tenant
     * @param tenant The tenant identifier
     * @return List of tickets for the tenant
     */
    List<Ticket> findByTenant(String tenant);
    
    /**
     * Find a ticket by ID and tenant
     * @param id The ticket ID
     * @param tenant The tenant identifier
     * @return Optional containing the ticket if found
     */
    Optional<Ticket> findByIdAndTenant(Long id, String tenant);
    
    /**
     * Count tickets assigned to a specific user within a tenant
     * @param assignedToUserId The ID of the user assigned to tickets
     * @param tenant The tenant identifier
     * @return Number of tickets assigned to the user
     */
    long countByAssignedToUserIdAndTenant(Long assignedToUserId, String tenant);
    long countByTenant(String tenant);
    long countByTenantAndStatus(String tenant, Status status);
    long countByAssignedToUserIdAndTenantAndStatus(Long userId, String tenant, Status status);
    long countByTenantAndCreatedAtBetween(String tenant, LocalDateTime start, LocalDateTime end);
    long countByTenantAndStatusAndUpdatedAtBetween(String tenant, Status status, LocalDateTime start, LocalDateTime end);
    long countByAssignedToUserIdAndTenantAndStatusAndUpdatedAtBetween(Long userId, String tenant, Status status, LocalDateTime start, LocalDateTime end);

    @Query("SELECT new map(" +
           "t.status as status, " +
           "COUNT(t) as count, " +
           "AVG(TIMESTAMPDIFF(HOUR, t.createdAt, t.updatedAt)) as avgResolutionTime) " +
           "FROM Ticket t " +
           "WHERE t.tenant = ?1 " +
           "GROUP BY t.status")
    List<Map<String, Object>> getTeamPerformanceStats(String tenant);

    @Query("SELECT new map(" +
           "t.status as status, " +
           "COUNT(t) as count) " +
           "FROM Ticket t " +
           "WHERE t.tenant = ?1 AND t.status IN ('MERGED_TO_TEST', 'DONE') " +
           "GROUP BY t.status")
    List<Map<String, Object>> getTestingProgressStats(String tenant);
}
