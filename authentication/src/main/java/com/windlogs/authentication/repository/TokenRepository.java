package com.windlogs.authentication.repository;

import com.windlogs.authentication.entity.Token;
import com.windlogs.authentication.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByToken(String token);
    void deleteByUser(User user);
}
