package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.Licenca;
import com.empresa.comissao.repository.EmpresaRepository;
import com.empresa.comissao.repository.LicencaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantMigrationService {

    private final EmpresaRepository empresaRepository;
    private final LicencaRepository licencaRepository;

    @Transactional
    public void migrarTenant(Long tenantId, Long novaLicencaId, String motivo) {
        // 1. Validate Entities
        Empresa empresa = empresaRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Empresa não encontrada"));

        Licenca novaLicenca = licencaRepository.findById(novaLicencaId)
                .orElseThrow(() -> new RuntimeException("Nova Licença não encontrada"));

        // 2. Audit
        logAudit(
                "MIGRACAO_TENANT",
                "Tenant " + empresa.getNome() + " (ID: " + empresa.getId() + ") movido da licença " +
                        (empresa.getLicenca() != null ? empresa.getLicenca().getId() : "ORPHAN") +
                        " para " + novaLicencaId,
                motivo);

        // 3. Execution
        empresa.setLicenca(novaLicenca);

        // 4. Reset specific configs if needed (not implemented yet)

        empresaRepository.save(empresa);
        log.info("✅ Migração concluída: Tenant {} -> Licença {}", tenantId, novaLicencaId);
    }

    private void logAudit(String action, String details, String reason) {
        // Simple logging for now, can be expanded to DB Audit Log later
        log.warn("[AUDIT] Action: {} | Details: {} | Reason: {}", action, details, reason);
    }
}
