package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.domain.enums.Role;
import com.empresa.comissao.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.empresa.comissao.security.AuthPrincipal;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository repository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final com.empresa.comissao.repository.EmpresaRepository empresaRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'SUPER_ADMIN')")
    public ResponseEntity<List<UserResponse>> getAllUsers(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AuthPrincipal authPrincipal) {

        User principal = repository.findById(authPrincipal.getUserId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "User not found"));

        java.util.List<User> users;

        // SUPER_ADMIN can see all users, ADMIN_EMPRESA only sees their company's users
        if (principal.getRole() == Role.SUPER_ADMIN) {
            users = repository.findAll();
        } else {
            // Tenant isolation: only return users from same empresa
            if (principal.getEmpresa() == null) {
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }
            users = repository.findByEmpresa(principal.getEmpresa());
        }

        // Map to UserResponse (without sensitive data like password)
        java.util.List<UserResponse> response = users.stream()
                .map(u -> new UserResponse(
                        u.getId(),
                        u.getEmail(),
                        u.getRole(),
                        u.isActive(),
                        u.getFeatures() != null ? u.getFeatures().stream()
                                .map(com.empresa.comissao.domain.entity.Feature::getCodigo)
                                .collect(java.util.stream.Collectors.toSet()) : java.util.Collections.emptySet(),
                        u.getEmpresa() != null ? u.getEmpresa().getId() : null,
                        u.isParticipaComissao()))
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(org.springframework.security.core.Authentication authentication) {
        if (authentication == null
                || !(authentication.getPrincipal() instanceof com.empresa.comissao.security.AuthPrincipal)) {
            return ResponseEntity.status(401).build();
        }

        com.empresa.comissao.security.AuthPrincipal principal = (com.empresa.comissao.security.AuthPrincipal) authentication
                .getPrincipal();
        Long userId = principal.getUserId();
        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();

        User user = repository.findById(userId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "Usuário não encontrado"));

        // Validate Tenant Security (prevent leaking data from other tenants)
        if (user.getEmpresa() != null && !user.getEmpresa().getId().equals(tenantId)) {
            // Special case: Super Admin might be accessing from outside context, but for
            // "me" it should match?
            // Actually, if it's a multi-tenant system, "me" is the global user.
            // But if the token is scoped to a tenant (tenantId != null), they should match.
            if (tenantId != null && !user.getRole().equals(Role.SUPER_ADMIN)) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN,
                        "Usuário pertence a outra empresa (Tenant mismatch)");
            }
        }

        java.util.Set<String> featureCodes = user.getFeatures().stream()
                .map(com.empresa.comissao.domain.entity.Feature::getCodigo)
                .collect(java.util.stream.Collectors.toSet());

        return ResponseEntity
                .ok(new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.isActive(), featureCodes,
                        user.getEmpresa() != null ? user.getEmpresa().getId() : null, user.isParticipaComissao()));
    }

    /**
     * Endpoint para listar usuários da equipe (mesma empresa).
     * Qualquer usuário autenticado pode acessar para selecionar responsável em OS.
     */
    @GetMapping("/equipe")
    public ResponseEntity<List<UserResponse>> getEquipe(
            @org.springframework.security.core.annotation.AuthenticationPrincipal AuthPrincipal authPrincipal) {

        User principal = repository.findById(authPrincipal.getUserId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "User not found"));

        if (principal == null || principal.getEmpresa() == null) {
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }

        // Retorna todos os usuários ativos da mesma empresa que possuem papéis de
        // tenant legítimos
        java.util.List<User> users = repository.findByEmpresa(principal.getEmpresa())
                .stream()
                .filter(u -> u.isActive() && (u.getRole() == Role.ADMIN_EMPRESA || u.getRole() == Role.FUNCIONARIO))
                .collect(java.util.stream.Collectors.toList());

        java.util.List<UserResponse> response = users.stream()
                .map(u -> new UserResponse(
                        u.getId(),
                        u.getEmail(),
                        u.getRole(),
                        u.isActive(),
                        java.util.Collections.emptySet(), // Não expõe features para funcionários
                        u.getEmpresa() != null ? u.getEmpresa().getId() : null,
                        u.isParticipaComissao()))
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    static class UserResponse {
        private Long id;
        private String email;
        private Role role;
        private boolean active;
        private java.util.Set<String> features;
        private Long empresaId;
        private boolean participaComissao;
    }

    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> approveUser(@PathVariable Long id) {
        User user = repository.findById(id).orElseThrow();
        user.setActive(true);
        return ResponseEntity.ok(repository.save(user));
    }

    private final com.empresa.comissao.repository.FeatureRepository featureRepository;

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN_EMPRESA', 'SUPER_ADMIN')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody UpdateUserRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal AuthPrincipal authPrincipal) {

        User principal = repository.findById(authPrincipal.getUserId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "User not found"));

        User user = repository.findById(id).orElseThrow(() -> new RuntimeException("User not found"));

        // Security check: only allow update if generated by same company admin, or
        // SUPER_ADMIN
        boolean isSuperAdmin = principal.getRole() == Role.SUPER_ADMIN;
        if (!isSuperAdmin) {
            if (user.getEmpresa() == null || !user.getEmpresa().getId().equals(principal.getEmpresa().getId())) {
                Long principalEmpresaId = principal.getEmpresa() != null ? principal.getEmpresa().getId() : null;
                Long userEmpresaId = user.getEmpresa() != null ? user.getEmpresa().getId() : null;
                throw new org.springframework.security.access.AccessDeniedException(
                        String.format(
                                "Cannot update user %d (Empresa: %s) from Admin (Empresa: %s). Tenant isolation violation.",
                                user.getId(), userEmpresaId, principalEmpresaId));
            }
        }

        // Update fields
        if (request.getName() != null) {
            // User entity doesn't have name field in the viewed code, only email/password?
            // Checking User.java from previous steps, it only has email.
            // Ignoring name update as requested snippet might be generic.
        }

        // Update Features
        if (request.getFeatures() != null) {
            // Clear and add new features based on codes
            if (user.getFeatures() == null) {
                user.setFeatures(new java.util.HashSet<>());
            } else {
                user.getFeatures().clear();
            }

            for (String code : request.getFeatures()) {
                com.empresa.comissao.domain.entity.Feature f = featureRepository.findByCodigo(code)
                        .orElseThrow(() -> new RuntimeException("Feature not found: " + code));
                user.getFeatures().add(f);
            }
        }

        // Update Role if provided
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        if (request.getParticipaComissao() != null) {
            user.setParticipaComissao(request.getParticipaComissao());
        }

        repository.save(user);

        java.util.Set<String> featureCodes = user.getFeatures().stream()
                .map(com.empresa.comissao.domain.entity.Feature::getCodigo)
                .collect(java.util.stream.Collectors.toSet());

        return ResponseEntity
                .ok(new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.isActive(), featureCodes,
                        user.getEmpresa() != null ? user.getEmpresa().getId() : null, user.isParticipaComissao()));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> updateUserRole(@PathVariable Long id, @RequestBody Role role) {
        User user = repository.findById(id).orElseThrow();
        user.setRole(role);
        return ResponseEntity.ok(repository.save(user));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<UserResponse> createUser(
            @RequestBody CreateUserRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal AuthPrincipal authPrincipal) {

        User principal = repository.findById(authPrincipal.getUserId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "User not found"));

        // Determine empresa: SUPER_ADMIN can specify, ADMIN_EMPRESA uses their own
        com.empresa.comissao.domain.entity.Empresa empresa = null;
        if (principal.getRole() == Role.SUPER_ADMIN && request.getEmpresaId() != null) {
            // Super Admin creating user for a specific tenant
            empresa = empresaRepository.findById(request.getEmpresaId())
                    .orElseThrow(() -> new RuntimeException("Empresa not found: " + request.getEmpresaId()));
        } else if (principal.getRole() == Role.ADMIN_EMPRESA) {
            // Admin creating user for their own company
            empresa = principal.getEmpresa();
        }

        var user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole() != null ? request.getRole() : Role.FUNCIONARIO)
                .empresa(empresa)
                .empresa(empresa)
                .active(true)
                .features(new java.util.HashSet<>())
                .participaComissao(request.getParticipaComissao() != null ? request.getParticipaComissao() : true)
                .build();

        // Assign features if provided
        if (request.getFeatures() != null && !request.getFeatures().isEmpty()) {
            for (String code : request.getFeatures()) {
                com.empresa.comissao.domain.entity.Feature f = featureRepository.findByCodigo(code)
                        .orElseThrow(() -> new RuntimeException("Feature not found: " + code));
                user.getFeatures().add(f);
            }
        }

        User saved = repository.save(user);

        java.util.Set<String> featureCodes = saved.getFeatures().stream()
                .map(com.empresa.comissao.domain.entity.Feature::getCodigo)
                .collect(java.util.stream.Collectors.toSet());

        return ResponseEntity
                .ok(new UserResponse(saved.getId(), saved.getEmail(), saved.getRole(), saved.isActive(), featureCodes,
                        saved.getEmpresa() != null ? saved.getEmpresa().getId() : null, saved.isParticipaComissao()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestBody ChangePasswordRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal AuthPrincipal authPrincipal) {

        User principal = repository.findById(authPrincipal.getUserId())
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "User not found"));

        // Validate current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), principal.getPassword())) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Senha atual incorreta"));
        }

        // Validate new password
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Nova senha deve ter pelo menos 6 caracteres"));
        }

        // Update password
        User user = repository.findById(principal.getId()).orElseThrow();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setMustChangePassword(false);
        repository.save(user);

        return ResponseEntity.ok(java.util.Map.of("message", "Senha alterada com sucesso"));
    }

    @lombok.Data
    static class ChangePasswordRequest {
        private String currentPassword;
        private String newPassword;
    }
}

@lombok.Data
class CreateUserRequest {
    private String email;
    private String password;
    private Role role;
    private Long empresaId;
    private java.util.List<String> features;
    private Boolean participaComissao;
}

@lombok.Data
class UpdateUserRequest {
    private String name; // Kept for compatibility with request, though mapped field might not exist
    private Role role;
    private java.util.List<String> features;
    private Boolean participaComissao;
}
