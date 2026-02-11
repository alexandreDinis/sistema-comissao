package com.empresa.comissao.controller;

import com.empresa.comissao.dto.response.SyncStatusResponse;
import com.empresa.comissao.repository.ClienteRepository;
import com.empresa.comissao.repository.OrdemServicoRepository;
import com.empresa.comissao.repository.TipoPecaRepository;
import com.empresa.comissao.repository.UserRepository;
import com.empresa.comissao.repository.ComissaoCalculadaRepository;
import com.empresa.comissao.service.TenantVersionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class SyncController {

    private final ClienteRepository clienteRepository;
    private final OrdemServicoRepository osRepository;
    private final TipoPecaRepository tipoPecaRepository;
    private final UserRepository userRepository;
    private final ComissaoCalculadaRepository comissaoRepository;
    private final TenantVersionService tenantVersionService;

    @GetMapping("/status")
    @Operation(summary = "Verificar status de sincronização", description = "Retorna os timestamps mais recentes de atualização para verificação leve")
    public ResponseEntity<SyncStatusResponse> getSyncStatus(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Instant lastSync) {
        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();

        if (tenantId == null) {
            throw new com.empresa.comissao.exception.BusinessException("Tenant não identificado no contexto");
        }

        Instant compareTime = lastSync != null ? lastSync : Instant.EPOCH;

        LocalDateTime maxClientes = clienteRepository.findMaxUpdatedAtByEmpresaId(tenantId);
        LocalDateTime maxOS = osRepository.findMaxUpdatedAtByEmpresaId(tenantId);
        LocalDateTime maxTipoPeca = tipoPecaRepository.findMaxUpdatedAtByEmpresaId(tenantId);
        LocalDateTime maxUser = userRepository.findMaxUpdatedAtByEmpresaId(tenantId);
        LocalDateTime maxComissao = comissaoRepository.findMaxUpdatedAtByEmpresaId(tenantId);

        Instant maxClientesInstant = maxClientes != null ? maxClientes.toInstant(ZoneOffset.UTC) : null;
        Instant maxOSInstant = maxOS != null ? maxOS.toInstant(ZoneOffset.UTC) : null;
        Instant maxTipoPecaInstant = maxTipoPeca != null ? maxTipoPeca.toInstant(ZoneOffset.UTC) : null;
        Instant maxUserInstant = maxUser != null ? maxUser.toInstant(ZoneOffset.UTC) : null;
        Instant maxComissaoInstant = maxComissao != null ? maxComissao.toInstant(ZoneOffset.UTC) : null;

        boolean clientesUpdated = maxClientesInstant != null && maxClientesInstant.isAfter(compareTime);
        boolean osUpdated = maxOSInstant != null && maxOSInstant.isAfter(compareTime);
        boolean tiposPecaUpdated = maxTipoPecaInstant != null && maxTipoPecaInstant.isAfter(compareTime);
        boolean usersUpdated = maxUserInstant != null && maxUserInstant.isAfter(compareTime);
        boolean comissoesUpdated = maxComissaoInstant != null && maxComissaoInstant.isAfter(compareTime);

        log.info(
                "[SYNC_STATUS] lastSync={}, compareTime={}, maxC={}, maxOS={}, maxTP={}, maxU={}, maxCom={}, updates=C:{},OS:{},TP:{},U:{},Com:{}",
                lastSync, compareTime, maxClientesInstant, maxOSInstant, maxTipoPecaInstant, maxUserInstant,
                maxComissaoInstant,
                clientesUpdated, osUpdated, tiposPecaUpdated, usersUpdated, comissoesUpdated);

        Long currentVersion = tenantVersionService.getCurrentVersion(tenantId);

        SyncStatusResponse response = SyncStatusResponse.builder()
                .serverTime(Instant.now())
                .clientesUpdatedAtMax(maxClientesInstant)
                .osUpdatedAtMax(maxOSInstant)
                .clientesUpdated(clientesUpdated)
                .osUpdated(osUpdated)
                .tiposPecaUpdated(tiposPecaUpdated)
                .usersUpdated(usersUpdated)
                .comissoesUpdated(comissoesUpdated)
                .lastTenantVersion(currentVersion)
                .build();

        return ResponseEntity.ok(response);
    }
}
