package com.windlogs.tickets.repository;

import com.windlogs.tickets.entity.Solution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolutionRepository extends JpaRepository<Solution, Long> {
    
    /**
     * Find a solution by its ID and tenant
     * @param id The solution ID
     * @param tenant The tenant
     * @return The solution if found
     */
    Optional<Solution> findByIdAndTenant(Long id, String tenant);
    
    /**
     * Find a solution by ticket ID
     * @param ticketId The ticket ID
     * @return The solution if found
     */
    Optional<Solution> findByTicketId(Long ticketId);
    
    /**
     * Find solutions by author user ID
     * @param authorUserId The author user ID
     * @return List of solutions by the author
     */
    List<Solution> findByAuthorUserId(Long authorUserId);
    
    /**
     * Find solutions by tenant
     * @param tenant The tenant
     * @return List of solutions for the tenant
     */
    List<Solution> findByTenant(String tenant);
    
    /**
     * Count solutions created by a specific developer within a tenant
     * @param authorUserId The ID of the developer who authored the solutions
     * @param tenant The tenant identifier
     * @return Number of solutions created by the developer
     */
    Long countByAuthorUserIdAndTenant(Long authorUserId, String tenant);
    
    @Query("SELECT s FROM Solution s WHERE s.authorUserId = ?1 AND s.tenant = ?2 ORDER BY s.createdAt DESC LIMIT 5")
    List<Solution> findRecentByAuthorUserIdAndTenant(Long authorUserId, String tenant);
} 