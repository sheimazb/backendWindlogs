package com.windlogs.tickets.controller;

import com.windlogs.tickets.dto.SolutionDTO;
import com.windlogs.tickets.dto.TicketDTO;
import com.windlogs.tickets.dto.UserResponseDTO;
import com.windlogs.tickets.enums.Status;
import com.windlogs.tickets.exception.UnauthorizedException;
import com.windlogs.tickets.service.AuthService;
import com.windlogs.tickets.service.SolutionService;
import com.windlogs.tickets.service.TicketService;
import lombok.AllArgsConstructor;
import lombok.Data;
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

    private void validateManager(UserResponseDTO user) {
        if (!"MANAGER".equalsIgnoreCase(user.getRole())) {
            logger.error("Unauthorized action by non-manager: {}", user.getEmail());
            throw new UnauthorizedException("Only managers can perform this action");
        }
    }

    @PostMapping
    public ResponseEntity<TicketDTO> createTicket(
            @RequestBody TicketDTO ticketDTO,
            @RequestHeader("Authorization") String authorizationHeader) {

        logger.info("Received request to create a ticket");

        UserResponseDTO manager = authService.getAuthenticatedUser(authorizationHeader);
        validateManager(manager);

        if (ticketDTO.getLogId() == null) {
            return ResponseEntity.badRequest().body(null);
        }

        ticketDTO.setCreatorUserId(manager.getId());
        ticketDTO.setUserEmail(manager.getEmail());
        ticketDTO.setTenant(manager.getTenant());

        TicketDTO createdTicket = ticketService.createTicket(ticketDTO);
        return ResponseEntity.ok(createdTicket);
    }


    @PutMapping("/{id}/assign")
    public ResponseEntity<TicketDTO> assignTicket(
            @PathVariable Long id,
            @RequestParam Long assignedToUserId,
            @RequestHeader("Authorization") String authorizationHeader) {

        UserResponseDTO manager = authService.getAuthenticatedUser(authorizationHeader);
        validateManager(manager);

        TicketDTO updatedTicket = ticketService.assignTicket(id, assignedToUserId, manager.getTenant(), authorizationHeader);
        return ResponseEntity.ok(updatedTicket);
    }

    @GetMapping
    public ResponseEntity<List<TicketDTO>> getAllTickets(@RequestHeader("Authorization") String authorizationHeader) {
        UserResponseDTO user = authService.getAuthenticatedUser(authorizationHeader);
        return ResponseEntity.ok(ticketService.getTicketsByTenant(user.getTenant()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketDTO> getTicketById(@PathVariable Long id, @RequestHeader("Authorization") String authorizationHeader) {
        logger.info("Fetching ticket with ID: {}", id);
        UserResponseDTO user = authService.getAuthenticatedUser(authorizationHeader);
        TicketDTO ticketDTO = ticketService.getTicketWithSolutionInfo(id, user.getTenant());
        return ResponseEntity.ok(ticketDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketDTO> updateTicket(
            @PathVariable Long id,
            @RequestBody TicketDTO ticketDTO,
            @RequestHeader("Authorization") String authorizationHeader) {
        UserResponseDTO user = authService.getAuthenticatedUser(authorizationHeader);
        ticketDTO.setTenant(user.getTenant());

        TicketDTO updatedTicket = ticketService.updateTicketWithTenantValidation(id, ticketDTO, user.getTenant());
        return updatedTicket != null ? ResponseEntity.ok(updatedTicket) : ResponseEntity.notFound().build();
    }

    @PutMapping("/{ticketId}/status")
    public ResponseEntity<?> updateTicketStatus(
            @PathVariable Long ticketId,
            @RequestParam Status newStatus,
            @RequestHeader("Authorization") String authorizationHeader) {
        try {
            UserResponseDTO user = authService.getAuthenticatedUser(authorizationHeader);
            String roleName = user.getRole() != null ? user.getRole().toUpperCase() : "UNKNOWN";
            
            if (!isStatusTransitionAllowed(roleName, newStatus)) {
                String allowedStatuses;
                switch (roleName) {
                    case "MANAGER":
                        allowedStatuses = "TO_DO, MERGED_TO_TEST";
                        break;
                    case "DEVELOPER":
                        allowedStatuses = "IN_PROGRESS, RESOLVED";
                        break;
                    case "TESTER":
                        allowedStatuses = "DONE";
                        break;
                    default:
                        allowedStatuses = "none";
                }
                
                String errorMessage = String.format(
                    "You don't have permission to change the ticket to %s status. Users with %s role can only change to: %s", 
                    newStatus, 
                    roleName,
                    allowedStatuses
                );
                
                logger.warn("User {} with role {} attempted unauthorized status change to {}", 
                    user.getEmail(), roleName, newStatus);
                    
                return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(new ErrorResponse(errorMessage));
            }
            
            try {
                TicketDTO updatedTicket = ticketService.updateStatusTicket(ticketId, newStatus, user.getTenant());
                logger.info("Ticket {} status updated to {} by user {} with role {}", 
                    ticketId, newStatus, user.getEmail(), roleName);
                return ResponseEntity.ok(updatedTicket);
            } catch (ResponseStatusException e) {
                // This will catch validation errors from the service
                logger.error("Status transition validation failed: {}", e.getReason());
                return ResponseEntity
                    .status(e.getStatusCode())
                    .body(new ErrorResponse(e.getReason()));
            }
        } catch (UnauthorizedException e) {
            return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating ticket status", e);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("An error occurred while updating the ticket status."));
        }
    }

    private boolean isStatusTransitionAllowed(String userRole, Status newStatus) {
        if (userRole == null) {
            return false;
        }

        switch (userRole.toUpperCase()) {
            case "MANAGER":
                // Managers can assign tickets and move from RESOLVED to MERGED_TO_TEST
                return newStatus == Status.TO_DO || 
                       newStatus == Status.MERGED_TO_TEST;
            case "DEVELOPER":
                // Developers can change to IN_PROGRESS and RESOLVED
                return newStatus == Status.IN_PROGRESS || 
                       newStatus == Status.RESOLVED;
            case "TESTER":
                // Testers can mark as DONE after testing
                return newStatus == Status.DONE;
            default:
                return false;
        }
    }

    @Data
    @AllArgsConstructor
    private static class ErrorResponse {
        private String message;
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id, @RequestHeader("Authorization") String authorizationHeader) {
        UserResponseDTO user = authService.getAuthenticatedUser(authorizationHeader);
        return ticketService.deleteTicketWithTenantValidation(id, user.getTenant()) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/{ticketId}/solution")
    public ResponseEntity<SolutionDTO> getTicketSolution(@PathVariable Long ticketId, @RequestHeader("Authorization") String authorization) {
        logger.info("Fetching solution for ticket ID: {}", ticketId);
        UserResponseDTO user = authService.getAuthenticatedUser(authorization);

        SolutionDTO solution = solutionService.getSolutionByTicketId(ticketId, user.getTenant());
        return solution != null ? ResponseEntity.ok(solution) : ResponseEntity.notFound().build();
    }
}
