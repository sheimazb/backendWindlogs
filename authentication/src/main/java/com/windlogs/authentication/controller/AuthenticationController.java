package com.windlogs.authentication.controller;

import com.windlogs.authentication.dto.AuthDTO.*;
import com.windlogs.authentication.entity.Role;
import com.windlogs.authentication.entity.User;
import com.windlogs.authentication.service.AuthenticationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;

import com.windlogs.authentication.repository.UserRepository;
import com.windlogs.authentication.security.JwtService;

/**
 * REST controller for handling authentication-related operations such as registration,
 * login, account activation, password management, and user information retrieval.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
@RequiredArgsConstructor
public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    private final AuthenticationService authenticationService;
    private final SecureRandom secureRandom = new SecureRandom();
    private final UserRepository userRepository;
    private final JwtService jwtService;

    /**
     * Registers a new user based on the provided registration request.
     *
     * @param request the {@link RegistrationRequest} object containing user registration details (e.g., email, password)
     * @return a {@link ResponseEntity} with a success message if registration is successful,
     *         or an error message with an appropriate HTTP status code if it fails
     * @throws ResponseStatusException if the registration fails due to invalid input or business logic errors
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody @Valid RegistrationRequest request) {
        try {
            logger.info("Received registration request for email: {}", request.getEmail());
            authenticationService.register(request);
            return ResponseEntity.ok("Registration successful! Please check your email to activate your account.");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getReason());
        } catch (MessagingException e) {
            logger.error("Error sending email", e);
            return ResponseEntity.internalServerError()
                    .body("Registration successful but failed to send activation email. Please contact support.");
        }
    }

    /**
     * Authenticates a user and returns a JWT token upon successful login.
     *
     * @param request the {@link AuthenticationRequest} containing email and password
     * @return a {@link ResponseEntity} containing an {@link AuthenticationResponse} with the JWT token and user details
     */
    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody @Valid AuthenticationRequest request) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    /**
     * Retrieves information about the currently authenticated user.
     *
     * @param authentication the Spring Security {@link Authentication} object representing the current user
     * @return a {@link ResponseEntity} containing a {@link UserResponse} with user details, or 401 if not authenticated
     */
    @GetMapping("/user")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(401).build();
        }

        User user = (User) authentication.getPrincipal();
        logger.info("Getting current user: {}, tenant: {}", user.getEmail(), user.getTenant());
        
        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .tenant(user.getTenant())
                .build();
                
        return ResponseEntity.ok(userResponse);
    }

    /**
     * Activates a user account using the provided activation token.
     *
     * @param token the activation token received via email
     * @return a {@link ResponseEntity} with a success message if activation succeeds, or an error message if it fails
     * @throws MessagingException if a new email needs to be sent due to token expiration
     */
    @GetMapping("/activate-account")
    public ResponseEntity<String> confirm(@RequestParam String token) throws MessagingException {
        try {
            authenticationService.activateAccount(token);
            return ResponseEntity.ok("Account activated successfully!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Initiates a password reset process by generating a token and sending a reset email.
     *
     * @param email the email address of the user requesting a password reset
     * @return a {@link ResponseEntity} with a success message if the email is sent, or an error message if it fails
     */
    @PostMapping("/forgot_password")
    public ResponseEntity<String> processForgotPassword(@RequestParam String email) {
        try {
            // Generate a 6-digit token using SecureRandom
            String token = String.format("%06d", secureRandom.nextInt(1000000));
            authenticationService.updateResetPasswordToken(token, email);
            // Return success message
            return ResponseEntity.ok("We have sent a reset password link to your email.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Resets a user's password using the provided token and new password.
     *
     * @param token the reset token received via email
     * @param password the new password to set
     * @return a {@link ResponseEntity} with a success message if the password is reset, or an error message if it fails
     */
    @PostMapping("/reset_password")
    public ResponseEntity<String> processResetPassword(
            @RequestParam String token,
            @RequestParam String password) {
        try {
            var user = authenticationService.getByResetPasswordToken(token);
            if (user == null) {
                return ResponseEntity.badRequest().body("Invalid reset password token");
            }
            
            authenticationService.updatePassword(user, password);
            return ResponseEntity.ok("You have successfully changed your password. Please login with your new password.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Requests a password change by sending a verification code to the user's email.
     *
     * @param request the {@link ChangePasswordRequest} containing email and current password
     * @return a {@link ResponseEntity} with a success message if the code is sent
     * @throws MessagingException if there is an error sending the email
     */
    @PostMapping("/request-password-change")
    public ResponseEntity<String> requestPasswordChange(@RequestBody @Valid ChangePasswordRequest request) throws MessagingException {
        authenticationService.requestPasswordChange(request);
        return ResponseEntity.ok("Verification code sent to your email!");
    }

    /**
     * Verifies a password change token and updates the user's password.
     *
     * @param request the {@link VerifyChangePasswordRequest} containing the token and new password
     * @return a {@link ResponseEntity} with a success message if the password is changed
     */
    @PostMapping("/verify-and-change-password")
    public ResponseEntity<String> verifyAndChangePassword(@RequestBody @Valid VerifyChangePasswordRequest request) {
        authenticationService.verifyAndChangePassword(request);
        return ResponseEntity.ok("Password changed successfully!");
    }

    /**
     * Retrieves information about a user by their ID, with tenant-based access control.
     *
     * @param userId the ID of the user to retrieve
     * @param authentication the Spring Security {@link Authentication} object representing the current user
     * @return a {@link ResponseEntity} containing a {@link UserResponse} with user details, or an error status if unauthorized
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long userId,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            return ResponseEntity.status(401).build();
        }
        
        User authenticatedUser = (User) authentication.getPrincipal();
        logger.info("Getting user by ID: {}, requested by: {}", userId, authenticatedUser.getEmail());

        User user = userRepository.findById(userId)
                .orElse(null);
        
        if (user == null) {
            logger.warn("User not found with ID: {}", userId);
            return ResponseEntity.notFound().build();
        }

        if (!authenticatedUser.getTenant().equals(user.getTenant()) && 
            !authenticatedUser.getRole().equals(Role.ADMIN)) {
            logger.warn("Unauthorized attempt to access user information. Requester tenant: {}, Target tenant: {}", 
                    authenticatedUser.getTenant(), user.getTenant());
            return ResponseEntity.status(403).build();
        }
        
        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().name())
                .tenant(user.getTenant())
                .build();
        
        return ResponseEntity.ok(userResponse);
    }

    /**
     * Retrieves a list of users filtered by tenant and role.
     * This endpoint is used by the notification service to get managers for sending notifications.
     *
     * @param tenant the tenant identifier to filter by
     * @param role the role to filter by (e.g., "MANAGER")
     * @return a {@link ResponseEntity} containing a list of {@link UserResponse} objects, or an error status if it fails
     */
    @GetMapping("/users/tenant/{tenant}/role/{role}")
    public ResponseEntity<List<UserResponse>> getUsersByTenantAndRole(
            @PathVariable String tenant,
            @PathVariable String role) {
        
        logger.info("Getting users with tenant: {} and role: {}", tenant, role);
        
        try {
            Role roleEnum = Role.valueOf(role.toUpperCase());
            
            List<User> users = userRepository.findByTenantAndRoleIn(
                    tenant, 
                    List.of(roleEnum)
            );
            
            if (users.isEmpty()) {
                logger.info("No users found with tenant: {} and role: {}", tenant, role);
                return ResponseEntity.ok(List.of());
            }
            
            List<UserResponse> userResponses = users.stream()
                    .map(user -> UserResponse.builder()
                            .id(user.getId())
                            .username(user.getUsername())
                            .email(user.getEmail())
                            .role(user.getRole().name())
                            .tenant(user.getTenant())
                            .build())
                    .collect(Collectors.toList());
            
            logger.info("Found {} users with tenant: {} and role: {}", users.size(), tenant, role);
            return ResponseEntity.ok(userResponses);
        } catch (IllegalArgumentException e) {
            logger.error("Invalid role: {}", role, e);
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            logger.error("Error getting users by tenant and role", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Creates a service account token for a specified service and tenant, restricted to admin users.
     *
     * @param serviceName the name of the service requesting the token
     * @param tenant the tenant identifier for the service account
     * @return a {@link ResponseEntity} containing an {@link AuthenticationResponse} with the service token
     */
    @PostMapping("/service-account")
    public ResponseEntity<AuthenticationResponse> createServiceToken(
            @RequestParam String serviceName,
            @RequestParam String tenant) {
        logger.info("Service account token request for service: {}, tenant: {}", serviceName, tenant);

        var claims = new HashMap<String, Object>();
        claims.put("type", "SERVICE_ACCOUNT");
        claims.put("serviceName", serviceName);
        claims.put("tenant", tenant);
        claims.put("role", "SERVICE");
        
        String token = jwtService.generateServiceToken(claims);
        
        return ResponseEntity.ok(AuthenticationResponse.builder()
                .token(token)
                .role("SERVICE")
                .tenant(tenant)
                .build());
    }
}
