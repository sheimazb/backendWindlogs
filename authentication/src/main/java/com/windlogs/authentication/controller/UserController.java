package com.windlogs.authentication.controller;

import com.windlogs.authentication.dto.UpdateProfileRequest;
import com.windlogs.authentication.service.UserService;
import com.windlogs.authentication.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);


    private final UserService userService;

    @PutMapping("/update-profile/{email}")
    public ResponseEntity<User> updateUserProfile(
            @PathVariable String email,
            @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        // Check if the authenticated user is updating their own profile
        if (!userDetails.getUsername().equals(email)) {
            logger.warn("Unauthorized attempt to update profile for email: {}", email);
            return ResponseEntity.status(403).build();
        }

        logger.info("Processing profile update request for email: {}", email);
        User updated = userService.updateUserProfileByEmail(email, request);
        return ResponseEntity.ok(updated);
    }
}