package com.windlogs.tickets.controller;

import com.windlogs.tickets.dto.SolutionDTO;
import com.windlogs.tickets.dto.UserResponseDTO;
import com.windlogs.tickets.entity.Ticket;
import com.windlogs.tickets.kafka.NotificationProducer;
import com.windlogs.tickets.kafka.SolutionEvent;
import com.windlogs.tickets.repository.TicketRepository;
import com.windlogs.tickets.service.SolutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * Controller for testing functionality
 */
@RestController
@RequestMapping("/api/test")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);
    private final NotificationProducer notificationProducer;
    private final SolutionService solutionService;
    private final TicketRepository ticketRepository;

    public TestController(NotificationProducer notificationProducer, 
                         SolutionService solutionService,
                         TicketRepository ticketRepository) {
        this.notificationProducer = notificationProducer;
        this.solutionService = solutionService;
        this.ticketRepository = ticketRepository;
    }

    /**
     * Test sending a solution notification
     */
    @GetMapping("/notification/solution")
    public ResponseEntity<String> testSolutionNotification() {
        logger.info("TEST: Sending test solution notification");
        try {
            notificationProducer.sendTestSolutionNotification();
            return ResponseEntity.ok("Test solution notification sent successfully");
        } catch (Exception e) {
            logger.error("TEST: Error sending test solution notification: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
    
    @GetMapping(
        value = "/create-solution/{ticketId}",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<?> testCreateSolution(@PathVariable Long ticketId) {
        logger.info("Attempting to create a test solution for ticket ID: {}", ticketId);
        
        Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
        if (!ticketOpt.isPresent()) {
            return ResponseEntity.badRequest().body("Ticket not found: " + ticketId);
        }
        
        Ticket ticket = ticketOpt.get();
        logger.info("Found ticket: {}, creator: {}, tenant: {}", 
                ticket.getId(), ticket.getCreatorUserId(), ticket.getTenant());
        
        // Create a fake user for testing
        UserResponseDTO fakeUser = new UserResponseDTO();
        fakeUser.setId(999L); // Different from the ticket creator
        fakeUser.setEmail("de@gmail.com");
        fakeUser.setTenant(ticket.getTenant());
        
        // Create solution DTO
        SolutionDTO solutionDTO = new SolutionDTO();
        solutionDTO.setTicketId(ticketId);
        solutionDTO.setContent("This is a test solution created via test endpoint.");
        solutionDTO.setTitle("Test Solution");
        
        try {
            SolutionDTO created = solutionService.createSolution(
                    solutionDTO, 
                    fakeUser.getId(), 
                    fakeUser.getEmail(), 
                    fakeUser.getTenant()
            );
            
            return ResponseEntity.ok("Solution created successfully: " + created.getId());
        } catch (Exception e) {
            logger.error("Error creating test solution: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    /**
     * Test sending a directed solution notification
     */
    @GetMapping("/notification/direct-solution")
    public ResponseEntity<String> testDirectSolutionNotification(
            @RequestParam(required = false, defaultValue = "test-recipient@example.com") String recipient) {
        logger.info("TEST: Sending direct test solution notification to {}", recipient);
        try {
            SolutionEvent solutionEvent = new SolutionEvent(
                    String.valueOf(System.currentTimeMillis()),
                    "test-tenant",
                    999L, // Test solution ID
                    888L, // Test ticket ID
                    "test-sender@example.com", // Sender email
                    recipient, // Recipient email
                    "This is a direct test solution notification", // Content
                    "Test User", // Author name
                    "DRAFT" // Status
            );
            
            notificationProducer.sendNotificationEvent(solutionEvent);
            return ResponseEntity.ok("Direct test solution notification sent to " + recipient);
        } catch (Exception e) {
            logger.error("TEST: Error sending direct test solution notification: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }

    /**
     * Test creating a solution with specific creator and assignee
     */
    @PostMapping("/create-test-solution")
    public ResponseEntity<?> createTestSolution(
            @RequestParam Long ticketId,
            @RequestParam(required = false, defaultValue = "1") Long creatorId,
            @RequestParam(required = false, defaultValue = "2") Long assigneeId,
            @RequestParam(required = false, defaultValue = "creator@example.com") String creatorEmail,
            @RequestParam(required = false, defaultValue = "assignee@example.com") String assigneeEmail) {
        
        logger.info("TEST: Creating test solution with ticketId={}, creatorId={}, assigneeId={}", 
                ticketId, creatorId, assigneeId);
        
        try {
            // First check if ticket exists
            Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
            if (!ticketOpt.isPresent()) {
                return ResponseEntity.badRequest().body("Ticket not found: " + ticketId);
            }
            
            Ticket ticket = ticketOpt.get();
            
            // Update ticket with test values
            ticket.setCreatorUserId(creatorId);
            ticket.setAssignedToUserId(assigneeId);
            ticket.setUserEmail(creatorEmail); // Set creator's email directly on ticket
            ticketRepository.save(ticket);
            
            logger.info("TEST: Updated ticket with creatorId={}, assigneeId={}, userEmail={}", 
                    creatorId, assigneeId, creatorEmail);
            
            // Create solution DTO
            SolutionDTO solutionDTO = new SolutionDTO();
            solutionDTO.setTicketId(ticketId);
            solutionDTO.setContent("This is a test solution created via test endpoint.");
            solutionDTO.setTitle("Test Solution");
            
            // Create solution as the assignee
            SolutionDTO created = solutionService.createSolution(
                    solutionDTO, 
                    assigneeId,
                    assigneeEmail,
                    ticket.getTenant()
            );
            
            return ResponseEntity.ok("Test solution created successfully with ID: " + created.getId());
        } catch (Exception e) {
            logger.error("TEST: Error creating test solution: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Error: " + e.getMessage());
        }
    }
} 