package com.windlogs.authentication.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class BeansConfig {
     private final UserDetailsService userDetailsService;
    /**
     *AuthenticationProvider it is an interface
     * to see which class or what are the classes
     * that implement this interface called authenticationprovider
     *
     * DoeAuthenticationProvider is one of the list founded in AuthenticationProvider
     * DAO : data access object
     * it has an attribute of the type password encoder and then a private object of userDetailsService(it"s an interface that has one methode
     * return a user details ) this is why when we created our user class we implemented the userDetails Interface
     * */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        //spring will check if the password or the row password provided matches the hashed  password
        //if doesn't match he will say that the passwords are not okay and user not will be able to authenticate
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
