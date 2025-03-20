package com.windlogs.tickets.service;

import com.windlogs.tickets.dto.SolutionDTO;
import com.windlogs.tickets.dto.TicketDTO;
import com.windlogs.tickets.dto.UserResponseDTO;
import com.windlogs.tickets.entity.Log;
import com.windlogs.tickets.entity.Ticket;
import com.windlogs.tickets.exception.UnauthorizedException;
import com.windlogs.tickets.kafka.TicketP;
import com.windlogs.tickets.kafka.TicketProducer;
import com.windlogs.tickets.mapper.TicketMapper;
import com.windlogs.tickets.repository.SolutionRepository;
import com.windlogs.tickets.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TicketService {
    private static final Logger logger = LoggerFactory.getLogger(TicketService.class);
    private final TicketRepository ticketRepository;
    private final TicketMapper ticketMapper;
    private final AuthService authService;
    private final LogService logService;
    private final SolutionRepository solutionRepository;
    private final TicketProducer ticketProducer;

    // Create a new ticket
    public TicketDTO createTicket(TicketDTO ticketDTO) {
        String incomingTenant = ticketDTO.getTenant();
        logger.info("Creating ticket with tenant: {}", incomingTenant);
        
        if (incomingTenant == null || incomingTenant.isEmpty()) {
            logger.error("Attempt to create ticket with null or empty tenant");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tenant must be specified when creating a ticket");
        }
        
        // Validate log ID if provided
        Log log = null;
        if (ticketDTO.getLogId() != null) {
            try {
                log = logService.getLogById(ticketDTO.getLogId());
                logger.info("Found log with ID: {}, type: {}, severity: {}", 
                        log.getId(), log.getType(), log.getSeverity());
            } catch (ResponseStatusException e) {
                logger.error("Invalid log ID: {}", ticketDTO.getLogId());
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid log ID");
            }
        } else {
            logger.warn("No log ID provided for ticket");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Log ID must be specified when creating a ticket");
        }
        
        Ticket ticket = ticketMapper.toEntity(ticketDTO);
        
        // Set the log reference
        ticket.setLog(log);
        
        // Double-check tenant is set correctly after mapping
        if (ticket.getTenant() == null || !incomingTenant.equals(ticket.getTenant())) {
            logger.warn("Tenant was not correctly mapped. Expected: {}, Got: {}. Setting it explicitly.", 
                    incomingTenant, ticket.getTenant());
            ticket.setTenant(incomingTenant);
        }
        
        // Set creator user ID
        ticket.setCreatorUserId(ticketDTO.getCreatorUserId());
        
        // Set user email
        ticket.setUserEmail(ticketDTO.getUserEmail());
        
        logger.info("Saving ticket with tenant: {}, creatorUserId: {}, userEmail: {}, logId: {}", 
                ticket.getTenant(), ticket.getCreatorUserId(), ticket.getUserEmail(), log.getId());
        
        Ticket savedTicket = ticketRepository.save(ticket);
        
        // Verify tenant was preserved after save
        if (!incomingTenant.equals(savedTicket.getTenant())) {
            logger.error("Tenant changed during save operation. Expected: {}, Got: {}", 
                    incomingTenant, savedTicket.getTenant());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Tenant changed during save operation");
        }
        
        TicketDTO resultDTO = ticketMapper.toDTO(savedTicket);
        
        // Final verification of tenant in DTO
        if (!incomingTenant.equals(resultDTO.getTenant())) {
            logger.error("Tenant changed during mapping to DTO. Expected: {}, Got: {}", 
                    incomingTenant, resultDTO.getTenant());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Tenant changed during mapping to DTO");
        }
        
        logger.info("Ticket created successfully with ID: {}, tenant: {}, logId: {}", 
                resultDTO.getId(), resultDTO.getTenant(), resultDTO.getLogId());

        ticketProducer.sendTicketP(
                new TicketP(
                        resultDTO.getStatus(),
                        resultDTO.getUserEmail(),
                        resultDTO.getTitle(),
                        resultDTO.getDescription(),
                        resultDTO.getTenant()
                )
        );
        
        return resultDTO;
    }

    // Get all tickets for a specific tenant
    public List<TicketDTO> getTicketsByTenant(String tenant) {
        return ticketRepository.findByTenant(tenant).stream()
                .map(ticketMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Get ticket by ID and tenant
    public TicketDTO getTicketByIdAndTenant(Long id, String tenant) {
        Optional<Ticket> ticket = ticketRepository.findByIdAndTenant(id, tenant);
        return ticket.map(ticketMapper::toDTO).orElse(null);
    }

    // Update ticket with tenant validation
    public TicketDTO updateTicketWithTenantValidation(Long id, TicketDTO ticketDTO, String tenant) {
        // Check if the ticket exists and belongs to the tenant
        Optional<Ticket> existingTicket = ticketRepository.findByIdAndTenant(id, tenant);
        
        if (existingTicket.isPresent()) {
            Ticket ticket = ticketMapper.toEntity(ticketDTO);
            ticket.setId(id);
            // Ensure tenant is not changed
            ticket.setTenant(tenant);
            // Preserve creator user ID
            ticket.setCreatorUserId(existingTicket.get().getCreatorUserId());
            // Preserve log reference
            ticket.setLog(existingTicket.get().getLog());
            
            // Ensure title and description are set
            if (ticket.getTitle() == null || ticket.getTitle().isEmpty()) {
                ticket.setTitle(existingTicket.get().getTitle());
            }
            
            if (ticket.getDescription() == null || ticket.getDescription().isEmpty()) {
                ticket.setDescription(existingTicket.get().getDescription());
            }
            
            Ticket updatedTicket = ticketRepository.save(ticket);
            TicketDTO updatedTicketDTO = ticketMapper.toDTO(updatedTicket);
            
            // Send Kafka message for the update
            ticketProducer.sendTicketP(
                new TicketP(
                    updatedTicketDTO.getStatus(),
                    updatedTicketDTO.getUserEmail(),
                    updatedTicketDTO.getTitle(),
                    updatedTicketDTO.getDescription(),
                    updatedTicketDTO.getTenant()
                )
            );
            
            return updatedTicketDTO;
        }
        
        return null;
    }

    // Delete ticket with tenant validation
    public boolean deleteTicketWithTenantValidation(Long id, String tenant) {
        Optional<Ticket> ticket = ticketRepository.findByIdAndTenant(id, tenant);
        
        if (ticket.isPresent()) {
            ticketRepository.deleteById(id);
            return true;
        }
        
        return false;
    }

    // Assign ticket to a user with tenant validation
    public TicketDTO assignTicket(Long ticketId, Long assignedToUserId, String managerTenant, String authorizationHeader) {
        // Check if the ticket exists and belongs to the manager's tenant
        Optional<Ticket> optionalTicket = ticketRepository.findByIdAndTenant(ticketId, managerTenant);
        
        if (optionalTicket.isEmpty()) {
            logger.error("Ticket not found or not in manager's tenant: {}", ticketId);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found");
        }
        
        Ticket ticket = optionalTicket.get();
        
        // Get the user to be assigned
        UserResponseDTO assignedUser;
        try {
            // Pass the authorization header to get the user
            assignedUser = authService.getUserById(assignedToUserId, authorizationHeader);
        } catch (Exception e) {
            logger.error("Error getting user with ID {}: {}", assignedToUserId, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid user ID");
        }
        
        // Check if the assigned user has the same tenant as the manager
        if (!managerTenant.equals(assignedUser.getTenant())) {
            logger.error("Cannot assign ticket to user from different tenant. Manager tenant: {}, User tenant: {}", 
                    managerTenant, assignedUser.getTenant());
            throw new UnauthorizedException("Cannot assign ticket to user from different tenant");
        }
        
        // Check if the assigned user is a developer or tester
        if (!"DEVELOPER".equalsIgnoreCase(assignedUser.getRole()) && 
            !"TESTER".equalsIgnoreCase(assignedUser.getRole())) {
            logger.error("Cannot assign ticket to non-developer/tester user: {}", assignedUser.getRole());
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, 
                "Tickets can only be assigned to users with DEVELOPER or TESTER role"
            );
        }
        
        // Assign the ticket
        ticket.setAssignedToUserId(assignedToUserId);
        Ticket updatedTicket = ticketRepository.save(ticket);
        
        return ticketMapper.toDTO(updatedTicket);
    }

    /**
     * Check if a ticket has a solution
     * @param ticketId The ticket ID
     * @return true if the ticket has a solution, false otherwise
     */
    public boolean hasTicketSolution(Long ticketId) {
        logger.info("Checking if ticket ID: {} has a solution", ticketId);
        return solutionRepository.findByTicketId(ticketId).isPresent();
    }

    /**
     * Get a ticket with solution information
     * @param id The ticket ID
     * @param tenant The tenant
     * @return The ticket with solution information
     */
    public TicketDTO getTicketWithSolutionInfo(Long id, String tenant) {
        logger.info("Getting ticket with solution info for ID: {}, tenant: {}", id, tenant);
        
        TicketDTO ticketDTO = getTicketByIdAndTenant(id, tenant);
        
        // Add solution information
        boolean hasSolution = hasTicketSolution(id);
        ticketDTO.setHasSolution(hasSolution);
        
        return ticketDTO;
    }


}
