package com.windlogs.tickets.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentResponseDTO {
    private Long id;
    private String content;
    private Long authorUserId;
    private Long ticketId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Set<Long> mentionedUserIds = new HashSet<>();
    private Set<UserResponseDTO> mentionedUsers = new HashSet<>();
} 