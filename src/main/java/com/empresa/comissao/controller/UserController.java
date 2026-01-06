package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.domain.enums.Role;
import com.empresa.comissao.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository repository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(java.security.Principal principal) {
        if (principal == null) {
            return ResponseEntity.notFound().build();
        }
        User user = repository.findByEmail(principal.getName()).orElseThrow();
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.isActive()));
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    static class UserResponse {
        private Long id;
        private String email;
        private Role role;
        private boolean active;
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> approveUser(@PathVariable Long id) {
        User user = repository.findById(id).orElseThrow();
        user.setActive(true);
        return ResponseEntity.ok(repository.save(user));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> updateUserRole(@PathVariable Long id, @RequestBody Role role) {
        User user = repository.findById(id).orElseThrow();
        user.setRole(role);
        return ResponseEntity.ok(repository.save(user));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> createUser(@RequestBody CreateUserRequest request) {
        var user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .active(true) // Admin-created users are active by default
                .build();
        return ResponseEntity.ok(repository.save(user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

@lombok.Data
class CreateUserRequest {
    private String email;
    private String password;
    private Role role;
}
