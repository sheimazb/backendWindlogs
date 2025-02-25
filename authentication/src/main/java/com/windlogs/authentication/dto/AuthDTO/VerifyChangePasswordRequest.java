package com.windlogs.authentication.dto.AuthDTO;

import jakarta.validation.constraints.NotBlank;

public class VerifyChangePasswordRequest {
    @NotBlank
    private String token;

    @NotBlank
    private String newPassword;

    public @NotBlank String getToken() {
        return token;
    }

    public void setToken(@NotBlank String token) {
        this.token = token;
    }

    public @NotBlank String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(@NotBlank String newPassword) {
        this.newPassword = newPassword;
    }
}
