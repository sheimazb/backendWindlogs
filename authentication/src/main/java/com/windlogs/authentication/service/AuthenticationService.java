package com.windlogs.authentication.service;

import com.windlogs.authentication.dto.*;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

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
                    .tenant(UUID.randomUUID().toString())
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
        logger.info("Authentication attempt for user: {}", request.getEmail());

        try {
            // First attempt authentication
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            var user = (User) auth.getPrincipal();

            // Check if account is enabled
            if (!user.isEnabled()) {
                logger.warn("Login attempt for disabled account: {}", request.getEmail());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account not activated.");
            }

            // Check if account is locked
            if (user.isAccountLocked()) {
                logger.warn("Login attempt for locked account: {}", request.getEmail());
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Account is locked. Please contact your administrator."
                );
            }

            // Add role-specific claims to JWT
            var claims = new HashMap<String, Object>();
            claims.put("fullName", user.fullName());
            claims.put("role", user.getRole().name());
            
            // For employees, add additional claims if needed
            if (user.getRole() == Role.DEVELOPER || user.getRole() == Role.TESTER) {
                claims.put("employeeType", user.getRole().name().toLowerCase());
            }

            var jwtToken = jwtService.generateToken(claims, user);

            logger.info("Successfully authenticated user: {}", request.getEmail());

            return AuthenticationResponse.builder()
                    .token(jwtToken)
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole().name())
                    .build();

        } catch (BadCredentialsException e) {
            logger.warn("Failed login attempt for user: {}", request.getEmail());
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid email or password"
            );
        }
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

    //forgot password features
    public void updateResetPasswordToken(String token, String email) throws MessagingException {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Could not find any User with the email " + email));
        
        user.setResetPasswordToken(token);
        userRepository.save(user);
        
        // Send password reset email
        emailService.sendEmail(
            user.getEmail(),
            user.getFullName(),
            EmailTemplateName.RESET_PASSWORD,
            activationUrl + "/reset-password",
            token,
            "Reset Password Request"
        );
    }

    public User getByResetPasswordToken(String token) {
        return userRepository.findByResetPasswordToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid reset password token"));
    }

    public void updatePassword(User user, String newPassword) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);

        user.setResetPasswordToken(null);
        userRepository.save(user);
    }

    public void requestPasswordChange(ChangePasswordRequest request) throws MessagingException {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // Verify the current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }

        // Generate a token and send it
        Token token = generateAndSaveActivationToken(user);
        emailService.sendEmail(
                user.getEmail(),
                user.getFullName(),
                EmailTemplateName.RESET_PASSWORD,
                "http://your-frontend-url/reset-password?token=" + token.getToken(),
                token.getToken(),
                "Password Change Request"
        );
    }
    public void verifyAndChangePassword(VerifyChangePasswordRequest request) {
        Token token = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"));

        if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token has expired");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword())); // Assuming you have a password encoder
        userRepository.save(user);

        // Optionally, invalidate the token after use
        token.setRevoked(true);
        tokenRepository.save(token);
    }

}
