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
}
