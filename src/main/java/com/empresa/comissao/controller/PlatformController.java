package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.Feature;
import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.domain.enums.Plano;
import com.empresa.comissao.domain.enums.Role;
import com.empresa.comissao.repository.EmpresaRepository;
import com.empresa.comissao.repository.FeatureRepository;
import com.empresa.comissao.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;

@RestController
@RequestMapping("/api/v1/platform")
@RequiredArgsConstructor
public class PlatformController {

    private final EmpresaRepository empresaRepository;
    private final UserRepository userRepository;
    private final FeatureRepository featureRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/tenants")
    @PreAuthorize("hasAuthority('PLATFORM_COMPANY_MANAGE')")
    public ResponseEntity<List<Empresa>> listTenants() {
        return ResponseEntity.ok(empresaRepository.findAll());
    }

    @PostMapping("/tenants")
    @PreAuthorize("hasAuthority('PLATFORM_COMPANY_MANAGE')")
    @Transactional
    public ResponseEntity<Empresa> createTenant(@RequestBody CreateTenantRequest request) {
        // 1. Create Company
        var empresa = Empresa.builder()
                .nome(request.getNome())
                .cnpj(request.getCnpj())
                .plano(request.getPlano())
                .ativo(true)
                .build();
        empresa = empresaRepository.save(empresa);

        // 2. Get features available for the company's plan
        // Build list of plans that are equal or lower than the company's plan
        List<Plano> eligiblePlans = java.util.Arrays.stream(Plano.values())
                .filter(p -> p.ordinal() <= request.getPlano().ordinal())
                .toList();
        List<Feature> availableFeatures = featureRepository.findByPlanoMinimoIn(eligiblePlans);

        // 3. Create Initial Admin with features assigned
        var admin = User.builder()
                .email(request.getAdminEmail())
                .password(passwordEncoder.encode(request.getAdminPassword()))
                .role(Role.ADMIN_EMPRESA)
                .empresa(empresa)
                .active(true)
                .features(new HashSet<>(availableFeatures))
                .build();
        userRepository.save(admin);

        return ResponseEntity.ok(empresa);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('PLATFORM_DASHBOARD_VIEW')")
    public ResponseEntity<PlatformStats> getStats() {
        return ResponseEntity.ok(PlatformStats.builder()
                .totalTenants(empresaRepository.count())
                .activeTenants(empresaRepository.countByAtivoTrue())
                .bronzeTenants(empresaRepository.countByPlano(Plano.BRONZE))
                .silverTenants(empresaRepository.countByPlano(Plano.PRATA))
                .goldTenants(empresaRepository.countByPlano(Plano.OURO))
                .totalUsers(userRepository.count())
                .mrr(java.math.BigDecimal.ZERO)
                .build());
    }

    @GetMapping("/plans")
    @PreAuthorize("hasAuthority('PLATFORM_PLAN_MANAGE')")
    public ResponseEntity<java.util.List<PlanDTO>> getPlans() {
        return ResponseEntity.ok(java.util.Arrays.stream(Plano.values())
                .map(p -> new PlanDTO(p.name(), p.name(), java.math.BigDecimal.ZERO)) // Todo: Add prices to Enum
                .collect(java.util.stream.Collectors.toList()));
    }

    @PutMapping("/tenants/{id}/toggle-status")
    @PreAuthorize("hasAuthority('PLATFORM_COMPANY_MANAGE')")
    public ResponseEntity<Empresa> toggleTenantStatus(@PathVariable Long id) {
        var empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));
        empresa.setAtivo(!empresa.isAtivo());
        return ResponseEntity.ok(empresaRepository.save(empresa));
    }

    @Data
    @lombok.Builder
    static class PlatformStats {
        private long totalTenants;
        private long activeTenants;
        private long bronzeTenants;
        private long silverTenants;
        private long goldTenants;
        private long totalUsers;
        private java.math.BigDecimal mrr;
    }

    @Data
    @lombok.AllArgsConstructor
    static class PlanDTO {
        private String id;
        private String name;
        private java.math.BigDecimal price;
    }

    @Data
    static class CreateTenantRequest {
        private String nome;
        private String cnpj;
        private Plano plano;
        private String adminEmail;
        private String adminPassword;
    }
}
