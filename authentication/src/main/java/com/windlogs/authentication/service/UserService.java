package com.windlogs.authentication.service;

import com.windlogs.authentication.dto.UpdateProfileRequest;
import com.windlogs.authentication.entity.User;
import com.windlogs.authentication.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    public User updateUserProfileByEmail(String email, UpdateProfileRequest request) {
        logger.info("Updating profile for user with email: {}", email);

        User existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found for email: " + email
                ));

        // Only update fields that are not null in the request
        if (request.getFirstname() != null) {
            existingUser.setFirstname(request.getFirstname());
        }
        if (request.getLastname() != null) {
            existingUser.setLastname(request.getLastname());
        }
        if (request.getImage() != null) {
            existingUser.setImage(request.getImage());
        }
        // Only update email if it's different and not null
        if (request.getEmail() != null && !request.getEmail().equals(email)) {
            // Check if new email is already taken
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Email already in use: " + request.getEmail()
                );
            }
            existingUser.setEmail(request.getEmail());
        }

        logger.info("Saving updated profile for user: {}", email);
        return userRepository.save(existingUser);
    }
}