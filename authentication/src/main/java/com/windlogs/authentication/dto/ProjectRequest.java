package com.windlogs.authentication.dto;

import com.windlogs.authentication.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ProjectRequest {
    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "platform is required")
    private String platform;

    @NotBlank(message = "description is required")
    private String description;

    @NotNull(message = "team is required")
    private String team;
}
