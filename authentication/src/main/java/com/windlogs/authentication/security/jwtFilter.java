package com.windlogs.authentication.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Component
@RequiredArgsConstructor
public class jwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private static final Logger logger = LoggerFactory.getLogger(jwtFilter.class);

    // List of paths that don't require authentication
    private static final List<String> PUBLIC_PATHS = Arrays.asList(
            "/api/v1/auth/register",
            "/api/v1/auth/authenticate",
            "/api/v1/auth/activate-account",
            "/api/v1/auth/forgot_password",
            "/api/v1/auth/reset_password",
            "/api/v1/auth/request-password-change",
            "/api/v1/auth/verify-and-change-password"
    );

    // Update filter to also check for paths that start with public URLs
    private boolean isPublicPath(String path) {
        if (PUBLIC_PATHS.stream().anyMatch(path::equals)) {
            return true;
        }
        // Check if the path starts with the public project endpoint
        return path.startsWith("/api/v1/projects/public/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String path = request.getServletPath();
            logger.debug("Processing request for path: {}", path);

            // Skip authentication for public paths
            if (isPublicPath(path)) {
                logger.debug("Skipping authentication for public path: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            final String authHeader = request.getHeader(AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("No valid authorization header found for protected path: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            final String jwt = authHeader.substring(7);
            final String userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    logger.debug("JWT token is valid for user: {}", userEmail);
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            logger.error("Error in JWT filter: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}