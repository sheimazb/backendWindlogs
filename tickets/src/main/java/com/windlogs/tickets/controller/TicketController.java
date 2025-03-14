package com.windlogs.tickets.controller;

import com.windlogs.tickets.dto.TicketDTO;
import com.windlogs.tickets.dto.UserResponseDTO;
import com.windlogs.tickets.service.AuthService;
import com.windlogs.tickets.service.TicketService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
public class TicketController {
    private static final Logger logger = LoggerFactory.getLogger(TicketController.class);
    private final TicketService ticketService;
    private final AuthService authService;

    public TicketController(TicketService ticketService, AuthService authService) {
        this.ticketService = ticketService;
        this.authService = authService;
    }

    // Create a ticket
    @PostMapping
    public ResponseEntity<TicketDTO> createTicket(
            @RequestBody TicketDTO ticketDTO,
            @RequestHeader("Authorization") String authorizationHeader) {
        
        logger.info("Creating ticket with authorization header");
        
        // Get the authenticated user from the authentication service
        UserResponseDTO user = authService.getAuthenticatedUser(authorizationHeader);
        
        // Set the user information in the ticket
        ticketDTO.setUserId(user.getId());
        ticketDTO.setUserEmail(user.getEmail());
        
        logger.info("Creating ticket for user: {}", user.getEmail());
        
        return ResponseEntity.ok(ticketService.createTicket(ticketDTO));
    }
    
    // Temporary endpoint for testing without authentication service
    @PostMapping("/test")
    public ResponseEntity<TicketDTO> createTicketTest(@RequestBody TicketDTO ticketDTO) {
        logger.info("Creating test ticket without authentication");
        
        // Set dummy user information
        ticketDTO.setUserId(1L);
        ticketDTO.setUserEmail("test@example.com");
        
        return ResponseEntity.ok(ticketService.createTicket(ticketDTO));
    }

    // Get all tickets
    @GetMapping
    public ResponseEntity<List<TicketDTO>> getAllTickets() {
        return ResponseEntity.ok(ticketService.getAllTickets());
    }

    // Get ticket by ID
    @GetMapping("/{id}")
    public ResponseEntity<TicketDTO> getTicketById(@PathVariable Long id) {
        TicketDTO ticketDTO = ticketService.getTicketById(id);
        return (ticketDTO != null) ? ResponseEntity.ok(ticketDTO) : ResponseEntity.notFound().build();
    }

    // Update ticket
    @PutMapping("/{id}")
    public ResponseEntity<TicketDTO> updateTicket(@PathVariable Long id, @RequestBody TicketDTO ticketDTO) {
        TicketDTO updatedTicket = ticketService.updateTicket(id, ticketDTO);
        return (updatedTicket != null) ? ResponseEntity.ok(updatedTicket) : ResponseEntity.notFound().build();
    }

    // Delete ticket
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long id) {
        return ticketService.deleteTicket(id) ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }
}
