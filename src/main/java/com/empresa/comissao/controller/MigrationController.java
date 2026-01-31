package com.empresa.comissao.controller;

import com.empresa.comissao.dto.TransferenciaTenantDTO;
import com.empresa.comissao.service.TenantMigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/platform/admin/migration")
@RequiredArgsConstructor
public class MigrationController {

    private final TenantMigrationService migrationService;

    @PutMapping("/transfer-tenant")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> transferirTenant(@RequestBody TransferenciaTenantDTO dto) {
        migrationService.migrarTenant(dto.empresaId(), dto.novaLicencaId(), dto.motivo());
        return ResponseEntity.ok().body(java.util.Map.of("message", "Migração realizada com sucesso"));
    }
}
