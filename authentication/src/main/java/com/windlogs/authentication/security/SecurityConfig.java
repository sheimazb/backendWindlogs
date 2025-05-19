package com.windlogs.authentication.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.http.HttpMethod;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {

    private final jwtFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final LogoutHandler logoutHandler;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Simply enable CORS with default configuration - will use Spring MVC's CORS
                .cors(cors -> cors.configure(http))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req -> req
                        .requestMatchers(
                                "/api/v1/auth/register",
                                "/api/v1/auth/authenticate",
                                "/api/v1/auth/activate-account",
                                "/api/v1/auth/forgot_password",
                                "/api/v1/auth/reset_password",
                                "/api/v1/auth/request-password-change",
                                "/api/v1/auth/verify-and-change-password",
                                "/api/v1/projects/public/by-tag/**",
                                "/api/v1/projects/public/{projectId}/members",
                                "/api/v1/users/id/{id}",
                                "/api/v1/auth/stats",
                                "/ws/**"
                        ).permitAll()
                        .requestMatchers("/api/v1/employees/create-staff").hasAuthority("CREATE_STAFF")
                        .requestMatchers("/api/v1/employees/my-staff",
                                "/api/v1/employees/{id}"
                        ).hasAuthority("VIEW_STAFF")
                        .requestMatchers("/api/v1/projects/create").hasAuthority("CREATE_PROJECT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/projects/{projectId}/users/{userId}").hasAnyAuthority("CREATE_PROJECT", "CREATE_STAFF")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/projects/{projectId}/users/{userId}").hasAnyAuthority("CREATE_PROJECT", "CREATE_STAFF")
                        .requestMatchers("/api/v1/projects/{id}/**").authenticated()
                        .requestMatchers("/api/v1/projects/search").authenticated()
                        .requestMatchers("/api/v1/projects/user/{userId}").authenticated()
                        .requestMatchers(
                                "/api/v1/auth/change-password",
                                "/api/v1/users/update-profile/**",
                                "/api/v1/admin/**"
                        ).authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .addLogoutHandler(logoutHandler)
                        .logoutSuccessHandler((request, response, authentication) ->
                                SecurityContextHolder.clearContext())
                );

        return http.build();
    }
}