package com.windlogs.tickets.service;

import com.windlogs.tickets.dto.CommentRequestDTO;
import com.windlogs.tickets.dto.CommentResponseDTO;
import com.windlogs.tickets.dto.UserResponseDTO;
import com.windlogs.tickets.entity.Comment;
import com.windlogs.tickets.entity.Ticket;
import com.windlogs.tickets.exception.UnauthorizedException;
import com.windlogs.tickets.kafka.NotificationProducer;
import com.windlogs.tickets.mapper.CommentMapper;
import com.windlogs.tickets.repository.CommentRepository;
import com.windlogs.tickets.repository.TicketRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CommentService {
    private static final Logger logger = LoggerFactory.getLogger(CommentService.class);
    
    private final CommentRepository commentRepository;
    private final TicketRepository ticketRepository;
    private final CommentMapper commentMapper;
    private final AuthService authService;
    private final NotificationProducer notificationProducer;

    public CommentService(
            CommentRepository commentRepository, 
            TicketRepository ticketRepository, 
            CommentMapper commentMapper,
            AuthService authService,
            NotificationProducer notificationProducer) {
        this.commentRepository = commentRepository;
        this.ticketRepository = ticketRepository;
        this.commentMapper = commentMapper;
        this.authService = authService;
        this.notificationProducer = notificationProducer;
    }

    /**
     * Get all comments for a ticket by ticket ID
     * @param ticketId The ticket ID
     * @param tenant The tenant of the requesting user
     * @return List of comments for the ticket
     */
    public List<CommentResponseDTO> getCommentsByTicketId(Long ticketId, String tenant) {
        logger.info("Getting comments for ticket ID: {}, tenant: {}", ticketId, tenant);
        
        // Check if the ticket exists and belongs to the tenant
        ticketRepository.findByIdAndTenant(ticketId, tenant)
                .orElseThrow(() -> {
                    logger.error("Ticket not found or not in user's tenant: {}", ticketId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found");
                });
        
        // Use the repository method that includes tenant security
        List<Comment> comments = commentRepository.findByTicketIdAndTenant(ticketId, tenant);
        List<CommentResponseDTO> commentDTOs = commentMapper.commentsToCommentResponseDTOs(comments);
        
        logger.debug("Retrieved {} comments for ticket ID: {}", comments.size(), ticketId);
        
        return commentDTOs;
    }
    
    /**
     * Get all comments for a ticket by ticket ID and populate mentioned users
     * @param ticketId The ticket ID
     * @param authorization The authorization header
     * @param tenant The tenant of the requesting user
     * @return List of comments for the ticket with populated mentioned users
     */
    public List<CommentResponseDTO> getCommentsByTicketIdWithToken(Long ticketId, String authorization, String tenant) {
        logger.info("Getting comments with user details for ticket ID: {}, tenant: {}", ticketId, tenant);
        
        // Check if the ticket exists and belongs to the tenant
        ticketRepository.findByIdAndTenant(ticketId, tenant)
                .orElseThrow(() -> {
                    logger.error("Ticket not found or not in user's tenant: {}", ticketId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found");
                });
        
        // Use the repository method that includes tenant security
        List<Comment> comments = commentRepository.findByTicketIdAndTenant(ticketId, tenant);
        List<CommentResponseDTO> commentDTOs = commentMapper.commentsToCommentResponseDTOs(comments);
        
        // Populate mentioned users for each comment
        for (CommentResponseDTO dto : commentDTOs) {
            populateMentionedUsers(dto, authorization);
        }
        
        logger.debug("Retrieved and populated {} comments for ticket ID: {}", comments.size(), ticketId);
        
        return commentDTOs;
    }

    /**
     * Get a comment by its ID
     * @param commentId The comment ID
     * @param tenant The tenant of the requesting user
     * @return The comment if found
     */
    public CommentResponseDTO getCommentById(Long commentId, String tenant) {
        logger.info("Getting comment by ID: {}, tenant: {}", commentId, tenant);
        
        Comment comment = commentRepository.findByIdAndTicketTenant(commentId, tenant)
                .orElseThrow(() -> {
                    logger.error("Comment not found or not in user's tenant: {}", commentId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
                });
        
        return commentMapper.commentToCommentResponseDTO(comment);
    }
    
    /**
     * Get a comment by its ID and populate mentioned users
     * @param commentId The comment ID
     * @param authorization The authorization header
     * @param tenant The tenant of the requesting user
     * @return The comment with populated mentioned users
     */
    public CommentResponseDTO getCommentByIdWithToken(Long commentId, String authorization, String tenant) {
        logger.info("Getting comment with user details by ID: {}, tenant: {}", commentId, tenant);
        
        Comment comment = commentRepository.findByIdAndTicketTenant(commentId, tenant)
                .orElseThrow(() -> {
                    logger.error("Comment not found or not in user's tenant: {}", commentId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
                });
        
        CommentResponseDTO dto = commentMapper.commentToCommentResponseDTO(comment);
        
        // Populate mentioned users
        populateMentionedUsers(dto, authorization);
        
        return dto;
    }

    /**
     * Create a new comment
     * @param commentRequestDTO The comment to create
     * @param userId The ID of the user creating the comment
     * @param tenant The tenant of the user
     * @return The created comment
     */
    @Transactional
    public CommentResponseDTO createComment(CommentRequestDTO commentRequestDTO, Long userId, String tenant) {
        logger.info("Creating comment for ticket ID: {}, by user: {}", commentRequestDTO.getTicketId(), userId);
        
        Ticket ticket = ticketRepository.findByIdAndTenant(commentRequestDTO.getTicketId(), tenant)
                .orElseThrow(() -> {
                    logger.error("Ticket not found or not in user's tenant: {}", commentRequestDTO.getTicketId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found");
                });

        // Use mapper to convert DTO to entity
        Comment comment = commentMapper.commentRequestDTOToComment(commentRequestDTO);
        comment.setAuthorUserId(userId);
        comment.setTicket(ticket);
        
        Comment savedComment = commentRepository.save(comment);
        logger.info("Comment created successfully with ID: {}, for ticket: {}", savedComment.getId(), ticket.getId());
        
        return commentMapper.commentToCommentResponseDTO(savedComment);
    }

    /**
     * Create a new comment with proper mention processing using authentication token
     * @param commentRequestDTO The comment to create
     * @param authorization The authorization header
     * @return The created comment with populated mentioned users
     */
    @Transactional
    public CommentResponseDTO createCommentWithToken(CommentRequestDTO commentRequestDTO, String authorization) {
        // Get current user from token
        UserResponseDTO currentUser = authService.getAuthenticatedUser(authorization);
        logger.info("Creating comment for ticket ID: {}, by user: {}, tenant: {}", 
                commentRequestDTO.getTicketId(), currentUser.getEmail(), currentUser.getTenant());
        
        Ticket ticket = ticketRepository.findByIdAndTenant(commentRequestDTO.getTicketId(), currentUser.getTenant())
                .orElseThrow(() -> {
                    logger.error("Ticket not found or not in user's tenant: {}", commentRequestDTO.getTicketId());
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticket not found");
                });

        // Use mapper to convert DTO to entity
        Comment comment = commentMapper.commentRequestDTOToComment(commentRequestDTO);
        comment.setAuthorUserId(currentUser.getId());
        comment.setTicket(ticket);
        
        // Process mentions if any - filter to only include users from the same tenant
        if (commentRequestDTO.getMentionedUserIds() != null && !commentRequestDTO.getMentionedUserIds().isEmpty()) {
            Set<Long> validMentionedUserIds = new HashSet<>();
            
            // Get all mentioned users' information and filter by same tenant
            for (Long userId : commentRequestDTO.getMentionedUserIds()) {
                try {
                    // Get the mentioned user
                    UserResponseDTO mentionedUser = authService.getUserById(userId, authorization);
                    
                    // Enhanced tenant security check - strict validation
                    if (mentionedUser != null) {
                        // First check - mentioned user must have a tenant value
                        if (mentionedUser.getTenant() == null) {
                            logger.warn("User {} attempted to mention user {} who has no tenant assigned", 
                                    currentUser.getId(), userId);
                            continue;
                        }
                        
                        // Second check - user tenant must match authenticated user tenant
                        if (!currentUser.getTenant().equals(mentionedUser.getTenant())) {
                            logger.warn("Cross-tenant mention attempt detected! User {} (tenant: {}) attempted to mention user {} (tenant: {})", 
                                    currentUser.getId(), currentUser.getTenant(), userId, mentionedUser.getTenant());
                            continue; // Skip this mention
                        }
                        
                        // If validation passes, add the user to valid mentions
                        validMentionedUserIds.add(userId);
                        logger.debug("Successfully added valid mention for user ID: {}", userId);
                    } else {
                        logger.warn("Mentioned user with ID {} does not exist", userId);
                    }
                } catch (Exception e) {
                    // Log the error but continue processing other mentions
                    logger.error("Error processing mention for user ID: {}: {}", userId, e.getMessage());
                }
            }
            
            // Set only the valid mentioned user IDs
            comment.setMentionedUserIds(validMentionedUserIds);
            if (validMentionedUserIds.size() < commentRequestDTO.getMentionedUserIds().size()) {
                logger.info("Filtered out {} invalid mentions from request", 
                        commentRequestDTO.getMentionedUserIds().size() - validMentionedUserIds.size());
            }
        }
        
        Comment savedComment = commentRepository.save(comment);
        logger.info("Comment created successfully with ID: {}, for ticket: {}", savedComment.getId(), ticket.getId());
        
        // Send notification for the new comment using the domain-specific event producer
        try {
            // Get notification message (use comment content or a placeholder)
            String content = savedComment.getContent();
            if (content == null || content.isEmpty()) {
                content = "New comment added to ticket #" + ticket.getId();
            } else if (content.length() > 200) {
                content = content.substring(0, 200) + "...";
            }
            
            // For simplicity, sending notification to ticket creator or assignee
            String recipientEmail = null;
            
            // Try to get the ticket creator's email
            if (ticket.getCreatorUserId() != null && !ticket.getCreatorUserId().equals(currentUser.getId())) {
                try {
                    UserResponseDTO creator = authService.getUserById(ticket.getCreatorUserId(), authorization);
                    if (creator != null) {
                        recipientEmail = creator.getEmail();
                    }
                } catch (Exception e) {
                    logger.warn("Could not get creator user info: {}", e.getMessage());
                }
            }
            
            // If no creator, try to get the assigned user's email
            if (recipientEmail == null && ticket.getAssignedToUserId() != null && !ticket.getAssignedToUserId().equals(currentUser.getId())) {
                try {
                    UserResponseDTO assignee = authService.getUserById(ticket.getAssignedToUserId(), authorization);
                    if (assignee != null) {
                        recipientEmail = assignee.getEmail();
                    }
                } catch (Exception e) {
                    logger.warn("Could not get assigned user info: {}", e.getMessage());
                }
            }
            
            // If we found a recipient, send notification
            if (recipientEmail != null) {
                notificationProducer.sendCommentNotification(
                    savedComment.getId(),
                    content,
                    ticket.getId(),
                    currentUser.getEmail(), // Using email as author name since getName() is not available
                    currentUser.getEmail(),  // Sender email
                    recipientEmail,          // Recipient email
                    currentUser.getTenant()
                );
                logger.info("Comment notification sent for comment ID: {} to recipient: {}", 
                        savedComment.getId(), recipientEmail);
            } else {
                logger.info("No suitable recipient found for comment notification");
            }
            
        } catch (Exception e) {
            // Don't fail the comment creation if notification fails
            logger.error("Failed to send comment notification: {}", e.getMessage(), e);
        }
        
        CommentResponseDTO responseDTO = commentMapper.commentToCommentResponseDTO(savedComment);
        
        // Populate mentioned users for response
        populateMentionedUsers(responseDTO, authorization);
        
        return responseDTO;
    }

    /**
     * Update an existing comment
     * @param commentId The comment ID to update
     * @param commentRequestDTO The updated comment data
     * @param userId The ID of the user updating the comment
     * @param tenant The tenant of the user
     * @return The updated comment
     */
    @Transactional
    public CommentResponseDTO updateComment(Long commentId, CommentRequestDTO commentRequestDTO, Long userId, String tenant) {
        logger.info("Updating comment with ID: {}, by user: {}", commentId, userId);
        
        Comment comment = commentRepository.findByIdAndTicketTenant(commentId, tenant)
                .orElseThrow(() -> {
                    logger.error("Comment not found or not in user's tenant: {}", commentId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
                });
        
        // Check if the current user is the author
        if (!comment.getAuthorUserId().equals(userId)) {
            logger.error("User {} is not the author of comment {}", userId, commentId);
            throw new UnauthorizedException("Only the author can update their comment");
        }
        
        // Update comment using mapper
        commentMapper.updateCommentFromDTO(commentRequestDTO, comment);
        
        Comment updatedComment = commentRepository.save(comment);
        logger.info("Comment updated successfully: {}", commentId);
        
        return commentMapper.commentToCommentResponseDTO(updatedComment);
    }
    
    /**
     * Update an existing comment with proper mention processing using authentication token
     * @param commentId The comment ID to update
     * @param commentRequestDTO The updated comment data
     * @param authorization The authorization header
     * @return The updated comment with populated mentioned users
     */
    @Transactional
    public CommentResponseDTO updateCommentWithToken(Long commentId, CommentRequestDTO commentRequestDTO, String authorization) {
        // Get current user from token
        UserResponseDTO currentUser = authService.getAuthenticatedUser(authorization);
        logger.info("Updating comment with ID: {}, by user: {}, tenant: {}", 
                commentId, currentUser.getEmail(), currentUser.getTenant());
        
        Comment comment = commentRepository.findByIdAndTicketTenant(commentId, currentUser.getTenant())
                .orElseThrow(() -> {
                    logger.error("Comment not found or not in user's tenant: {}", commentId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
                });
        
        // Check if the current user is the author
        if (!comment.getAuthorUserId().equals(currentUser.getId())) {
            logger.error("User {} is not the author of comment {}", currentUser.getId(), commentId);
            throw new UnauthorizedException("Only the author can update their comment");
        }
        
        // Update comment using mapper
        commentMapper.updateCommentFromDTO(commentRequestDTO, comment);
        
        // Process mentions if any - filter to only include users from the same tenant
        if (commentRequestDTO.getMentionedUserIds() != null && !commentRequestDTO.getMentionedUserIds().isEmpty()) {
            Set<Long> validMentionedUserIds = new HashSet<>();
            
            // Get all mentioned users' information and filter by same tenant
            for (Long userId : commentRequestDTO.getMentionedUserIds()) {
                try {
                    // Get the mentioned user
                    UserResponseDTO mentionedUser = authService.getUserById(userId, authorization);
                    
                    // Enhanced tenant security check - strict validation
                    if (mentionedUser != null) {
                        // First check - mentioned user must have a tenant value
                        if (mentionedUser.getTenant() == null) {
                            logger.warn("User {} attempted to mention user {} who has no tenant assigned", 
                                    currentUser.getId(), userId);
                            continue;
                        }
                        
                        // Second check - user tenant must match authenticated user tenant
                        if (!currentUser.getTenant().equals(mentionedUser.getTenant())) {
                            logger.warn("Cross-tenant mention attempt detected! User {} (tenant: {}) attempted to mention user {} (tenant: {})", 
                                    currentUser.getId(), currentUser.getTenant(), userId, mentionedUser.getTenant());
                            continue; // Skip this mention
                        }
                        
                        // If validation passes, add the user to valid mentions
                        validMentionedUserIds.add(userId);
                        logger.debug("Successfully added valid mention for user ID: {}", userId);
                    } else {
                        logger.warn("Mentioned user with ID {} does not exist", userId);
                    }
                } catch (Exception e) {
                    // Log the error but continue processing other mentions
                    logger.error("Error processing mention for user ID: {}: {}", userId, e.getMessage());
                }
            }
            
            // Set only the valid mentioned user IDs
            comment.setMentionedUserIds(validMentionedUserIds);
            if (validMentionedUserIds.size() < commentRequestDTO.getMentionedUserIds().size()) {
                logger.info("Filtered out {} invalid mentions from request", 
                        commentRequestDTO.getMentionedUserIds().size() - validMentionedUserIds.size());
            }
        }
        
        Comment updatedComment = commentRepository.save(comment);
        CommentResponseDTO responseDTO = commentMapper.commentToCommentResponseDTO(updatedComment);
        
        // Populate mentioned users in the response
        populateMentionedUsers(responseDTO, authorization);
        
        logger.info("Comment updated successfully: {}", commentId);
        
        return responseDTO;
    }

    /**
     * Delete a comment
     * @param commentId The comment ID to delete
     * @param userId The ID of the user deleting the comment
     * @param tenant The tenant of the user
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId, String tenant) {
        logger.info("Deleting comment with ID: {}, by user: {}", commentId, userId);
        
        Comment comment = commentRepository.findByIdAndTicketTenant(commentId, tenant)
                .orElseThrow(() -> {
                    logger.error("Comment not found or not in user's tenant: {}", commentId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
                });
        
        // Check if the current user is the author
        if (!comment.getAuthorUserId().equals(userId)) {
            logger.error("User {} is not the author of comment {}", userId, commentId);
            throw new UnauthorizedException("Only the author can delete their comment");
        }
        
        commentRepository.delete(comment);
        logger.info("Comment deleted successfully: {}", commentId);
    }
    
    /**
     * Delete a comment using authentication token
     * @param commentId The comment ID to delete
     * @param authorization The authorization header
     */
    @Transactional
    public void deleteCommentWithToken(Long commentId, String authorization) {
        // Get current user from token
        UserResponseDTO currentUser = authService.getAuthenticatedUser(authorization);
        logger.info("Deleting comment with ID: {}, by user: {}, tenant: {}", 
                commentId, currentUser.getEmail(), currentUser.getTenant());
        
        Comment comment = commentRepository.findByIdAndTicketTenant(commentId, currentUser.getTenant())
                .orElseThrow(() -> {
                    logger.error("Comment not found or not in user's tenant: {}", commentId);
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found");
                });
        
        // Check if the current user is the author
        if (!comment.getAuthorUserId().equals(currentUser.getId())) {
            logger.error("User {} is not the author of comment {}", currentUser.getId(), commentId);
            throw new UnauthorizedException("Only the author can delete their comment");
        }
        
        commentRepository.delete(comment);
        logger.info("Comment deleted successfully: {}", commentId);
    }
    
    /**
     * Get comments by the authenticated user
     * @param userId The user ID
     * @param tenant The tenant of the user
     * @return List of comments by the user
     */
    public List<CommentResponseDTO> getCommentsByAuthorUserId(Long userId, String tenant) {
        logger.info("Getting comments by author user ID: {}, tenant: {}", userId, tenant);
        
        List<Comment> comments = commentRepository.findByAuthorUserId(userId);
        
        // Filter by tenant
        comments = comments.stream()
                .filter(comment -> tenant.equals(comment.getTicket().getTenant()))
                .collect(Collectors.toList());
        
        return commentMapper.commentsToCommentResponseDTOs(comments);
    }
    
    /**
     * Helper method to populate mentioned users in a CommentResponseDTO
     * @param dto The CommentResponseDTO to populate
     * @param authorization The authorization header
     */
    private void populateMentionedUsers(CommentResponseDTO dto, String authorization) {
        if (dto.getMentionedUserIds() != null && !dto.getMentionedUserIds().isEmpty()) {
            Set<UserResponseDTO> mentionedUsers = new HashSet<>();
            
            for (Long userId : dto.getMentionedUserIds()) {
                try {
                    UserResponseDTO user = authService.getUserById(userId, authorization);
                    if (user != null) {
                        mentionedUsers.add(user);
                    }
                } catch (Exception e) {
                    // Log error but continue
                    logger.error("Error fetching user details for ID: {}: {}", userId, e.getMessage());
                }
            }
            
            dto.setMentionedUsers(mentionedUsers);
        }
    }
}
