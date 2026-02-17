package com.empresa.comissao.domain.entity;

import com.empresa.comissao.domain.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String email;

    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    private boolean active;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "empresa_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Empresa empresa; // Nullable for SUPER_ADMIN (Platform Level)

    @ManyToMany(fetch = FetchType.LAZY, cascade = { CascadeType.MERGE, CascadeType.PERSIST })
    @JoinTable(name = "user_features", joinColumns = @JoinColumn(name = "usuario_id"), inverseJoinColumns = @JoinColumn(name = "feature_id"))
    private java.util.Set<Feature> features;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "licenca_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Licenca licenca; // Nullable (only for Resellers)

    @Builder.Default
    @Column(name = "must_change_password")
    private boolean mustChangePassword = false;

    @Builder.Default
    @Column(name = "participa_comissao")
    private boolean participaComissao = true;

    @Builder.Default
    @Column(nullable = false)
    private Integer authVersion = 0;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        java.util.List<GrantedAuthority> authorities = new java.util.ArrayList<>();

        // 1. Convert Roles
        if (role == Role.ADMIN_EMPRESA) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN_EMPRESA"));
        } else if (role == Role.SUPER_ADMIN) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ROOT"));
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
        } else if (role == Role.ADMIN_LICENCA || role == Role.REVENDEDOR) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN_LICENCA"));
            authorities.add(new SimpleGrantedAuthority("ROLE_REVENDEDOR"));
        } else if (role == Role.FUNCIONARIO) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            authorities.add(new SimpleGrantedAuthority("ROLE_FUNCIONARIO"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        }

        // 2. Convert Features
        if (features != null) {
            features.forEach(f -> authorities.add(new SimpleGrantedAuthority(f.getCodigo())));
        }

        return authorities;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
