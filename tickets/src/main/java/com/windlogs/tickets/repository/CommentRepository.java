package com.windlogs.tickets.repository;

import com.windlogs.tickets.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    /**
     * Find comments by ticket ID
     * @param ticketId The ticket ID
     * @return List of comments for the ticket
     */
    List<Comment> findByTicketId(Long ticketId);
    
    /**
     * Find comments by author user ID
     * @param authorUserId The author user ID
     * @return List of comments by the author
     */
    List<Comment> findByAuthorUserId(Long authorUserId);
    
    /**
     * Find a comment by ID and check if it belongs to a ticket in the given tenant
     * This is a security check to prevent users from accessing comments from different tenants
     * @param id The comment ID
     * @param tenant The tenant to check against
     * @return The comment if found and belongs to the tenant
     */
    @Query("SELECT c FROM Comment c JOIN c.ticket t WHERE c.id = :id AND t.tenant = :tenant")
    Optional<Comment> findByIdAndTicketTenant(@Param("id") Long id, @Param("tenant") String tenant);
    
    /**
     * Find comments by ticket ID and tenant
     * This is a security check to prevent users from accessing comments from different tenants
     * @param ticketId The ticket ID
     * @param tenant The tenant to check against
     * @return List of comments for the ticket if it belongs to the tenant
     */
    @Query("SELECT c FROM Comment c JOIN c.ticket t WHERE t.id = :ticketId AND t.tenant = :tenant")
    List<Comment> findByTicketIdAndTenant(@Param("ticketId") Long ticketId, @Param("tenant") String tenant);
} 