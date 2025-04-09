package com.windlogs.authentication.controller;

import com.windlogs.authentication.dto.EmployeeCreationRequest;
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

/**
 * REST controller for handling staff's accounts,
 * create staff account, get staff by partner tenant, get staff by id.
 */
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

    @GetMapping("/my-staff")
    @PreAuthorize("hasAuthority('VIEW_STAFF')")
    public ResponseEntity<?> getEmployeesByPartnerTenant(Authentication authentication) {
        try {
            logger.info("Received request to fetch employees for authenticated partner");
            User partner = (User) authentication.getPrincipal();
            return ResponseEntity.ok(partnerService.getEmployeesByPartnerTenant(partner));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (Exception e) {
            logger.error("Error retrieving employees", e);
            return ResponseEntity.internalServerError().body("Failed to retrieve employees. Please try again.");
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_STAFF')")
    public ResponseEntity<?> getEmployeeById(@PathVariable Long id, Authentication authentication) {
        try {
            User partner = (User) authentication.getPrincipal();
            return ResponseEntity.ok(partnerService.getEmployeeById(id, partner));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode())
                    .body(e.getReason());
        } catch (Exception e) {
            logger.error("Error retrieving employee", e);
            return ResponseEntity.internalServerError()
                    .body("Failed to fetch employee details. Please try again.");
        }
    }
}