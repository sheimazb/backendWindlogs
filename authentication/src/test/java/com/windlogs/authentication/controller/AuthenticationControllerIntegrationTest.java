package com.windlogs.authentication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.windlogs.authentication.dto.AuthDTO.AuthenticationRequest;
import com.windlogs.authentication.dto.AuthDTO.AuthenticationResponse;
import com.windlogs.authentication.dto.AuthDTO.RegistrationRequest;
import com.windlogs.authentication.email.EmailService;
import com.windlogs.authentication.entity.Role;
import com.windlogs.authentication.entity.Token;
import com.windlogs.authentication.entity.TokenType;
import com.windlogs.authentication.entity.User;
import com.windlogs.authentication.repository.TokenRepository;
import com.windlogs.authentication.repository.UserRepository;
import com.windlogs.authentication.service.AuthenticationService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@SuppressWarnings("deprecation") // Suppress deprecation warnings for MockBean
public class AuthenticationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private TokenRepository tokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private AuthenticationService authenticationService;
    
    // Add these to ensure email functionality is mocked
    @MockitoBean
    private JavaMailSender javaMailSender;
    
    @MockitoBean
    private EmailService emailService;

    private User testUser;
    private Token testToken;

    @BeforeEach
    void setUp() throws MessagingException {
        // Create test user
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password(passwordEncoder.encode("Password123!"))
                .firstname("John")
                .lastname("Doe")
                .role(Role.PARTNER)
                .enabled(true)
                .accountLocked(false)
                .tenant("1234")
                .build();

        // Create test token
        testToken = Token.builder()
                .id(1L)
                .token("test-token")
                .user(testUser)
                .tokenType(TokenType.ACTIVATION)
                .expired(false)
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();
                
        // Set up common mock behaviors
        doNothing().when(emailService).sendEmail(anyString(), anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Register - Integration Test")
    void registerIntegrationTest() throws Exception {
        // Arrange
        RegistrationRequest request = RegistrationRequest.builder()
                .email("new@example.com")
                .password("Password123!")
                .firstname("Jane")
                .lastname("Doe")
                .build();

        doNothing().when(authenticationService).register(any(RegistrationRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Registration successful! Please check your email to activate your account."));
    }

    @Test
    @DisplayName("Authenticate - Integration Test")
    void authenticateIntegrationTest() throws Exception {
        // Arrange
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .build();

        // Create a mock response
        AuthenticationResponse authResponse = AuthenticationResponse.builder()
                .token("jwt-token")
                .email("test@example.com")
                .fullName("John Doe")
                .role(Role.PARTNER.name())
                .id(1)
                .tenant("1234")
                .build();

        // Mock the authentication service to return our response
        when(authenticationService.authenticate(any(AuthenticationRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/authenticate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.role").value("PARTNER"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.tenant").value("1234"));
    }

    @Test
    @DisplayName("Activate Account - Integration Test")
    void activateAccountIntegrationTest() throws Exception {
        // Arrange
        String token = "test-token";
        when(tokenRepository.findByToken(anyString())).thenReturn(Optional.of(testToken));
        when(userRepository.findById(any())).thenReturn(Optional.of(testUser));
        doNothing().when(authenticationService).activateAccount(anyString());

        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/activate-account")
                        .param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().string("Account activated successfully!"));
    }

    @Test
    @DisplayName("Forgot Password - Integration Test")
    void forgotPasswordIntegrationTest() throws Exception {
        // Arrange
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/forgot_password")
                        .param("email", email))
                .andExpect(status().isOk())
                .andExpect(content().string("We have sent a reset password link to your email."));
    }
} 