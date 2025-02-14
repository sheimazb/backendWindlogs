package com.windlogs.authentication.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmployeeAuthResponse {
    private String token;
    private String email;
    private String fullName;
    private String role;
}
