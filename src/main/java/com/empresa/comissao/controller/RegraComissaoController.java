package com.empresa.comissao.controller;

import com.empresa.comissao.dto.request.RegraComissaoRequest;
import com.empresa.comissao.dto.response.RegraComissaoResponse;
import com.empresa.comissao.service.RegraComissaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para gerenciamento de regras de comissionamento.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class RegraComissaoController {

    private final RegraComissaoService regraComissaoService;

    /**
     * Lista todas as regras de comissão de uma empresa.
     */
    @GetMapping("/empresas/{empresaId}/regras-comissao")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'SUPER_ADMIN')")
    public ResponseEntity<List<RegraComissaoResponse>> listarPorEmpresa(@PathVariable Long empresaId) {
        log.info("📋 GET /empresas/{}/regras-comissao", empresaId);
        List<RegraComissaoResponse> regras = regraComissaoService.listarPorEmpresa(empresaId);
        return ResponseEntity.ok(regras);
    }

    /**
     * Busca a regra ativa de uma empresa.
     */
    @GetMapping("/empresas/{empresaId}/regras-comissao/ativa")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'SUPER_ADMIN', 'FUNCIONARIO')")
    public ResponseEntity<RegraComissaoResponse> buscarRegraAtiva(@PathVariable Long empresaId) {
        log.info("🔍 GET /empresas/{}/regras-comissao/ativa", empresaId);
        RegraComissaoResponse regra = regraComissaoService.buscarRegraAtiva(empresaId);
        return ResponseEntity.ok(regra);
    }

    /**
     * Busca uma regra por ID.
     */
    @GetMapping("/regras-comissao/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'SUPER_ADMIN')")
    public ResponseEntity<RegraComissaoResponse> buscarPorId(@PathVariable Long id) {
        log.info("🔍 GET /regras-comissao/{}", id);
        RegraComissaoResponse regra = regraComissaoService.buscarPorId(id);
        return ResponseEntity.ok(regra);
    }

    /**
     * Cria uma nova regra de comissão.
     */
    @PostMapping("/empresas/{empresaId}/regras-comissao")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'SUPER_ADMIN')")
    public ResponseEntity<RegraComissaoResponse> criar(
            @PathVariable Long empresaId,
            @Valid @RequestBody RegraComissaoRequest request) {
        log.info("➕ POST /empresas/{}/regras-comissao", empresaId);
        RegraComissaoResponse regra = regraComissaoService.criar(empresaId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(regra);
    }

    /**
     * Atualiza uma regra existente.
     */
    @PutMapping("/regras-comissao/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'SUPER_ADMIN')")
    public ResponseEntity<RegraComissaoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody RegraComissaoRequest request) {
        log.info("📝 PUT /regras-comissao/{}", id);
        RegraComissaoResponse regra = regraComissaoService.atualizar(id, request);
        return ResponseEntity.ok(regra);
    }

    /**
     * Ativa uma regra (desativando as outras da mesma empresa).
     */
    @PostMapping("/regras-comissao/{id}/ativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'SUPER_ADMIN')")
    public ResponseEntity<RegraComissaoResponse> ativar(@PathVariable Long id) {
        log.info("✅ POST /regras-comissao/{}/ativar", id);
        RegraComissaoResponse regra = regraComissaoService.ativar(id);
        return ResponseEntity.ok(regra);
    }

    /**
     * Desativa uma regra.
     */
    @PostMapping("/regras-comissao/{id}/desativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'SUPER_ADMIN')")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        log.info("📴 POST /regras-comissao/{}/desativar", id);
        regraComissaoService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Deleta uma regra (apenas se inativa).
     */
    @DeleteMapping("/regras-comissao/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'SUPER_ADMIN')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        log.info("🗑️ DELETE /regras-comissao/{}", id);
        regraComissaoService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
