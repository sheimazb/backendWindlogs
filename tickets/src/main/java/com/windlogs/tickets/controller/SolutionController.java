package com.windlogs.tickets.controller;

import com.windlogs.tickets.dto.SolutionDTO;
import com.windlogs.tickets.dto.UserResponseDTO;
import com.windlogs.tickets.service.AuthService;
import com.windlogs.tickets.service.SolutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/solutions")
public class SolutionController {
    private static final Logger logger = LoggerFactory.getLogger(SolutionController.class);
    private final SolutionService solutionService;
    private final AuthService authService;

    public SolutionController(SolutionService solutionService, AuthService authService) {
        this.solutionService = solutionService;
        this.authService = authService;
    }

    /**
     * Create a new solution
     * @param solutionDTO The solution data
     * @param authorization The authorization header
     * @return The created solution
     */
    @PostMapping
    public ResponseEntity<SolutionDTO> createSolution(@RequestBody SolutionDTO solutionDTO,
                                                     @RequestHeader("Authorization") String authorization) {
        logger.info("Received request to create a solution for ticket ID: {}", solutionDTO.getTicketId());
        
        // Get authenticated user
        UserResponseDTO user = authService.getAuthenticatedUser(authorization);
        
        // Create the solution
        SolutionDTO createdSolution = solutionService.createSolution(
                solutionDTO, 
                user.getId(), 
                user.getEmail(), 
                user.getTenant()
        );
        
        return new ResponseEntity<>(createdSolution, HttpStatus.CREATED);
    }

    /**
     * Get a solution by ID
     * @param id The solution ID
     * @param authorization The authorization header
     * @return The solution
     */
    @GetMapping("/{id}")
    public ResponseEntity<SolutionDTO> getSolutionById(@PathVariable Long id,
                                                      @RequestHeader("Authorization") String authorization) {
        logger.info("Received request to get solution with ID: {}", id);
        
        // Get authenticated user
        UserResponseDTO user = authService.getAuthenticatedUser(authorization);
        
        // Get the solution
        SolutionDTO solution = solutionService.getSolutionById(id, user.getTenant());
        
        return ResponseEntity.ok(solution);
    }

    /**
     * Get a solution by ticket ID
     * @param ticketId The ticket ID
     * @param authorization The authorization header
     * @return The solution
     */
    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<SolutionDTO> getSolutionByTicketId(@PathVariable Long ticketId,
                                                           @RequestHeader("Authorization") String authorization) {
        logger.info("Received request to get solution for ticket ID: {}", ticketId);
        
        // Get authenticated user
        UserResponseDTO user = authService.getAuthenticatedUser(authorization);
        
        // Get the solution
        SolutionDTO solution = solutionService.getSolutionByTicketId(ticketId, user.getTenant());
        
        return ResponseEntity.ok(solution);
    }

    /**
     * Update a solution
     * @param id The solution ID
     * @param solutionDTO The updated solution data
     * @param authorization The authorization header
     * @return The updated solution
     */
    @PutMapping("/{id}")
    public ResponseEntity<SolutionDTO> updateSolution(@PathVariable Long id,
                                                    @RequestBody SolutionDTO solutionDTO,
                                                    @RequestHeader("Authorization") String authorization) {
        logger.info("Received request to update solution with ID: {}", id);
        
        // Get authenticated user
        UserResponseDTO user = authService.getAuthenticatedUser(authorization);
        
        // Update the solution
        SolutionDTO updatedSolution = solutionService.updateSolution(id, solutionDTO, user.getId(), user.getTenant());
        
        return ResponseEntity.ok(updatedSolution);
    }

    /**
     * Get solutions by the authenticated user
     * @param authorization The authorization header
     * @return List of solutions by the user
     */
    @GetMapping("/my-solutions")
    public ResponseEntity<List<SolutionDTO>> getMySolutions(@RequestHeader("Authorization") String authorization) {
        logger.info("Received request to get solutions by authenticated user");
        
        // Get authenticated user
        UserResponseDTO user = authService.getAuthenticatedUser(authorization);
        
        // Get the solutions
        List<SolutionDTO> solutions = solutionService.getSolutionsByAuthorUserId(user.getId(), user.getTenant());
        
        return ResponseEntity.ok(solutions);
    }
} 