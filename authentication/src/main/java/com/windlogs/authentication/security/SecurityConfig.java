package com.windlogs.authentication.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

@Configuration
@EnableWebSecurity // Active Spring Security
@RequiredArgsConstructor //to create a constructor with all the finale and private fields
@EnableMethodSecurity(securedEnabled = true) // since we spoke about roles so we must enable security methode
public class SecurityConfig {

    private final jwtFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;
    private final CorsConfigurationSource corsConfigurationSource;
    @Bean // spring boot when it sees that there are a beans it know that this class need to configure and put in the context
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception  {
        http

                .cors(cors -> cors.configurationSource(corsConfigurationSource)) // Explicitly setting CORS
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(req -> req
                        .requestMatchers("/api/v1/auth/register", 
                                       "/api/v1/auth/authenticate", 
                                       "/api/v1/auth/activate-account",
                                       "/api/v1/auth/forgot_password",
                                       "/api/v1/auth/reset_password").permitAll()
                        .requestMatchers("/api/v1/employees/create-staff").hasAuthority("CREATE_STAFF")
                        .requestMatchers("/api/v1/employees/create-project").hasAuthority("CREATE_PROJECT")
                        .requestMatchers(
                                "/api/v1/auth/change-password",
                                "/api/v1/users/update-profile/**",
                                "/api/v1/admin/**"
                        ).authenticated()


                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(STATELESS)) //means that spring should not store the session stateù
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        //we build our http object with all the configurations
        return http.build();
    }
}
