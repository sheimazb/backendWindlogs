package com.windlogs.authentication.controller;

import com.windlogs.authentication.dto.UpdateProfileRequest;
import com.windlogs.authentication.service.UserService;
import com.windlogs.authentication.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);


    private final UserService userService;

    @PostMapping(value = "/update-profile/{email}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<User> updateUserProfile(
            @PathVariable String email,
            @ModelAttribute UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) throws IOException {

        if (userDetails == null) {
            logger.error("UserDetails is null. Authentication might have failed.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Authenticated email: {}", userDetails.getUsername());
        logger.info("Path email: {}", email);

        if (!userDetails.getUsername().equals(email)) {
            logger.warn("Unauthorized attempt to update profile for email: {}", email);
            return ResponseEntity.status(403).build();
        }

        logger.info("Processing profile update request for email: {}", email);
        User updated = userService.updateUserProfileByEmail(email, request);
        return ResponseEntity.ok(updated);
    }

}