package com.windlogs.tickets.controller;

import com.windlogs.tickets.dto.SolutionDTO;
import com.windlogs.tickets.dto.TicketDTO;
import com.windlogs.tickets.dto.UserResponseDTO;
import com.windlogs.tickets.exception.UnauthorizedException;
import com.windlogs.tickets.service.AuthService;
import com.windlogs.tickets.service.SolutionService;
import com.windlogs.tickets.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {
    private static final Logger logger = LoggerFactory.getLogger(TicketController.class);
    private final TicketService ticketService;
    private final AuthService authService;
    private final SolutionService solutionService;

    public TicketController(TicketService ticketService, AuthService authService, SolutionService solutionService) {
        this.ticketService = ticketService;
        this.authService = authService;
        this.solutionService = solutionService;
    }

    // Create a ticket - only managers can create tickets
    @PostMapping
    public ResponseEntity<TicketDTO> createTicket(
            @RequestBody TicketDTO ticketDTO,
            @RequestHeader("Authorization") String authorizationHeader) {
        
        logger.info("Creating ticket with authorization header");
        
        // Get the authenticated user from the authentication service
        UserResponseDTO manager = authService.getAuthenticatedUser(authorizationHeader);
        logger.info("Authenticated manager: ID={}, email={}, role={}, tenant={}", 
                manager.getId(), manager.getEmail(), manager.getRole(), manager.getTenant());
        
        // Check if the user has MANAGER role
        if (!"MANAGER".equalsIgnoreCase(manager.getRole())) {
            logger.error("Unauthorized attempt to create ticket by non-manager user: {}", manager.getEmail());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only managers can create tickets");
        }
        
        // Validate manager's tenant
        if (manager.getTenant() == null || manager.getTenant().isEmpty()) {
            logger.error("Manager has no tenant assigned: {}", manager.getEmail());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Manager has no tenant assigned");
        }
        
        // Validate log ID
        if (ticketDTO.getLogId() == null) {
            logger.error("No log ID provided for ticket");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Log ID must be specified when creating a ticket");
        }
        
        // Set the creator information in the ticket
        ticketDTO.setUserId(manager.getId());
        ticketDTO.setUserEmail(manager.getEmail());
        
        // Explicitly set the tenant from the manager's tenant - overriding any tenant that might have been in the request
        String managerTenant = manager.getTenant();
        ticketDTO.setTenant(managerTenant);
        
        logger.info("Creating ticket for manager: {} with tenant: {}, logId: {}", 
                manager.getEmail(), managerTenant, ticketDTO.getLogId());
        
        // Create the ticket
        TicketDTO createdTicket = ticketService.createTicket(ticketDTO);
        
        // Verify tenant was set correctly
        if (createdTicket.getTenant() == null || !managerTenant.equals(createdTicket.getTenant())) {
            logger.error("Tenant was not set correctly in the created ticket. Expected: {}, Got: {}", 
                    managerTenant, createdTicket.getTenant());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to set tenant in ticket");
        }
        
        logger.info("Ticket created successfully with ID: {}, tenant: {}, logId: {}", 
                createdTicket.getId(), createdTicket.getTenant(), createdTicket.getLogId());
        
        return ResponseEntity.ok(createdTicket);
    }
    
    // Temporary endpoint for testing without authentication service
    @PostMapping("/test")
    public ResponseEntity<TicketDTO> createTicketTest(@RequestBody TicketDTO ticketDTO) {
        logger.info("Creating test ticket without authentication");
        
        // Simulate a manager user
        Long managerId = 1L;
        String managerEmail = "test-manager@example.com";
        String managerTenant = "test-tenant";
        
        logger.info("Simulating manager: ID={}, email={}, tenant={}", managerId, managerEmail, managerTenant);
        
        // Set creator information
        ticketDTO.setUserId(managerId);
        ticketDTO.setUserEmail(managerEmail);
        
        // Explicitly set the tenant from the simulated manager
        ticketDTO.setTenant(managerTenant);
        
        // Validate log ID
        if (ticketDTO.getLogId() == null) {
            logger.error("No log ID provided for test ticket");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Log ID must be specified when creating a ticket");
        }
        
        logger.info("Creating test ticket with tenant: {}, logId: {}", managerTenant, ticketDTO.getLogId());
        
        // Create the ticket
        TicketDTO createdTicket = ticketService.createTicket(ticketDTO);
        
        // Verify tenant was set correctly
        if (createdTicket.getTenant() == null || !managerTenant.equals(createdTicket.getTenant())) {
            logger.error("Tenant was not set correctly in the test ticket. Expected: {}, Got: {}", 
                    managerTenant, createdTicket.getTenant());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to set tenant in test ticket");
        }
        
        logger.info("Test ticket created successfully with ID: {}, tenant: {}, logId: {}", 
                createdTicket.getId(), createdTicket.getTenant(), createdTicket.getLogId());
        
        return ResponseEntity.ok(createdTicket);
    }

    // Assign ticket to a user (developer or tester)
    @PutMapping("/{id}/assign")
    public ResponseEntity<TicketDTO> assignTicket(
            @PathVariable Long id, 
            @RequestParam Long assignedToUserId,
            @RequestHeader("Authorization") String authorizationHeader) {
        
        // Get the authenticated user (manager)
        UserResponseDTO manager = authService.getAuthenticatedUser(authorizationHeader);
        
        // Check if the user has MANAGER role
        if (!"MANAGER".equalsIgnoreCase(manager.getRole())) {
            logger.error("Unauthorized attempt to assign ticket by non-manager user: {}", manager.getEmail());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only managers can assign tickets");
        }
        
        // Assign the ticket to the user with tenant validation
        TicketDTO updatedTicket = ticketService.assignTicket(id, assignedToUserId, manager.getTenant(), authorizationHeader);
        return ResponseEntity.ok(updatedTicket);
    }

    // Get all tickets for the current user's tenant
    @GetMapping
    public ResponseEntity<List<TicketDTO>> getAllTickets(
            @RequestHeader("Authorization") String authorizationHeader) {
        
        // Get the authenticated user
        UserResponseDTO user = authService.getAuthenticatedUser(authorizationHeader);
        
        // Get tickets for the user's tenant
        return ResponseEntity.ok(ticketService.getTicketsByTenant(user.getTenant()));
    }

    // Get a ticket by ID
    @GetMapping("/{id}")
    public ResponseEntity<TicketDTO> getTicketById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorization) {
        logger.info("Received request to get ticket with ID: {}", id);

        // Get authenticated user
        UserResponseDTO user = authService.getAuthenticatedUser(authorization);

        // Get the ticket with solution information
        TicketDTO ticketDTO = ticketService.getTicketWithSolutionInfo(id, user.getTenant());

        return ResponseEntity.ok(ticketDTO);
    }

    // Update ticket with tenant validation
    @PutMapping("/{id}")
    public ResponseEntity<TicketDTO> updateTicket(
            @PathVariable Long id, 
            @RequestBody TicketDTO ticketDTO,
            @RequestHeader("Authorization") String authorizationHeader) {
        
        // Get the authenticated user
        UserResponseDTO user = authService.getAuthenticatedUser(authorizationHeader);
        
        // Ensure tenant is not changed
        ticketDTO.setTenant(user.getTenant());
        
        // Update the ticket with tenant validation
        TicketDTO updatedTicket = ticketService.updateTicketWithTenantValidation(id, ticketDTO, user.getTenant());
        return (updatedTicket != null) ? ResponseEntity.ok(updatedTicket) : ResponseEntity.notFound().build();
    }

    // Delete ticket with tenant validation
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(
            @PathVariable Long id,
            @RequestHeader("Authorization") String authorizationHeader) {
        
        // Get the authenticated user
        UserResponseDTO user = authService.getAuthenticatedUser(authorizationHeader);
        
        // Delete the ticket with tenant validation
        return ticketService.deleteTicketWithTenantValidation(id, user.getTenant()) 
            ? ResponseEntity.ok().build() 
            : ResponseEntity.notFound().build();
    }

    /**
     * Get the solution for a ticket
     * @param ticketId The ticket ID
     * @param authorization The authorization header
     * @return The solution for the ticket
     */
    @GetMapping("/{ticketId}/solution")
    public ResponseEntity<SolutionDTO> getTicketSolution(@PathVariable Long ticketId,
                                                       @RequestHeader("Authorization") String authorization) {
        logger.info("Received request to get solution for ticket ID: {}", ticketId);
        
        // Get authenticated user
        UserResponseDTO user = authService.getAuthenticatedUser(authorization);
        
        try {
            // Get the solution
            SolutionDTO solution = solutionService.getSolutionByTicketId(ticketId, user.getTenant());
            return ResponseEntity.ok(solution);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                logger.info("No solution found for ticket ID: {}", ticketId);
                return ResponseEntity.notFound().build();
            }
            throw e;
        }
    }
}
