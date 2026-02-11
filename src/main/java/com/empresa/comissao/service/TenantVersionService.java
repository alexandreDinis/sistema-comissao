package com.empresa.comissao.service;

import com.empresa.comissao.repository.EmpresaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantVersionService {

    private final EmpresaRepository empresaRepository;

    /**
     * Atomically increments the tenant version.
     * Call this whenever a syncable entity (OS, Client, etc.) changes.
     */
    @Transactional
    public void bump(Long tenantId) {
        if (tenantId == null) {
            log.warn("Tentativa de bump version com tenantId nulo");
            return;
        }
        empresaRepository.incrementTenantVersion(tenantId);
        log.trace("Tenant version bumped for company {}", tenantId);
    }

    /**
     * Helper to get current version (rarely used directly, usually via
     * SyncController)
     */
    @Transactional(readOnly = true)
    public Long getCurrentVersion(Long tenantId) {
        if (tenantId == null)
            return 0L;
        return empresaRepository.findById(tenantId)
                .map(com.empresa.comissao.domain.entity.Empresa::getTenantVersion)
                .orElse(0L);
    }
}
