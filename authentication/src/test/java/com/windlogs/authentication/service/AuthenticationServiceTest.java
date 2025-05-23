package com.windlogs.authentication.service;

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
import com.windlogs.authentication.security.JwtService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthenticationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenRepository tokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthenticationService authenticationService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    @Captor
    private ArgumentCaptor<Token> tokenCaptor;

    @BeforeEach
    void setUp() {
        // Set private field value using reflection
        ReflectionTestUtils.setField(authenticationService, "activationUrl", "http://localhost:3000/activate");
    }

    @Test
    @DisplayName("Register - Success")
    void registerSuccess() throws MessagingException {
        // Arrange
        RegistrationRequest request = RegistrationRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .firstname("John")
                .lastname("Doe")
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User savedUser = invocation.getArgument(0);
            savedUser.setId(1L);
            return savedUser;
        });

        // Act
        authenticationService.register(request);

        // Assert
        verify(userRepository).findByEmail("test@example.com");
        verify(passwordEncoder).encode("Password123!");
        verify(userRepository).save(userCaptor.capture());
        verify(tokenRepository).save(any(Token.class));
        verify(emailService).sendEmail(anyString(), anyString(), any(), anyString(), anyString(), anyString());

        User savedUser = userCaptor.getValue();
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals("encodedPassword", savedUser.getPassword());
        assertEquals("John", savedUser.getFirstname());
        assertEquals("Doe", savedUser.getLastname());
        assertEquals(Role.PARTNER, savedUser.getRole());
        assertTrue(savedUser.isAccountLocked());
        assertFalse(savedUser.isEnabled());
    }

    @Test
    @DisplayName("Register - Email Already Exists")
    void registerEmailAlreadyExists() {
        // Arrange
        RegistrationRequest request = RegistrationRequest.builder()
                .email("existing@example.com")
                .password("Password123!")
                .firstname("John")
                .lastname("Doe")
                .build();

        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(new User()));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authenticationService.register(request));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("Un compte existe déjà avec cet email", exception.getReason());
        verify(userRepository).findByEmail("existing@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Authenticate - Success")
    void authenticateSuccess() {
        // Arrange
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .build();

        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .firstname("John")
                .lastname("Doe")
                .enabled(true)
                .accountLocked(false)
                .role(Role.PARTNER)
                .tenant("1234")
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);
        when(jwtService.generateToken(anyMap(), any(User.class))).thenReturn("jwt-token");

        // Act
        AuthenticationResponse response = authenticationService.authenticate(request);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("John Doe", response.getFullName());
        assertEquals("PARTNER", response.getRole());
        assertEquals(1, response.getId());
        assertEquals("1234", response.getTenant());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(anyMap(), eq(user));
    }

    @Test
    @DisplayName("Authenticate - Account Not Enabled")
    void authenticateAccountNotEnabled() {
        // Arrange
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("test@example.com")
                .password("Password123!")
                .build();

        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .firstname("John")
                .lastname("Doe")
                .enabled(false)
                .accountLocked(false)
                .role(Role.PARTNER)
                .build();

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(authentication);

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authenticationService.authenticate(request));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Account not activated.", exception.getReason());
    }

    @Test
    @DisplayName("Authenticate - Bad Credentials")
    void authenticateBadCredentials() {
        // Arrange
        AuthenticationRequest request = AuthenticationRequest.builder()
                .email("test@example.com")
                .password("WrongPassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // Act & Assert
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> authenticationService.authenticate(request));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertEquals("Invalid email or password", exception.getReason());
    }

    @Test
    @DisplayName("Activate Account - Success")
    void activateAccountSuccess() throws MessagingException {
        // Arrange
        String token = "valid-token";
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .enabled(false)
                .build();

        Token activationToken = Token.builder()
                .id(1L)
                .token(token)
                .user(user)
                .tokenType(TokenType.ACTIVATION)
                .expired(false)
                .revoked(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(activationToken));
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(tokenRepository.save(any(Token.class))).thenReturn(activationToken);

        // Act
        authenticationService.activateAccount(token);

        // Assert
        verify(tokenRepository).findByToken(token);
        verify(userRepository).findById(user.getId());
        verify(userRepository).save(userCaptor.capture());
        verify(tokenRepository).save(tokenCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertTrue(savedUser.isEnabled());

        Token savedToken = tokenCaptor.getValue();
        assertNotNull(savedToken.getValidatedAt());
    }

    @Test
    @DisplayName("Activate Account - Invalid Token")
    void activateAccountInvalidToken() {
        // Arrange
        String token = "invalid-token";
        when(tokenRepository.findByToken(token)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authenticationService.activateAccount(token));

        assertEquals("Invalid token", exception.getMessage());
        verify(tokenRepository).findByToken(token);
        verify(userRepository, never()).findById(anyLong());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Activate Account - Expired Token")
    void activateAccountExpiredToken() throws MessagingException {
        // Arrange
        String token = "expired-token";
        User user = User.builder()
                .id(1L)
                .email("test@example.com")
                .enabled(false)
                .build();

        Token activationToken = Token.builder()
                .id(1L)
                .token(token)
                .user(user)
                .tokenType(TokenType.ACTIVATION)
                .expired(false)
                .revoked(false)
                .createdAt(LocalDateTime.now().minusHours(1))
                .expiresAt(LocalDateTime.now().minusMinutes(45))
                .build();

        when(tokenRepository.findByToken(token)).thenReturn(Optional.of(activationToken));
        doNothing().when(emailService).sendEmail(anyString(), anyString(), any(), anyString(), anyString(), anyString());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> authenticationService.activateAccount(token));

        assertEquals("Activation token has expired. A new token has been send to the same email address", exception.getMessage());
        verify(tokenRepository).findByToken(token);
        verify(tokenRepository).save(any(Token.class));
        verify(emailService).sendEmail(anyString(), anyString(), any(), anyString(), anyString(), anyString());
    }
} 