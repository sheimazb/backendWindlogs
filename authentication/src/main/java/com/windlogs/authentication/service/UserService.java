package com.windlogs.authentication.service;

import com.windlogs.authentication.dto.UserProfileDto.ProfileRequest;
import com.windlogs.authentication.dto.UserProfileDto.ProfileResponse;
import com.windlogs.authentication.entity.User;
import com.windlogs.authentication.repository.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.Optional;

@Service
@AllArgsConstructor
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);


    private final UserRepository userRepository;

    public User updateUserProfileByEmail(String email, ProfileRequest request) throws IOException {
        logger.info("Updating profile for user with email: {}", email);
        String uploadDir = "public/images/";
        User existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found for email: " + email
                ));
        if (existingUser.getImage() != null && !existingUser.getImage().isEmpty()) {
            // If the stored image is not a URL, then try to delete it locally.
            if (!existingUser.getImage().startsWith("http://") && !existingUser.getImage().startsWith("https://")) {
                Path oldImagePath = Paths.get(uploadDir + existingUser.getImage());
                try {
                    Files.delete(oldImagePath);
                } catch (Exception e) {
                    logger.error("Failed to delete old image: {}", e.getMessage());
                }
            } else {
                logger.info("Existing image is a URL; skipping file deletion.");
            }
        }


        // Process new image
        MultipartFile image = request.getImage();
        Date createdDate = new Date();
        String storageFileName = createdDate.getTime() + "_" + image.getOriginalFilename();

        try (InputStream inputStream = image.getInputStream()) {
            // Create directory if it doesn't exist
            Path uploadPath = Paths.get(uploadDir);
            Files.createDirectories(uploadPath);

            // Use Paths.get(uploadDir, storageFileName) for better path handling
            Path destination = Paths.get(uploadDir, storageFileName);
            logger.info("Saving image to: {}", destination.toAbsolutePath().toString());

            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            existingUser.setImage("http://localhost:8222/images" + '/' + storageFileName);
        } catch (Exception e) {
            logger.error("Failed to save new image: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to save image: " + e.getMessage());
        }


        // Only update fields that are not null in the request
        if (request.getFirstname() != null) {
            existingUser.setFirstname(request.getFirstname());
        }
        if (request.getLastname() != null) {
            existingUser.setLastname(request.getLastname());
        }

        if (request.getBio() != null) {
            existingUser.setBio(request.getBio());
        }
        if (request.getPhone() != null) {
            existingUser.setPhone(request.getPhone());
        }
        if (request.getLocation() != null) {
            existingUser.setLocation(request.getLocation());
        }
        if (request.getCompany() != null) {
            existingUser.setCompany(request.getCompany());
        }
        if (request.getPronouns() != null) {
            existingUser.setPronouns(request.getPronouns());
        }
        if (request.getLien() != null) {
            existingUser.setLien(request.getLien());
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

    public Optional<ProfileResponse> getUserDetailsByEmail(String email) {
        return userRepository.findByEmail(email).map(this::convertToProfileResponse);
    }

    public Optional<ProfileResponse> getUserById(Long id){
        return userRepository.findById(id).map(this::convertToProfileResponse);
    }

    private ProfileResponse convertToProfileResponse(User user) {
        return ProfileResponse.builder()
                .firstname(user.getFirstname())
                .lastname(user.getLastname())
                .email(user.getEmail())
                .image(user.getImage())
                .role(user.getRole())
                .bio(user.getBio())
                .phone(user.getPhone())
                .location(user.getLocation())
                .company(user.getCompany())
                .pronouns(user.getPronouns())
                .lien(user.getLien())
                .build();
    }


}
