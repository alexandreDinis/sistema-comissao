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
import lombok.extern.slf4j.Slf4j;

@Slf4j

@RestController
@RequestMapping("/api/v1/platform")
@RequiredArgsConstructor
public class PlatformController {

    private final EmpresaRepository empresaRepository;
    private final UserRepository userRepository;
    private final FeatureRepository featureRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/tenants")
    @PreAuthorize("hasAuthority('PLATFORM_COMPANY_MANAGE') or hasAnyRole('REVENDEDOR', 'ADMIN_LICENCA')")
    public ResponseEntity<List<Empresa>> listTenants(
            @org.springframework.security.core.annotation.AuthenticationPrincipal User principal) {
        // Isolation Check
        if (principal.getRole() == Role.ADMIN_LICENCA || principal.getRole() == Role.REVENDEDOR) {
            if (principal.getLicenca() == null) {
                return ResponseEntity.ok(java.util.Collections.emptyList());
            }
            return ResponseEntity.ok(empresaRepository.findByLicenca(principal.getLicenca()));
        }

        // SUPER_ADMIN sees all
        return ResponseEntity.ok(empresaRepository.findAll());
    }

    @PostMapping("/tenants")
    @PreAuthorize("hasAuthority('PLATFORM_COMPANY_MANAGE') or hasAnyRole('REVENDEDOR', 'ADMIN_LICENCA')")
    @Transactional
    public ResponseEntity<?> createTenant(@RequestBody CreateTenantRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User principal) {

        // Determine License Context
        com.empresa.comissao.domain.entity.Licenca ownerLicenca = null;

        if (principal.getRole() == Role.ADMIN_LICENCA || principal.getRole() == Role.REVENDEDOR) {
            if (principal.getLicenca() == null) {
                return ResponseEntity.badRequest().body("Erro: Revendedor sem licença vinculada.");
            }
            ownerLicenca = principal.getLicenca();
        } else if (request.getLicencaId() != null) {
            // SUPER_ADMIN manually assigning a license
            ownerLicenca = licencaRepository.findById(request.getLicencaId())
                    .orElseThrow(() -> new RuntimeException("Licença informada não encontrada"));
        } else {
            // CRITICAL: Orphan tenants are not allowed.
            return ResponseEntity.badRequest()
                    .body("Erro: É obrigatório vincular uma Licença (Revendedor) ao criar um Tenant.");
        }

        // 1. Create Company
        var empresa = Empresa.builder()
                .nome(request.getNome())
                .cnpj(request.getCnpj())
                .plano(request.getPlano())
                .ativo(true)
                .licenca(ownerLicenca) // Link to Reseller if applicable
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
    @PreAuthorize("hasAuthority('PLATFORM_DASHBOARD_VIEW') or hasAnyRole('REVENDEDOR', 'ADMIN_LICENCA')")
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
    @PreAuthorize("hasAuthority('PLATFORM_PLAN_MANAGE') or hasAnyRole('REVENDEDOR', 'ADMIN_LICENCA')")
    public ResponseEntity<java.util.List<PlanDTO>> getPlans() {
        return ResponseEntity.ok(java.util.Arrays.stream(Plano.values())
                .map(p -> new PlanDTO(p.name(), p.name(), java.math.BigDecimal.ZERO)) // Todo: Add prices to Enum
                .collect(java.util.stream.Collectors.toList()));
    }

    @PutMapping("/tenants/{id}/toggle-status")
    @PreAuthorize("hasAuthority('PLATFORM_COMPANY_MANAGE') or hasAnyRole('REVENDEDOR', 'ADMIN_LICENCA')")
    public ResponseEntity<Empresa> toggleTenantStatus(@PathVariable Long id,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User principal) {
        var empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        // Security: Reseller check
        checkResellerOwnership(principal, empresa);

        empresa.setAtivo(!empresa.isAtivo());
        return ResponseEntity.ok(empresaRepository.save(empresa));
    }

    @PutMapping("/tenants/{id}")
    @PreAuthorize("hasAuthority('PLATFORM_COMPANY_MANAGE') or hasAnyRole('REVENDEDOR', 'ADMIN_LICENCA')")
    @Transactional
    public ResponseEntity<Empresa> updateTenant(@PathVariable Long id, @RequestBody UpdateTenantRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User principal) {
        var empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        // Security: Reseller check
        checkResellerOwnership(principal, empresa);

        // 1. Update Basic Info
        empresa.setNome(request.getNome());
        empresa.setCnpj(request.getCnpj());

        // 2. Handle Plan Change
        if (request.getPlano() != null && request.getPlano() != empresa.getPlano()) {
            log.info("🔄 Alterando plano da empresa {} de {} para {}", empresa.getNome(), empresa.getPlano(),
                    request.getPlano());
            empresa.setPlano(request.getPlano());

            // Recalculate features
            List<Plano> eligiblePlans = java.util.Arrays.stream(Plano.values())
                    .filter(p -> p.ordinal() <= request.getPlano().ordinal())
                    .toList();
            List<Feature> availableFeatures = featureRepository.findByPlanoMinimoIn(eligiblePlans);

            // Update Admin Features
            List<User> admins = userRepository.findByEmpresaAndRole(empresa, Role.ADMIN_EMPRESA);
            for (User admin : admins) {
                admin.setFeatures(new HashSet<>(availableFeatures));
                userRepository.save(admin);
                log.info("✅ Features atualizadas para admin: {}", admin.getEmail());
            }
        }

        // 3. Update Admin Email (Optional - usually just updates the first admin found)
        if (request.getAdminEmail() != null && !request.getAdminEmail().isBlank()) {
            List<User> admins = userRepository.findByEmpresaAndRole(empresa, Role.ADMIN_EMPRESA);
            if (!admins.isEmpty()) {
                User mainAdmin = admins.get(0);
                if (!mainAdmin.getEmail().equals(request.getAdminEmail())) {
                    mainAdmin.setEmail(request.getAdminEmail());
                    userRepository.save(mainAdmin);
                }
            }
        }

        return ResponseEntity.ok(empresaRepository.save(empresa));
    }

    private void checkResellerOwnership(User principal, Empresa empresa) {
        if (principal.getRole() == Role.REVENDEDOR || principal.getRole() == Role.ADMIN_LICENCA) {
            if (empresa.getLicenca() == null || !empresa.getLicenca().getId().equals(principal.getLicenca().getId())) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "Você não tem permissão para gerenciar este inquilino.");
            }
        }
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
        private Long licencaId; // Optional: For Super Admin to assign directly
    }

    @Data
    static class UpdateTenantRequest {
        private String nome;
        private String cnpj;
        private Plano plano;
        private String adminEmail;
    }

    @Data
    static class ResetPasswordRequest {
        private String newPassword;
    }

    @PutMapping("/tenants/{id}/reset-password")
    @PreAuthorize("hasAuthority('PLATFORM_COMPANY_MANAGE') or hasAnyRole('REVENDEDOR', 'ADMIN_LICENCA')")
    @Transactional
    public ResponseEntity<?> resetTenantAdminPassword(@PathVariable Long id, @RequestBody ResetPasswordRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User principal) {

        var empresa = empresaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        // Security: Reseller check
        checkResellerOwnership(principal, empresa);

        if (request.getNewPassword() == null || request.getNewPassword().isBlank()) {
            return ResponseEntity.badRequest().body("A nova senha é obrigatória.");
        }

        // Find Admin (Assuming single admin or main admin)
        List<User> admins = userRepository.findByEmpresaAndRole(empresa, Role.ADMIN_EMPRESA);
        if (admins.isEmpty()) {
        }

        // Reset for the first admin found (usually the owner)
        User admin = admins.get(0);
        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(admin);

        log.info("✅ Senha do admin da empresa {} redefinida com sucesso por {}", empresa.getNome(),
                principal.getEmail());

        return ResponseEntity.ok().build();
    }

    // ========================================
    // OWNER DASHBOARD — Per-Reseller Stats
    // ========================================

    private final com.empresa.comissao.repository.LicencaRepository licencaRepository;

    @GetMapping("/licencas")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<com.empresa.comissao.domain.entity.Licenca>> listLicencas() {
        return ResponseEntity.ok(licencaRepository.findAll());
    }

    @GetMapping("/licencas/{id}/stats")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<LicencaStats> getLicencaStats(@PathVariable Long id) {
        var licenca = licencaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Licença não encontrada"));

        long total = empresaRepository.countByLicencaId(id);
        long ativos = empresaRepository.countByLicencaIdAndStatus(id,
                com.empresa.comissao.domain.enums.StatusEmpresa.ATIVA);
        long bloqueados = empresaRepository.countByLicencaIdAndStatus(id,
                com.empresa.comissao.domain.enums.StatusEmpresa.BLOQUEADA);
        java.math.BigDecimal receitaTotal = empresaRepository.sumValorMensalPagoByLicencaId(id);
        if (receitaTotal == null)
            receitaTotal = java.math.BigDecimal.ZERO;

        // Split calculation (configurable: example 20% royalty to owner)
        java.math.BigDecimal royaltyPercent = new java.math.BigDecimal("0.20"); // 20% for owner
        java.math.BigDecimal receitaOwner = receitaTotal.multiply(royaltyPercent);
        java.math.BigDecimal receitaRevendedor = receitaTotal.subtract(receitaOwner);

        return ResponseEntity.ok(LicencaStats.builder()
                .licencaId(id)
                .razaoSocial(licenca.getRazaoSocial())
                .nomeFantasia(licenca.getNomeFantasia())
                .status(licenca.getStatus().name())
                .totalTenants((int) total)
                .tenantsAtivos((int) ativos)
                .tenantsBloqueados((int) bloqueados)
                .receitaTotalTenants(receitaTotal)
                .receitaRevendedor(receitaRevendedor)
                .receitaOwner(receitaOwner)
                .crescimentoMensal(java.math.BigDecimal.ZERO) // TODO: calcular MoM
                .build());
    }

    @PostMapping("/licencas/{id}/rescindir")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<Void> rescindirLicenca(@PathVariable Long id) {
        var licenca = licencaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Licença não encontrada"));

        log.warn("🔴 RESCINDINDO LICENÇA: {} ({})", licenca.getRazaoSocial(), id);

        // 1. Migrate all tenants to direct owner management
        var tenants = empresaRepository.findByLicencaId(id);
        for (var tenant : tenants) {
            tenant.setLicencaOriginal(tenant.getLicenca()); // Keep history
            tenant.setLicenca(null); // Orphan = direct owner management
            empresaRepository.save(tenant);
            log.info("  → Tenant {} migrado para gestão direta", tenant.getNome());
        }

        // 2. Mark license as cancelled
        licenca.setStatus(com.empresa.comissao.domain.enums.StatusLicenca.CANCELADA);
        licencaRepository.save(licenca);

        log.warn("✅ Licença {} rescindida. {} tenants migrados.", id, tenants.size());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tenants/orphans")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Empresa>> listOrphanTenants() {
        return ResponseEntity.ok(empresaRepository.findByLicencaIsNull());
    }

    @GetMapping("/tenants/risk")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<List<Empresa>> listRiskyTenants() {
        // Tenants whose reseller is suspended or cancelled
        return ResponseEntity.ok(empresaRepository.findEmpresasComRevendedorBloqueado());
    }

    @PostMapping("/tenants/{tenantId}/reassign")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Transactional
    public ResponseEntity<Empresa> reassignTenant(@PathVariable Long tenantId, @RequestParam Long licencaId) {
        var tenant = empresaRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant não encontrado"));
        var licenca = licencaRepository.findById(licencaId)
                .orElseThrow(() -> new RuntimeException("Licença não encontrada"));

        tenant.setLicenca(licenca);
        return ResponseEntity.ok(empresaRepository.save(tenant));
    }

    @Data
    @lombok.Builder
    static class LicencaStats {
        private Long licencaId;
        private String razaoSocial;
        private String nomeFantasia;
        private String status;
        private Integer totalTenants;
        private Integer tenantsAtivos;
        private Integer tenantsBloqueados;
        private java.math.BigDecimal receitaTotalTenants;
        private java.math.BigDecimal receitaRevendedor;
        private java.math.BigDecimal receitaOwner; // Royalty
        private java.math.BigDecimal crescimentoMensal;
    }
}
