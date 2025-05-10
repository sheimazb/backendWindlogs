package com.windlogs.tickets.controller;

import com.windlogs.tickets.dto.CommentRequestDTO;
import com.windlogs.tickets.dto.CommentResponseDTO;
import com.windlogs.tickets.dto.UserResponseDTO;
import com.windlogs.tickets.service.AuthService;
import com.windlogs.tickets.service.CommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/comments")
public class CommentController {
    private static final Logger logger = LoggerFactory.getLogger(CommentController.class);
    
    private final CommentService commentService;
    private final AuthService authService;

    public CommentController(CommentService commentService, AuthService authService) {
        this.commentService = commentService;
        this.authService = authService;
    }

    /**
     * Get all comments for a specific ticket
     * @param ticketId The ticket ID
     * @param authorization The authorization header
     * @return List of comments for the ticket
     */
    @GetMapping("/ticket/{ticketId}")
    public ResponseEntity<List<CommentResponseDTO>> getCommentsByTicketId(
            @PathVariable Long ticketId,
            @RequestHeader("Authorization") String authorization) {
        logger.info("Received request to get comments for ticket ID: {}", ticketId);
        
        // Get authenticated user
        UserResponseDTO user = authService.getAuthenticatedUser(authorization);
        
        // Use the token-based method to populate mentioned users
        List<CommentResponseDTO> comments = commentService.getCommentsByTicketIdWithToken(ticketId, authorization, user.getTenant());
        
        return ResponseEntity.ok(comments);
    }

    /**
     * Get a comment by ID
     * @param commentId The comment ID
     * @param authorization The authorization header
     * @return The comment
     */
    @GetMapping("/{commentId}")
    public ResponseEntity<CommentResponseDTO> getCommentById(
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String authorization) {
        logger.info("Received request to get comment with ID: {}", commentId);
        
        // Get authenticated user
        UserResponseDTO user = authService.getAuthenticatedUser(authorization);
        
        // Use the token-based method to populate mentioned users
        CommentResponseDTO comment = commentService.getCommentByIdWithToken(commentId, authorization, user.getTenant());
        
        return ResponseEntity.ok(comment);
    }

    /**
     * Create a new comment
     * @param commentRequestDTO The comment data
     * @param authorization The authorization header
     * @return The created comment
     */
    @PostMapping
    public ResponseEntity<CommentResponseDTO> createComment(
            @RequestBody CommentRequestDTO commentRequestDTO,
            @RequestHeader("Authorization") String authorization) {
        logger.info("Received request to create a comment for ticket ID: {}", commentRequestDTO.getTicketId());
        
        // Use the token-based method to properly process mentions
        CommentResponseDTO createdComment = commentService.createCommentWithToken(commentRequestDTO, authorization);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(createdComment);
    }

    /**
     * Update a comment
     * @param commentId The comment ID
     * @param commentRequestDTO The updated comment data
     * @param authorization The authorization header
     * @return The updated comment
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<CommentResponseDTO> updateComment(
            @PathVariable Long commentId,
            @RequestBody CommentRequestDTO commentRequestDTO,
            @RequestHeader("Authorization") String authorization) {
        logger.info("Received request to update comment with ID: {}", commentId);
        
        // Use the token-based method to properly process mentions
        CommentResponseDTO updatedComment = commentService.updateCommentWithToken(commentId, commentRequestDTO, authorization);
        
        return ResponseEntity.ok(updatedComment);
    }

    /**
     * Delete a comment
     * @param commentId The comment ID
     * @param authorization The authorization header
     * @return No content
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @RequestHeader("Authorization") String authorization) {
        logger.info("Received request to delete comment with ID: {}", commentId);
        
        // Use the token-based method
        commentService.deleteCommentWithToken(commentId, authorization);
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Get comments by the authenticated user
     * @param authorization The authorization header
     * @return List of comments by the user
     */
    @GetMapping("/my-comments")
    public ResponseEntity<List<CommentResponseDTO>> getMyComments(
            @RequestHeader("Authorization") String authorization) {
        logger.info("Received request to get comments by authenticated user");
        
        // Get authenticated user
        UserResponseDTO user = authService.getAuthenticatedUser(authorization);
        
        // Get the comments
        List<CommentResponseDTO> comments = commentService.getCommentsByAuthorUserId(user.getId(), user.getTenant());
        
        return ResponseEntity.ok(comments);
    }
}
