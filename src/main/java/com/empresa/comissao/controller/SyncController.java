package com.empresa.comissao.controller;

import com.empresa.comissao.dto.response.SyncStatusResponse;
import com.empresa.comissao.repository.ClienteRepository;
import com.empresa.comissao.repository.OrdemServicoRepository;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1/sync")
@Tag(name = "Sincronização", description = "Endpoints para sincronização de dados")
@RequiredArgsConstructor
public class SyncController {

    private final ClienteRepository clienteRepository;
    private final OrdemServicoRepository osRepository;

    @GetMapping("/status")
    @Operation(summary = "Verificar status de sincronização", description = "Retorna os timestamps mais recentes de atualização para verificação leve")
    public ResponseEntity<SyncStatusResponse> getSyncStatus() {
        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();

        if (tenantId == null) {
            // Should be handled by filter, but double check
            throw new com.empresa.comissao.exception.BusinessException("Tenant não identificado no contexto");
        }

        LocalDateTime maxClientes = clienteRepository.findMaxUpdatedAtByEmpresaId(tenantId);
        LocalDateTime maxOS = osRepository.findMaxUpdatedAtByEmpresaId(tenantId);

        SyncStatusResponse response = SyncStatusResponse.builder()
                .serverTime(Instant.now())
                .clientesUpdatedAtMax(maxClientes != null ? maxClientes.toInstant(ZoneOffset.UTC) : null)
                .osUpdatedAtMax(maxOS != null ? maxOS.toInstant(ZoneOffset.UTC) : null)
                .build();

        return ResponseEntity.ok(response);
    }
}
