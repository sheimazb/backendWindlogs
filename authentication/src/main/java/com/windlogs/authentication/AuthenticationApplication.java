package com.windlogs.authentication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableJpaAuditing // if you don't add this one the @EntityListeners annotation at the user Entity will not be working
@EnableAsync
@EntityScan(basePackages = "com.windlogs.authentication.entity") // Spécifie où chercher les entités
@EnableJpaRepositories(basePackages = "com.windlogs.authentication.repository")
public class AuthenticationApplication {
	public static void main(String[] args) {
		SpringApplication.run(AuthenticationApplication.class, args);
	}
}
