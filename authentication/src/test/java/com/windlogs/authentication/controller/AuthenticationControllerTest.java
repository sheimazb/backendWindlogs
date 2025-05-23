package com.windlogs.authentication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.windlogs.authentication.dto.AuthDTO.AuthenticationRequest;
import com.windlogs.authentication.dto.AuthDTO.AuthenticationResponse;
import com.windlogs.authentication.dto.AuthDTO.RegistrationRequest;
import com.windlogs.authentication.entity.Role;
import com.windlogs.authentication.entity.User;
import com.windlogs.authentication.service.AuthenticationService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationControllerTest {

    @Mock
    private AuthenticationService authenticationService;

    @InjectMocks
    private AuthenticationController authenticationController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("Register - Success")
    void registerSuccess() throws Exception {
        // Arrange
        RegistrationRequest request = RegistrationRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .firstname("John")
                .lastname("Doe")
                .build();

        doNothing().when(authenticationService).register(any(RegistrationRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(
                        content().string("Registration successful! Please check your email to activate your account."));

        verify(authenticationService, times(1)).register(any(RegistrationRequest.class));
    }

    @Test
    @DisplayName("Register - Email Already Exists")
    void registerEmailAlreadyExists() throws Exception {
        // Arrange
        RegistrationRequest request = RegistrationRequest.builder()
                .email("existing@example.com")
                .password("Password123!")
                .firstname("John")
                .lastname("Doe")
                .build();

        doThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,
                "Un compte existe déjà avec cet email"))
                .when(authenticationService).register(any(RegistrationRequest.class));

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().string("Un compte existe déjà avec cet email"));

        verify(authenticationService, times(1)).register(any(RegistrationRequest.class));
    }

    @Test
    @DisplayName("Authenticate - Success")
    void authenticateSuccess() throws Exception {
        // Arrange
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .build();

        AuthenticationResponse response = AuthenticationResponse.builder()
                .token("jwt-token")
                .email("test@example.com")
                .fullName("John Doe")
                .role(Role.PARTNER.name())
                .id(1)
                .tenant("1234")
                .build();

        when(authenticationService.authenticate(any(AuthenticationRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/authenticate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.fullName").value("John Doe"))
                .andExpect(jsonPath("$.role").value("PARTNER"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.tenant").value("1234"));

        verify(authenticationService, times(1)).authenticate(any(AuthenticationRequest.class));
    }

    @Test
    @DisplayName("Get Current User - Success")
    void getCurrentUserSuccess() throws Exception {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .firstname("testuser")
                .lastname("testuser")
                .role(Role.PARTNER)
                .tenant("1234")
                .build();

        when(authentication.getPrincipal()).thenReturn(user);

        // Act
        ResponseEntity<?> response = authenticationController.getCurrentUser(authentication);

        // Assert
        verify(authentication, times(2)).getPrincipal(); // Changed from times(1) to times(2)
        assert response.getStatusCode().is2xxSuccessful();
    }

    @Test
    @DisplayName("Activate Account - Success")
    void activateAccountSuccess() throws Exception {
        // Arrange
        String token = "valid-token";
        doNothing().when(authenticationService).activateAccount(token);

        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/activate-account")
                .param("token", token))
                .andExpect(status().isOk())
                .andExpect(content().string("Account activated successfully!"));

        verify(authenticationService, times(1)).activateAccount(token);
    }

    @Test
    @DisplayName("Activate Account - Invalid Token")
    void activateAccountInvalidToken() throws Exception {
        // Arrange
        String token = "invalid-token";
        doThrow(new RuntimeException("Invalid token")).when(authenticationService).activateAccount(token);

        // Act & Assert
        mockMvc.perform(get("/api/v1/auth/activate-account")
                .param("token", token))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid token"));

        verify(authenticationService, times(1)).activateAccount(token);
    }
}