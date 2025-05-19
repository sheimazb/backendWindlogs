package com.windlogs.authentication.repository;

import com.windlogs.authentication.entity.Role;
import com.windlogs.authentication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByResetPasswordToken(String token);
    List<User> findAllByRole(Role role);
    List<User> findByTenantAndRoleIn(String tenant, List<Role> roles);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = com.windlogs.authentication.entity.Role.PARTNER")
    long countAllPartners();

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = com.windlogs.authentication.entity.Role.PARTNER AND u.accountLocked = false")
    long countActivePartners();
  
    @Query("SELECT COUNT(u) FROM User u WHERE u.role = com.windlogs.authentication.entity.Role.PARTNER AND u.accountLocked = true")
    long countLockedPartners();
}
