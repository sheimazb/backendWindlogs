package com.windlogs.tickets.dto;

import lombok.Data;

@Data
public class UserResponseDTO {
    private Long id;
    private String username;
    private String email;
    private String role; // e.g., ADMIN, USER
    private String tenant; // Added tenant field to track user's organization
}