package com.windlogs.authentication.service;

import com.windlogs.authentication.dto.AuthDTO.*;
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

/**
 * Service class responsible for handling user authentication, registration, and account management.
 * Provides methods for user registration, authentication, account activation, and password management.
 */
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
     * Registers a new user with the provided registration details and sends an activation email.
     *
     * @param request the {@link RegistrationRequest} containing user details (e.g., email, password)
     * @throws ResponseStatusException if the email is already registered
     * @throws MessagingException if there is an error sending the activation email
     */
    @Transactional
    public void register(RegistrationRequest request) throws MessagingException {
        logger.info("Starting registration process for email: {}", request.getEmail());

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            logger.warn("Email already exists: {}", request.getEmail());
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email");
        }

        var user = User.builder()
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .accountLocked(true)
                .enabled(false)
                .role(Role.PARTNER)
                .tenant(generateRandomFourDigitNumber())
                .build();

        logger.info("Saving new user: {}", user.getEmail());
        user = userRepository.save(user);

        logger.info("Sending validation email to: {}", user.getEmail());
        sendValidationEmail(user);

        logger.info("Registration completed successfully for: {}", user.getEmail());
    }

    /**
     * Authenticates a user and generates a JWT token upon successful login.
     *
     * @param request the {@link AuthenticationRequest} containing email and password
     * @return an {@link AuthenticationResponse} containing the JWT token and user details
     * @throws ResponseStatusException if authentication fails or account is locked/disabled
     */
    public AuthenticationResponse authenticate(@Valid AuthenticationRequest request) {

        logger.info("Authentication attempt for user: {}", request.getEmail());

        try {
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            var user = (User) auth.getPrincipal();

            if (!user.isEnabled()) {
                logger.warn("Login attempt for disabled account: {}", request.getEmail());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account not activated.");
            }

            if (user.getRole() == Role.PARTNER && user.isAccountLocked()) {
                logger.warn("Login attempt for locked partner account: {}", request.getEmail());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Your account is locked. Please wait for admin approval.");
            }

            if (user.isAccountLocked()) {
                logger.warn("Login attempt for locked account: {}", request.getEmail());
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is locked. Please contact your administrator.");
            }

            var claims = new HashMap<String, Object>();
            claims.put("fullName", user.fullName());
            claims.put("role", user.getRole().name());
            claims.put("id",user.getId().intValue());
            claims.put("tenant",user.getTenant());
            if (user.getRole() == Role.DEVELOPER || user.getRole() == Role.TESTER || user.getRole() == Role.MANAGER) {
                claims.put("employeeType", user.getRole().name().toLowerCase());
            }

            var jwtToken = jwtService.generateToken(claims, user);

            logger.info("Successfully authenticated user: {}", request.getEmail());

            return AuthenticationResponse.builder()
                    .token(jwtToken)
                    .email(user.getEmail())
                    .fullName(user.getFullName())
                    .role(user.getRole().name())
                    .id(user.getId().intValue())
                    .tenant(user.getTenant())
                    .build();

        } catch (BadCredentialsException e) {
            logger.warn("Failed login attempt for user: {}", request.getEmail());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"
            );
        }
    }

    /**
     * Sends an account activation email to the specified user with a unique token.
     *
     * @param user the {@link User} to send the activation email to
     * @throws MessagingException if there is an error sending the email
     */
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

    /**
     * Generates and saves an activation token for the specified user.
     *
     * @param user the {@link User} to generate the token for
     * @return the generated {@link Token} object
     */
    public Token generateAndSaveActivationToken(User user) {
        Token token = Token.builder()
                .token(generateActivationCode())
                .user(user)
                .tokenType(TokenType.ACTIVATION)
                .expired(false)
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        return tokenRepository.save(token);
    }

    /**
     * Generates a random activation code of the specified length.
     *
     * @return a random alphanumeric string of the specified length
     */
    private String generateActivationCode() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder codeBuilder = new StringBuilder();
        SecureRandom secureRandom = new SecureRandom();
        for (int i = 0; i < 6; i++) {
            int randomIndex = secureRandom.nextInt(characters.length());
            codeBuilder.append(characters.charAt(randomIndex));
        }
        return codeBuilder.toString();
    }

    /**
     * Activates a user account using the provided activation token.
     *
     * @param token the activation token to validate
     * @throws MessagingException if a new email needs to be sent due to token expiration
     * @throws RuntimeException if the token is invalid or expired
     */
    @Transactional
    public void activateAccount(String token) throws MessagingException {

        Token savedToken = tokenRepository.findByToken(token).orElseThrow(() -> new RuntimeException("Invalid token"));

        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
            sendValidationEmail(savedToken.getUser());
            throw new RuntimeException("Activation token has expired. A new token has been send to the same email address");
        }

        var user = userRepository.findById(savedToken.getUser().getId()).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);

        savedToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(savedToken);
    }

    /**
     * Generates and saves a password reset token for the specified email and sends a reset email.
     *
     * @param token the reset token to associate with the user
     * @param email the email of the user requesting a password reset
     * @throws MessagingException if there is an error sending the reset email
     * @throws RuntimeException if no user is found with the provided email
     */
    public void updateResetPasswordToken(String token, String email) throws MessagingException {
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Could not find any User with the email " + email));
        
        user.setResetPasswordToken(token);
        userRepository.save(user);
        
        emailService.sendEmail(
            user.getEmail(),
            user.getFullName(),
            EmailTemplateName.RESET_PASSWORD,
            activationUrl + "/reset-password",
            token,
            "Reset Password Request"
        );
    }

    /**
     * Retrieves a user by their password reset token.
     *
     * @param token the reset token to look up
     * @return the {@link User} associated with the token
     * @throws RuntimeException if the token is invalid
     */
    public User getByResetPasswordToken(String token) {
        return userRepository.findByResetPasswordToken(token)
            .orElseThrow(() -> new RuntimeException("Invalid reset password token"));
    }

    /**
     * Updates the user's password and clears the reset token.
     *
     * @param user the {@link User} whose password is to be updated
     * @param newPassword the new password to set
     */
    public void updatePassword(User user, String newPassword) {
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPassword(encodedPassword);

        user.setResetPasswordToken(null);
        userRepository.save(user);
    }

    /**
     * Initiates a password change request by verifying the current password and sending a token.
     *
     * @param request the {@link ChangePasswordRequest} containing email and current password
     * @throws MessagingException if there is an error sending the email
     * @throws ResponseStatusException if the user is not found or the current password is incorrect
     */
    public void requestPasswordChange(ChangePasswordRequest request) throws MessagingException {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Current password is incorrect");
        }

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

    /**
     * Verifies a password change token and updates the user's password.
     *
     * @param request the {@link VerifyChangePasswordRequest} containing the token and new password
     * @throws ResponseStatusException if the token is invalid or expired
     */
    public void verifyAndChangePassword(VerifyChangePasswordRequest request) {
        Token token = tokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid token"));

        if (LocalDateTime.now().isAfter(token.getExpiresAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token has expired");
        }

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        token.setRevoked(true);
        tokenRepository.save(token);
    }

    /**
     * Validates an admin JWT token from the authorization header and returns the associated user.
     *
     * @param authorizationHeader the HTTP Authorization header containing the JWT token
     * @return the {@link User} if the token is valid and the user is an admin
     * @throws ResponseStatusException if the token is invalid, the user is not found, or not an admin
     */
    public User validateAdminToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authorization header");
        }

        String token = authorizationHeader.substring(7);
        String userEmail = jwtService.extractUsername(token);
        
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
        
        if (user.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only admins can create service accounts");
        }
        
        return user;
    }

    /**
     * Generates a random four-digit number for use as a tenant identifier.
     *
     * @return a four-digit string between 1000 and 9999
     */
    private String generateRandomFourDigitNumber() {
        SecureRandom random = new SecureRandom();
        int number = 1000 + random.nextInt(9000);
        return String.valueOf(number);
    }

}
