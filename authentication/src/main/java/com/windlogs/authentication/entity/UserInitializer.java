package com.windlogs.authentication.entity;

import com.windlogs.authentication.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.util.Optional;
import java.util.UUID;

@Component
public class UserInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    public UserInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        Optional<User> existingAdmin = userRepository.findByEmail("admin@example.com");

        if (existingAdmin.isEmpty()) {  // Fix: Using isEmpty() instead of ispresent()
            User admin = User.builder()
                    .firstname("Admin")  // Ensure your User entity has this field
                    .lastname("User")
                    .email("admin@example.com")
                    .password(passwordEncoder.encode("adminpassword"))
                    .enabled(true)
                    .accountLocked(false)
                    .role(Role.ADMIN)  // Ensure Role enum is correctly stored in User entity
                    .tenant(UUID.randomUUID().toString())
                    .build();

            userRepository.save(admin);
            System.out.println("Admin user created successfully!");
        } else {
            System.out.println("Admin user already exists.");
        }
    }
}
