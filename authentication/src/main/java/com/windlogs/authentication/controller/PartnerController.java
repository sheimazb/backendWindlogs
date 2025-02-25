package com.windlogs.authentication.controller;

import com.windlogs.authentication.dto.EmployeeCreationRequest;
import com.windlogs.authentication.dto.ProjectDto.ProjectRequest;
import com.windlogs.authentication.entity.User;
import com.windlogs.authentication.service.PartnerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/employees")
@Tag(name = "Employee Management")
@RequiredArgsConstructor
public class PartnerController {
    private static final Logger logger = LoggerFactory.getLogger(PartnerController.class);

    private final PartnerService partnerService;

    @PostMapping("/create-staff")
    @PreAuthorize("hasAuthority('CREATE_STAFF')")
    public ResponseEntity<?> createEmployee(
            @RequestBody @Valid EmployeeCreationRequest request,
            Authentication authentication) {
        try {
            logger.info("Received employee creation request for email: {}", request.getEmail());
            User partner = (User) authentication.getPrincipal();
            partnerService.createEmployee(request, partner);
            return ResponseEntity.ok("Employee account created successfully! Credentials have been sent via email.");
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getReason());
        } catch (MessagingException e) {
            logger.error("Error sending credentials email", e);
            return ResponseEntity.internalServerError()
                    .body("Account created but failed to send credentials email. Please contact support.");
        } catch (Exception e) {
            logger.error("Error creating employee account", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to create employee account. Please try again.");
        }
    }

    @PostMapping("/create-project")
    @PreAuthorize("hasAuthority('CREATE_PROJECT')")
    public ResponseEntity<?> createProject(
            @RequestBody @Valid ProjectRequest request,
            Authentication authentication
    ) {
        try {
            logger.info("Received project creation request for project: {}", request.getName());
            User partner = (User) authentication.getPrincipal();
            partnerService.createProject(request, partner);
            return ResponseEntity.ok("Project created successfully!");
        } catch (ResponseStatusException e) {
            logger.error("Error creating project: {}", e.getReason());
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getReason());
        } catch (Exception e) {
            logger.error("Error creating project", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to create project. Please try again.");
        }
    }
}