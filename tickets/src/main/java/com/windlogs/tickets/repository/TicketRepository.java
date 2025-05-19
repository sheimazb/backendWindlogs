package com.windlogs.tickets.repository;

import com.windlogs.tickets.entity.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

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
    Long countByAssignedToUserIdAndTenant(Long assignedToUserId, String tenant);

}
