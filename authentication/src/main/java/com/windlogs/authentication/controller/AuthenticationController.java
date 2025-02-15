package com.windlogs.authentication.controller;

import com.windlogs.authentication.dto.AuthenticationRequest;
import com.windlogs.authentication.dto.AuthenticationResponse;
import com.windlogs.authentication.service.AuthenticationService;
import com.windlogs.authentication.dto.RegistrationRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication")
@RequiredArgsConstructor
public class AuthenticationController {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    private final AuthenticationService authenticationService;

    /**
     * This is the controller for the Partner role to register
     * in our application.

     * It includes the `register` function to create a new account,
     * and the `activateAccount` function for partner account validation.

     * After validation and email verification, the partner's account
     * is successfully created, granting them permission to authenticate.
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

    @PostMapping("/authenticate")
    public ResponseEntity<AuthenticationResponse> authenticate(
            @RequestBody @Valid AuthenticationRequest request) {
        return ResponseEntity.ok(authenticationService.authenticate(request));
    }

    @GetMapping("/activate-account")
    public ResponseEntity<String> confirm(@RequestParam String token) throws MessagingException {
        try {
            authenticationService.activateAccount(token);
            return ResponseEntity.ok("Account activated successfully!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
