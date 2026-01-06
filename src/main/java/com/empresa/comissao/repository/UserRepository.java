package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    // UserDetails findByEmail(String email); // Not strictly needed if returning
    // Optional<User> works for CustomUserDetailsService
}
