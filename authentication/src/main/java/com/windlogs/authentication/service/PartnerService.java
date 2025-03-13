package com.windlogs.authentication.service;


import com.windlogs.authentication.dto.EmployeeCreationRequest;
import com.windlogs.authentication.email.EmailService;
import com.windlogs.authentication.entity.Role;
import com.windlogs.authentication.entity.User;
import com.windlogs.authentication.repository.UserRepository;
import com.windlogs.authentication.security.JwtService;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PartnerService {

    private static final Logger logger = LoggerFactory.getLogger(PartnerService.class);
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;




    public void createEmployee(EmployeeCreationRequest request, User partner) throws MessagingException {
        logger.info("Starting employee creation process for email: {}", request.getEmail());

        // Verify that the requesting user is a PARTNER
        if (partner.getRole() != Role.PARTNER) {
            logger.warn("Unauthorized attempt to create employee by non-partner user: {}", partner.getEmail());
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only partners can create employee accounts"
            );
        }

        // Verify that the requested role is either TESTER or DEVELOPER
        if (request.getRole() != Role.TESTER && request.getRole() != Role.DEVELOPER && request.getRole() != Role.MANAGER) {
            logger.warn("Invalid role requested: {}", request.getRole());
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only TESTER or DEVELOPER roles are allowed"
            );
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            logger.warn("Email already exists: {}", request.getEmail());
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An account already exists with this email"
            );
        }

        // Generate a random password
        String randomPassword = generateRandomPassword();

        try {
            // Create the employee with the partner's tenant
            var user = User.builder()
                    .firstname(request.getFirstname())
                    .lastname(request.getLastname())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(randomPassword))
                    .accountLocked(false)
                    .enabled(true)  // Employee accounts are enabled by default
                    .role(request.getRole())
                    .tenant(partner.getTenant()) // Use partner's tenant
                    .build();

            logger.info("Saving new employee: {}", user.getEmail());
            user = userRepository.save(user);

            // Generate JWT token for the new employee
            var claims = new HashMap<String, Object>();
            claims.put("fullName", user.fullName());
            claims.put("role", user.getRole().name());
            claims.put("tenant", user.getTenant()); // Include tenant in claims

            // Send credentials via email
            sendEmployeeCredentials(user, randomPassword, partner);

            logger.info("Employee creation completed successfully for: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Error during employee creation: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void sendEmployeeCredentials(User employee, String password, User partner) throws MessagingException {
        Map<String, String> variables = new HashMap<>();
        variables.put("fullName", employee.getFullName());
        variables.put("email", employee.getEmail());
        variables.put("password", password);
        variables.put("role", employee.getRole().toString());
        variables.put("partnerName", partner.getFullName());

        emailService.sendEmployeeCredentialsEmail(
                employee.getEmail(),
                "Your WindLogs Account Credentials",
                variables
        );
    }

    private String generateRandomPassword() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();

        for (int i = 0; i < 12; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            password.append(characters.charAt(randomIndex));
        }

        return password.toString();
    }

    public List<User> getEmployeesByPartnerTenant(User partner) {
        logger.info("fetching employees for partner : {}", partner.getEmail());

        //ensure the requesting user have a partner role
        if (partner.getRole() != Role.PARTNER) {
            logger.warn("Unauthorized attempt to fetch employees by non-partner user: {}", partner.getEmail());
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "only partners can get employee accounts"
            );
        }

        //fetch list employees who belong to the same tenant as the partner
        List<User> employees = userRepository.findByTenantAndRoleIn(
                partner.getTenant(),
                List.of(Role.TESTER, Role.DEVELOPER, Role.MANAGER)
        );
        logger.info("found {} employees for partner {}", employees.size(), partner.getEmail());
        return employees;

    }

    public User getEmployeeById(Long id, User partner) {
        return userRepository.findById(id)
                .filter(user -> user.getTenant().equals(partner.getTenant())) // Ensure it's within the same tenant
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
    }

}
