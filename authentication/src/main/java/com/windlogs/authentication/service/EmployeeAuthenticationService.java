package com.windlogs.authentication.service;


import com.windlogs.authentication.dto.AuthenticationRequest;
import com.windlogs.authentication.dto.EmployeeAuthResponse;
import com.windlogs.authentication.entity.Role;
import com.windlogs.authentication.entity.User;
import com.windlogs.authentication.security.JwtService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class EmployeeAuthenticationService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public  EmployeeAuthResponse userAuth(@Valid AuthenticationRequest request) {
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
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Account not activated. Please check your email for activation instructions."
                );
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
            claims.put("fullName", user.getFullName());
            claims.put("role", user.getRole().name());

            // For employees, add additional claims if needed
            if (user.getRole() == Role.DEVELOPER || user.getRole() == Role.TESTER) {
                claims.put("employeeType", user.getRole().name().toLowerCase());
            }

            var jwtToken = jwtService.generateToken(claims, user);

            logger.info("Successfully authenticated user: {}", request.getEmail());

            return EmployeeAuthResponse.builder()
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

}
