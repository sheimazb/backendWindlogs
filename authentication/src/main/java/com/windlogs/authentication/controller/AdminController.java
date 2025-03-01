package com.windlogs.authentication.controller;

import com.windlogs.authentication.dto.UserProfileDto.PartnerResponse;
import com.windlogs.authentication.entity.User;
import com.windlogs.authentication.service.AdminService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/admin")
public class AdminController {

    private final AdminService adminService;
    private static final Logger logger = LoggerFactory.getLogger(AdminController.class); // Fixed logger class reference

    @GetMapping("/partners")
    public ResponseEntity<List<PartnerResponse>> fetchPartnersList(
            @AuthenticationPrincipal UserDetails userDetails) {

        if (userDetails == null) {
            logger.error("UserDetails is null. Authentication might have failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<PartnerResponse> partners = adminService.fetchAllPartners();

        if (partners.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(partners);
    }

    @PostMapping("/edit-status/{email}")
    public ResponseEntity<User> editPartnerStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String email,
            @RequestBody PartnerResponse request) {

        if (userDetails == null) {
            logger.error("UserDetails is null. Authentication might have failed.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User updatedUser = adminService.editPartnerStatus(email, request);
        return ResponseEntity.ok(updatedUser);
    }

    @DeleteMapping("/delete/{email}")
    public ResponseEntity<Void> deletePartner(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String email) {

        if (userDetails == null) {
            logger.error("UserDetails is null.Authentication might have failed.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Received delete request for partner with email: {}", email);
        adminService.deletePartner(email);
        return ResponseEntity.noContent().build();
    }

}