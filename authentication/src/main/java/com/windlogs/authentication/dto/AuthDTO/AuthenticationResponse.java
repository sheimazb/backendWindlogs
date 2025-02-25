package com.windlogs.authentication.dto.AuthDTO;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AuthenticationResponse {
    private String token;
    private String email;
    private String fullName;
    private String role;
}
