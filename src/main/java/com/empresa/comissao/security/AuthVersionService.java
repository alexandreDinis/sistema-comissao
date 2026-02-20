package com.empresa.comissao.security;

import com.empresa.comissao.domain.enums.StatusEmpresa;
import com.empresa.comissao.domain.enums.StatusLicenca;
import com.empresa.comissao.repository.EmpresaRepository;
import com.empresa.comissao.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthVersionService {

    private final UserRepository userRepository;
    private final EmpresaRepository empresaRepository;

    /**
     * Busca versão de auth do usuário (cacheado por 60s).
     * Cache miss → 1 query leve (SELECT id, auth_version, active).
     */
    @Cacheable(value = "userAuthVersion", key = "#userId", unless = "#result == null")
    public UserAuthSnapshot getUserAuthVersion(Long userId) {
        log.debug("[AuthCache] Cache miss para userId={}", userId);

        return userRepository.findAuthVersionById(userId)
                .map(snapshot -> {
                    log.debug("[AuthCache] Loaded userId={}, version={}, active={}",
                            userId, snapshot.getAuthVersion(), snapshot.isActive());
                    return snapshot;
                })
                .orElse(null);
    }

    /**
     * Busca versão de tenant (cacheado por 60s).
     * Cache miss → 1 query leve (SELECT id, tenant_version, status,
     * licenca_status).
     */
    @Cacheable(value = "tenantAccessVersion", key = "#tenantId", unless = "#result == null")
    public TenantAccessSnapshot getTenantAccessVersion(Long tenantId) {
        log.debug("[AuthCache] Cache miss para tenantId={}", tenantId);

        return empresaRepository.findTenantAccessVersionById(tenantId)
                .map(snapshot -> {
                    log.debug("[AuthCache] Loaded tenantId={}, version={}, status={}",
                            tenantId, snapshot.getTenantVersion(), snapshot.getStatus());
                    return snapshot;
                })
                .orElse(null);
    }

    /**
     * Incrementa versão de auth (força invalidação de tokens).
     * Chamado quando: role muda, permissões mudam, usuário desativado, etc.
     */
    @org.springframework.cache.annotation.CacheEvict(value = "userAuthVersion", key = "#userId")
    public void incrementUserAuthVersion(Long userId) {
        log.info("[AuthInvalidate] Incrementando auth_version para userId={}", userId);
        userRepository.incrementAuthVersion(userId);
    }

    /**
     * Incrementa versão de tenant (força invalidação de tokens).
     * Chamado quando: plano muda, empresa bloqueada, licença suspensa, etc.
     */
    @org.springframework.cache.annotation.CacheEvict(value = "tenantAccessVersion", key = "#tenantId")
    public void incrementTenantVersion(Long tenantId) {
        log.info("[AuthInvalidate] Incrementando tenant_version para tenantId={}", tenantId);
        empresaRepository.incrementTenantVersion(tenantId);
    }

    // ===== DTOs para snapshots leves =====

    public static class UserAuthSnapshot {
        private Long id;
        private Integer authVersion;
        private boolean active;

        public UserAuthSnapshot(Long id, Integer authVersion, boolean active) {
            this.id = id;
            this.authVersion = authVersion;
            this.active = active;
        }

        public Long getId() {
            return id;
        }

        public Integer getAuthVersion() {
            return authVersion;
        }

        public boolean isActive() {
            return active;
        }
    }

    public static class TenantAccessSnapshot {
        private Long id;
        private Long tenantVersion;
        private StatusEmpresa status;
        private StatusLicenca licencaStatus;

        public TenantAccessSnapshot(Long id, Long tenantVersion, StatusEmpresa status, StatusLicenca licencaStatus) {
            this.id = id;
            this.tenantVersion = tenantVersion;
            this.status = status;
            this.licencaStatus = licencaStatus;
        }

        public Long getId() {
            return id;
        }

        public Long getTenantVersion() {
            return tenantVersion;
        }

        public StatusEmpresa getStatus() {
            return status;
        }

        public StatusLicenca getLicencaStatus() {
            return licencaStatus;
        }
    }
}
