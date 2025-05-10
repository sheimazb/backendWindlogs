package com.windlogs.tickets.service;

import com.windlogs.tickets.dto.TicketDTO;
import com.windlogs.tickets.dto.UserResponseDTO;
import com.windlogs.tickets.entity.Log;
import com.windlogs.tickets.entity.Ticket;
import com.windlogs.tickets.enums.Status;
import com.windlogs.tickets.exception.UnauthorizedException;
import com.windlogs.tickets.mapper.TicketMapper;
import com.windlogs.tickets.repository.SolutionRepository;
import com.windlogs.tickets.repository.TicketRepository;
import jakarta.ws.rs.NotFoundException;
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
        
        // Set initial status to TO_DO
        ticket.setStatus(Status.TO_DO);
        
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
        
        logger.info("Ticket created successfully with ID: {}, tenant: {}, logId: {}, status: {}", 
                resultDTO.getId(), resultDTO.getTenant(), resultDTO.getLogId(), resultDTO.getStatus());
        
        return resultDTO;
    }

    public List<TicketDTO> getTicketsByTenant(String tenant) {
        return ticketRepository.findByTenant(tenant).stream()
                .map(ticketMapper::toDTO)
                .collect(Collectors.toList());
    }

    public TicketDTO getTicketByIdAndTenant(Long id, String tenant) {
        Optional<Ticket> ticket = ticketRepository.findByIdAndTenant(id, tenant);
        return ticket.map(ticketMapper::toDTO).orElse(null);
    }

    public TicketDTO updateStatusTicket(Long ticketId, Status newStatus, String tenant) {
        logger.info("Request to update status of ticket ID {} for tenant '{}' to '{}'", ticketId, tenant, newStatus);

        // Find the ticket
        Ticket ticket = ticketRepository.findByIdAndTenant(ticketId, tenant)
                .orElseThrow(() -> {
                    logger.error("Ticket with ID {} not found for tenant '{}'", ticketId, tenant);
                    return new NotFoundException("Ticket not found");
                });

        Status currentStatus = ticket.getStatus();
        logger.info("Current ticket status: {}", currentStatus);

        // Validate the status transition follows the correct workflow
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            String errorMsg = String.format("Invalid status transition from %s to %s", currentStatus, newStatus);
            logger.error(errorMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errorMsg);
        }

        ticket.setStatus(newStatus);
        Ticket updatedTicket = ticketRepository.save(ticket);

        logger.info("Ticket ID {} status updated to '{}'", ticketId, newStatus);

        return ticketMapper.toDTO(updatedTicket);
    }

    /**
     * Validates if the status transition follows the correct workflow
     * @param currentStatus The current status of the ticket
     * @param newStatus The new status to transition to
     * @return true if the transition is valid, false otherwise
     */
    private boolean isValidStatusTransition(Status currentStatus, Status newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }

        switch (currentStatus) {
            case TO_DO:
                // From TO_DO, can only move to IN_PROGRESS
                return newStatus == Status.IN_PROGRESS;
            case IN_PROGRESS:
                // From IN_PROGRESS, can only move to RESOLVED
                return newStatus == Status.RESOLVED;
            case RESOLVED:
                // From RESOLVED, can only move to MERGED_TO_TEST
                return newStatus == Status.MERGED_TO_TEST;
            case MERGED_TO_TEST:
                // From MERGED_TO_TEST, can only move to DONE
                return newStatus == Status.DONE;
            case DONE:
                // Cannot change status once it's DONE
                return false;
            default:
                return false;
        }
    }

    public TicketDTO updateTicketWithTenantValidation(Long id, TicketDTO ticketDTO, String tenant) {
        Optional<Ticket> existingTicketOpt = ticketRepository.findByIdAndTenant(id, tenant);

        if (existingTicketOpt.isPresent()) {
            Ticket existingTicket = getTicket(ticketDTO, existingTicketOpt);

            Ticket updatedTicket = ticketRepository.save(existingTicket);
            TicketDTO updatedTicketDTO = ticketMapper.toDTO(updatedTicket);

            return updatedTicketDTO;
        }

        return null;
    }

    private static Ticket getTicket(TicketDTO ticketDTO, Optional<Ticket> existingTicketOpt) {
        Ticket existingTicket = existingTicketOpt.get();

        if (ticketDTO.getTitle() != null && !ticketDTO.getTitle().isEmpty()) {
            existingTicket.setTitle(ticketDTO.getTitle());
        }

        if (ticketDTO.getDescription() != null && !ticketDTO.getDescription().isEmpty()) {
            existingTicket.setDescription(ticketDTO.getDescription());
        }

        if (ticketDTO.getAssignedToUserId() != null) {
            existingTicket.setAssignedToUserId(ticketDTO.getAssignedToUserId());
        }

        if (ticketDTO.getStatus() != null) {
            existingTicket.setStatus(ticketDTO.getStatus());
        }

        if(ticketDTO.getPriority()!=null)
        {
            existingTicket.setPriority(ticketDTO.getPriority());
        }

        if (ticketDTO.getUserEmail() != null && !ticketDTO.getUserEmail().isEmpty()) {
            existingTicket.setUserEmail(ticketDTO.getUserEmail());
        }
        return existingTicket;
    }

    public boolean deleteTicketWithTenantValidation(Long id, String tenant) {
        Optional<Ticket> ticket = ticketRepository.findByIdAndTenant(id, tenant);
        
        if (ticket.isPresent()) {
            ticketRepository.deleteById(id);
            return true;
        }
        
        return false;
    }

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
        
        // Update status to TO_DO when assigned to a developer
        if ("DEVELOPER".equalsIgnoreCase(assignedUser.getRole())) {
            ticket.setStatus(Status.TO_DO);
            logger.info("Setting ticket status to TO_DO as it's assigned to a developer");
        }
        
        Ticket updatedTicket = ticketRepository.save(ticket);
        
        return ticketMapper.toDTO(updatedTicket);
    }

    public boolean hasTicketSolution(Long ticketId) {
        logger.info("Checking if ticket ID: {} has a solution", ticketId);
        return solutionRepository.findByTicketId(ticketId).isPresent();
    }
   
    public TicketDTO getTicketWithSolutionInfo(Long id, String tenant) {
        logger.info("Getting ticket with solution info for ID: {}, tenant: {}", id, tenant);
        
        TicketDTO ticketDTO = getTicketByIdAndTenant(id, tenant);
        
        // Add solution information
        boolean hasSolution = hasTicketSolution(id);
        ticketDTO.setHasSolution(hasSolution);
        
        return ticketDTO;
    }


}
