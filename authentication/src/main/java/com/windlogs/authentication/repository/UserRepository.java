package com.windlogs.authentication.repository;

import com.windlogs.authentication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByResetPasswordToken(String token);
    /**
     * The findByEmail() method will be used to check a user's email when he starts
      to use the forgot password function.
     * And the findByResetPasswordToken() method will be used to validate
     the token when the user clicks the change password link in email.
     * */

    // Custom method to find user by ID
    Optional<User> findById(Long id);
    
    // You can add more custom query methods if needed
}
