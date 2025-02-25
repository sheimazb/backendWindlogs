package com.windlogs.authentication.dto.AuthDTO;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public class ChangePasswordRequest {
    @Email
    private String email;

    public @NotBlank String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(@NotBlank String currentPassword) {
        this.currentPassword = currentPassword;
    }

    @NotBlank
    private String currentPassword; // Add this field

    @NotBlank
    private String newPassword;

    public ChangePasswordRequest() {}

    public @Email String getEmail() {
        return email;
    }

    public void setEmail(@Email String email) {
        this.email = email;
    }

    public @NotBlank String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(@NotBlank String newPassword) {
        this.newPassword = newPassword;
    }
}