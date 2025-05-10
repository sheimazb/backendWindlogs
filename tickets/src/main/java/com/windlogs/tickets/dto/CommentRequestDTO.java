package com.windlogs.tickets.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentRequestDTO {
    private String content;
    private Long ticketId;
    private Set<Long> mentionedUserIds = new HashSet<>();
} 