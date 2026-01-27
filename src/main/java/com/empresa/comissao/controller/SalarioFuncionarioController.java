package com.empresa.comissao.controller;

import com.empresa.comissao.dto.request.SalarioFuncionarioRequest;
import com.empresa.comissao.dto.response.SalarioFuncionarioResponse;
import com.empresa.comissao.service.SalarioFuncionarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller para gerenciamento de salários e tipos de remuneração de
 * funcionários.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class SalarioFuncionarioController {

    private final SalarioFuncionarioService salarioFuncionarioService;

    /**
     * Lista todas as configurações de salário de uma empresa.
     */
    @GetMapping("/empresas/{empresaId}/salarios")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'SUPER_ADMIN')")
    public ResponseEntity<List<SalarioFuncionarioResponse>> listarPorEmpresa(@PathVariable Long empresaId) {
        log.info("📋 GET /empresas/{}/salarios", empresaId);
        List<SalarioFuncionarioResponse> salarios = salarioFuncionarioService.listarPorEmpresa(empresaId);
        return ResponseEntity.ok(salarios);
    }

    /**
     * Busca a configuração de salário ativa de um funcionário.
     */
    @GetMapping("/usuarios/{usuarioId}/salario")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'SUPER_ADMIN')")
    public ResponseEntity<SalarioFuncionarioResponse> buscarPorFuncionario(@PathVariable Long usuarioId) {
        log.info("🔍 GET /usuarios/{}/salario", usuarioId);
        return salarioFuncionarioService.buscarAtivaPorFuncionario(usuarioId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Busca uma configuração por ID.
     */
    @GetMapping("/salarios/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'SUPER_ADMIN')")
    public ResponseEntity<SalarioFuncionarioResponse> buscarPorId(@PathVariable Long id) {
        log.info("🔍 GET /salarios/{}", id);
        SalarioFuncionarioResponse salario = salarioFuncionarioService.buscarPorId(id);
        return ResponseEntity.ok(salario);
    }

    /**
     * Define a configuração de salário de um funcionário.
     * Desativa configurações anteriores automaticamente.
     */
    @PostMapping("/empresas/{empresaId}/salarios")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'SUPER_ADMIN')")
    public ResponseEntity<SalarioFuncionarioResponse> definir(
            @PathVariable Long empresaId,
            @Valid @RequestBody SalarioFuncionarioRequest request) {
        log.info("💰 POST /empresas/{}/salarios - Funcionário: {}", empresaId, request.getUsuarioId());
        SalarioFuncionarioResponse salario = salarioFuncionarioService.definir(empresaId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(salario);
    }

    /**
     * Atualiza uma configuração de salário existente.
     */
    @PutMapping("/salarios/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'SUPER_ADMIN')")
    public ResponseEntity<SalarioFuncionarioResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody SalarioFuncionarioRequest request) {
        log.info("📝 PUT /salarios/{}", id);
        SalarioFuncionarioResponse salario = salarioFuncionarioService.atualizar(id, request);
        return ResponseEntity.ok(salario);
    }

    /**
     * Desativa uma configuração de salário.
     */
    @DeleteMapping("/salarios/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'SUPER_ADMIN')")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        log.info("📴 DELETE /salarios/{}", id);
        salarioFuncionarioService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
