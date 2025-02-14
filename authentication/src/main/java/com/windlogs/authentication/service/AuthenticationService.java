package com.windlogs.authentication.service;

import com.windlogs.authentication.dto.AuthenticationRequest;
import com.windlogs.authentication.dto.AuthenticationResponse;
import com.windlogs.authentication.dto.EmployeeCreationRequest;
import com.windlogs.authentication.dto.RegistrationRequest;
import com.windlogs.authentication.email.EmailService;
import com.windlogs.authentication.email.EmailTemplateName;
import com.windlogs.authentication.entity.Role;
import com.windlogs.authentication.security.JwtService;
import com.windlogs.authentication.entity.Token;
import com.windlogs.authentication.repository.TokenRepository;
import com.windlogs.authentication.entity.User;
import com.windlogs.authentication.repository.UserRepository;
import com.windlogs.authentication.entity.TokenType;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);


    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * first thing: need to assign the role by default (default role : user)
     * seconde: we need to create a user object and then save it
     * finally: we need to send a validation email ( we need to implement an email sender service )
     */

    //Partner register, Role.PARTNER par le rôle par défaut que tu veux assigner aux nouveaux utilisateurs.
    public void register(RegistrationRequest request) throws MessagingException {
        logger.info("Starting registration process for email: {}", request.getEmail());

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            logger.warn("Email already exists: {}", request.getEmail());
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Un compte existe déjà avec cet email"
            );
        }

        try {


            var user = User.builder()
                    .firstname(request.getFirstname())
                    .lastname(request.getLastname())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .accountLocked(false)
                    .enabled(false)
                    .role(Role.PARTNER) // ou un autre rôle par défaut
                    .build();

            logger.info("Saving new user: {}", user.getEmail());
            user = userRepository.save(user);

            logger.info("Sending validation email to: {}", user.getEmail());
            sendValidationEmail(user);

            logger.info("Registration completed successfully for: {}", user.getEmail());
        } catch (Exception e) {
            logger.error("Error during registration: {}", e.getMessage(), e);
            throw e;
        }
    }


    //sendValidationEmail
    private void sendValidationEmail(User user) throws MessagingException {
        Token token = generateAndSaveActivationToken(user);
        emailService.sendEmail(
                user.getEmail(),
                user.getFullName(),
                EmailTemplateName.ACTIVATE_ACCOUNT,
                activationUrl,
                token.getToken(),
                "Account activation"
        );
    }

    public Token generateAndSaveActivationToken(User user) {
        Token token = Token.builder()
                .token(generateActivationCode(6))
                .user(user)
                .tokenType(TokenType.ACTIVATION)
                .expired(false)
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        return tokenRepository.save(token);
    }

    private String generateActivationCode(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();
        for (int i = 0; i < length; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }
        return codeBuilder.toString();
    }


    public AuthenticationResponse authenticate(@Valid AuthenticationRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );
        var claims = new HashMap<String, Object>();
        var user = ((User) auth.getPrincipal());
        claims.put("fullName", user.getFullName());
        var jwtToken = jwtService.generateToken(claims, user);
        return AuthenticationResponse.builder()
                .token(jwtToken).build();
    }

    @Transactional
    public void activateAccount(String token) throws MessagingException {
        Token savedToken = tokenRepository.findByToken(token)
                // todo exception has to be defined
                .orElseThrow(() -> new RuntimeException("Invalid token"));
        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
            sendValidationEmail(savedToken.getUser());
            throw new RuntimeException("Activation token has expired. A new token has been send to the same email address");
        }

        var user = userRepository.findById(savedToken.getUser().getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);

        savedToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(savedToken);
    }

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
        if (request.getRole() != Role.TESTER && request.getRole() != Role.DEVELOPER) {
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
            var user = User.builder()
                    .firstname(request.getFirstname())
                    .lastname(request.getLastname())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(randomPassword))
                    .accountLocked(false)
                    .enabled(true)  // Employee accounts are enabled by default
                    .role(request.getRole())
                    .build();

            logger.info("Saving new employee: {}", user.getEmail());
            user = userRepository.save(user);

            // Generate JWT token for the new employee
            var claims = new HashMap<String, Object>();
            claims.put("fullName", user.getFullName());
            String token = jwtService.generateToken(claims, user);

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
}
