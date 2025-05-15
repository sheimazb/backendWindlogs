package com.windlogs.tickets.service;

import com.windlogs.tickets.dto.SolutionDTO;
import com.windlogs.tickets.dto.UserResponseDTO;
import com.windlogs.tickets.entity.Solution;
import com.windlogs.tickets.entity.Ticket;
import com.windlogs.tickets.enums.SolutionStatus;
import com.windlogs.tickets.exception.UnauthorizedException;
import com.windlogs.tickets.kafka.NotificationProducer;
import com.windlogs.tickets.mapper.SolutionMapper;
import com.windlogs.tickets.repository.SolutionRepository;
import com.windlogs.tickets.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SolutionService {
    private static final Logger logger = LoggerFactory.getLogger(SolutionService.class);
    private final SolutionRepository solutionRepository;
    private final TicketRepository ticketRepository;
    private final SolutionMapper solutionMapper;
    private final AuthService authService;
    private final NotificationProducer notificationProducer;

    public SolutionService(SolutionRepository solutionRepository, TicketRepository ticketRepository, 
                          SolutionMapper solutionMapper, AuthService authService, 
                          NotificationProducer notificationProducer) {
        this.solutionRepository = solutionRepository;
        this.ticketRepository = ticketRepository;
        this.solutionMapper = solutionMapper;
        this.authService = authService;
        this.notificationProducer = notificationProducer;
    }

    /**
     * Create a solution for a ticket
     * @param solutionDTO The solution to create
     * @param userId The ID of the user creating the solution
     * @param userEmail The email of the user creating the solution
     * @param tenant The tenant of the user
     * @return The created solution
     */
    @Transactional
    public SolutionDTO createSolution(SolutionDTO solutionDTO, Long userId, String userEmail, String tenant) {
        logger.info("Creating solution for ticket ID: {}, by user: {}", solutionDTO.getTicketId(), userEmail);
        
        // Validate ticket ID
        if (solutionDTO.getTicketId() == null) {
            logger.error("No ticket ID provided for solution");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ticket ID must be specified when creating a solution");
        }
        
        // Get the ticket
        Ticket ticket = ticketRepository.findByIdAndTenant(solutionDTO.getTicketId(), tenant)
                .orElseThrow(() -> {
                    logger.error("Ticket not found or not in user's tenant: {}", solutionDTO.getTicketId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found");
                });
        
        // Check if the user is assigned to the ticket
        if (!userId.equals(ticket.getAssignedToUserId())) {
            logger.error("User {} is not assigned to ticket {}", userEmail, ticket.getId());
            throw new UnauthorizedException("Only the assigned user can create a solution for this ticket");
        }
        
        // Check if a solution already exists for this ticket
        Optional<Solution> existingSolution = solutionRepository.findByTicketId(ticket.getId());
        if (existingSolution.isPresent()) {
            logger.error("Solution already exists for ticket {}", ticket.getId());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A solution already exists for this ticket");
        }
        
        // Map DTO to entity
        Solution solution = solutionMapper.toEntity(solutionDTO);
        
        // Set additional fields
        solution.setAuthorUserId(userId);
        solution.setTenant(tenant);
        solution.setTicket(ticket);
        
        // Set initial status if not provided
        if (solution.getStatus() == null) {
            solution.setStatus(SolutionStatus.DRAFT);
        }
        
        // Save the solution
        Solution savedSolution = solutionRepository.save(solution);
        
        // Map back to DTO
        SolutionDTO resultDTO = solutionMapper.toDTO(savedSolution);
        resultDTO.setAuthorEmail(userEmail);
        
        logger.info("Solution created successfully with ID: {}, for ticket: {}", resultDTO.getId(), resultDTO.getTicketId());
        
        // Try to send notification, but don't fail if it doesn't work
        try {
            sendSolutionNotification(savedSolution, ticket, userId, userEmail, tenant);
            logger.info("Successfully sent notification for solution ID: {}", savedSolution.getId());
        } catch (Exception e) {
            // Log the error but don't fail the solution creation
            logger.error("Failed to send notification for solution ID: {}. Error: {}", 
                savedSolution.getId(), e.getMessage());
            logger.debug("Notification error details:", e);
            // Add more detailed error information 
            logger.error("Exception class: {}, Stack trace: {}", 
                e.getClass().getName(), e.getStackTrace().length > 0 ? e.getStackTrace()[0] : "No stack trace");
        }
        
        return resultDTO;
    }

    /**
     * Helper method to send solution notification
     */
    private void sendSolutionNotification(Solution solution, Ticket ticket, Long userId, String userEmail, String tenant) {
        // Get notification content
        String content = solution.getContent();
        if (content == null || content.isEmpty()) {
            content = "Solution added to ticket #" + ticket.getId();
        } else if (content.length() > 200) {
            content = content.substring(0, 200) + "...";
        }

        logger.info("NOTIFICATION DEBUG: Starting solution notification process");
        logger.info("NOTIFICATION DEBUG: Ticket creatorUserId={}, solutionAuthorId={}", 
                ticket.getCreatorUserId(), userId);
        logger.info("NOTIFICATION DEBUG: Ticket creator email={}", ticket.getUserEmail());

        // Only try to send notification if there's a creator and it's different from current user
        if (ticket.getCreatorUserId() != null && !ticket.getCreatorUserId().equals(userId)) {
            logger.info("NOTIFICATION DEBUG: Conditions met to send notification");
            
            // First try to get creator email from ticket's userEmail field directly
            String recipientEmail = ticket.getUserEmail();
            
            // If no email in ticket, try to get from auth service
            if (recipientEmail == null || recipientEmail.isEmpty()) {
                try {
                    // Get creator's email using authService
                    logger.info("NOTIFICATION DEBUG: Fetching creator info for user ID: {}", ticket.getCreatorUserId());
                    UserResponseDTO creator = authService.getUserById(ticket.getCreatorUserId(), "Bearer service-token");
                    
                    if (creator != null) {
                        logger.info("NOTIFICATION DEBUG: Creator info retrieved: {}", creator);
                        recipientEmail = creator.getEmail();
                    } else {
                        logger.warn("NOTIFICATION DEBUG: Creator info is null");
                    }
                } catch (Exception e) {
                    logger.error("NOTIFICATION DEBUG: Error getting creator info: {}", e.getMessage());
                    // Don't rethrow - we'll try other approaches
                }
            }
            
            // If we still don't have an email, log error and return
            if (recipientEmail == null || recipientEmail.isEmpty()) {
                logger.error("NOTIFICATION DEBUG: Could not determine recipient email, skipping notification");
                return;
            }
            
            try {
                logger.info("NOTIFICATION DEBUG: Sending solution notification to: {}", recipientEmail);
                
                // Log notification details
                logger.info("NOTIFICATION DEBUG: Solution Notification Details:");
                logger.info("  - SolutionId: {}", solution.getId());
                logger.info("  - Content: {}", content);
                logger.info("  - TicketId: {}", ticket.getId());
                logger.info("  - AuthorName: {}", userEmail);
                logger.info("  - Status: {}", solution.getStatus().toString());
                logger.info("  - SenderEmail: {}", userEmail);
                logger.info("  - RecipientEmail: {}", recipientEmail);
                logger.info("  - Tenant: {}", tenant);
                
                notificationProducer.sendSolutionNotification(
                    solution.getId(),
                    content,
                    ticket.getId(),
                    userEmail,  // Using email as author name
                    solution.getStatus().toString(),
                    userEmail,  // Sender email
                    recipientEmail,  // Recipient email
                    tenant
                );
                logger.info("NOTIFICATION DEBUG: Successfully called notificationProducer");
                logger.info("Successfully sent solution notification to creator: {}", recipientEmail);
            } catch (Exception e) {
                logger.error("NOTIFICATION DEBUG: Exception occurred: {}", e.getClass().getName());
                logger.error("NOTIFICATION DEBUG: Exception message: {}", e.getMessage());
                logger.error("Error getting creator information or sending notification: {}", e.getMessage());
                throw e; // Rethrow to be caught by the outer try-catch
            }
        } else {
            logger.info("NOTIFICATION DEBUG: Skipping notification: creatorUserId={}, authorUserId={}", 
                    ticket.getCreatorUserId(), userId);
            logger.info("Skipping notification: ticket creator is null or same as solution creator");
        }
    }

    /**
     * Get a solution by ID
     * @param id The solution ID
     * @param tenant The tenant of the user
     * @return The solution if found
     */
    public SolutionDTO getSolutionById(Long id, String tenant) {
        logger.info("Getting solution by ID: {}, tenant: {}", id, tenant);
        
        Solution solution = solutionRepository.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> {
                    logger.error("Solution not found or not in user's tenant: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Solution not found");
                });
        
        return solutionMapper.toDTO(solution);
    }

    /**
     * Get a solution by ticket ID
     * @param ticketId The ticket ID
     * @param tenant The tenant of the user
     * @return The solution if found
     */
    public SolutionDTO getSolutionByTicketId(Long ticketId, String tenant) {
        logger.info("Getting solution for ticket ID: {}, tenant: {}", ticketId, tenant);
        
        // First check if the ticket exists and belongs to the tenant
        Ticket ticket = ticketRepository.findByIdAndTenant(ticketId, tenant)
                .orElseThrow(() -> {
                    logger.error("Ticket not found or not in user's tenant: {}", ticketId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found");
                });
        
        // Then get the solution
        Solution solution = solutionRepository.findByTicketId(ticketId)
                .orElseThrow(() -> {
                    logger.error("Solution not found for ticket: {}", ticketId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Solution not found for this ticket");
                });
        
        return solutionMapper.toDTO(solution);
    }

    /**
     * Update a solution
     * @param id The solution ID
     * @param solutionDTO The updated solution data
     * @param userId The ID of the user updating the solution
     * @param tenant The tenant of the user
     * @return The updated solution
     */
    public SolutionDTO updateSolution(Long id, SolutionDTO solutionDTO, Long userId, String tenant) {
        logger.info("Updating solution with ID: {}, by user: {}", id, userId);
        
        // Get the existing solution
        Solution existingSolution = solutionRepository.findByIdAndTenant(id, tenant)
                .orElseThrow(() -> {
                    logger.error("Solution not found or not in user's tenant: {}", id);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Solution not found");
                });
        
        // Check if the user is the author of the solution
        if (!userId.equals(existingSolution.getAuthorUserId())) {
            logger.error("User {} is not the author of solution {}", userId, id);
            throw new UnauthorizedException("Only the author can update this solution");
        }
        
        // Map DTO to entity
        Solution solution = solutionMapper.toEntity(solutionDTO);
        solution.setId(id);
        solution.setTenant(tenant);
        solution.setAuthorUserId(userId);
        solution.setTicket(existingSolution.getTicket());
        
        // Save the updated solution
        Solution updatedSolution = solutionRepository.save(solution);
        
        return solutionMapper.toDTO(updatedSolution);
    }

    /**
     * Get solutions by author user ID
     * @param authorUserId The author user ID
     * @param tenant The tenant of the user
     * @return List of solutions by the author
     */
    public List<SolutionDTO> getSolutionsByAuthorUserId(Long authorUserId, String tenant) {
        logger.info("Getting solutions by author user ID: {}, tenant: {}", authorUserId, tenant);
        
        List<Solution> solutions = solutionRepository.findByAuthorUserId(authorUserId);
        
        // Filter by tenant
        solutions = solutions.stream()
                .filter(solution -> tenant.equals(solution.getTenant()))
                .collect(Collectors.toList());
        
        return solutions.stream()
                .map(solutionMapper::toDTO)
                .collect(Collectors.toList());
    }
}
