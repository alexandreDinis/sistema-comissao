package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.PagamentoAdiantado;
import com.empresa.comissao.service.ComissaoService;
import com.empresa.comissao.dto.request.AdiantamentoRequest;
import com.empresa.comissao.repository.UserRepository;
import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/adiantamento")
@RequiredArgsConstructor
public class AdiantamentoController {

    private final ComissaoService comissaoService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<PagamentoAdiantado> registrarAdiantamento(
            @RequestBody AdiantamentoRequest request) {

        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new BusinessException(" Empresa não encontrada no contexto de segurança.");
        }

        // ✅ OBRIGATÓRIO: Adiantamento deve ser vinculado a um funcionário específico
        if (request.getUsuarioId() == null) {
            throw new BusinessException(
                    "O campo 'usuarioId' é obrigatório. Selecione o funcionário que receberá o adiantamento.");
        }

        User targetUser = userRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new BusinessException(
                        "Funcionário não encontrado com ID: " + request.getUsuarioId()));

        // Verificar se o funcionário pertence à empresa do contexto
        if (targetUser.getEmpresa() == null || !targetUser.getEmpresa().getId().equals(tenantId)) {
            throw new BusinessException("Não é permitido lançar adiantamento para funcionário de outra empresa.");
        }

        PagamentoAdiantado adiantamento = comissaoService.adicionarAdiantamento(
                request.getDataPagamento(),
                request.getValor(),
                request.getObservacao(),
                targetUser);
        return new ResponseEntity<>(adiantamento, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<PagamentoAdiantado>> listar() {
        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new BusinessException("Empresa não identificada.");
        }
        com.empresa.comissao.domain.entity.Empresa empresaProxy = new com.empresa.comissao.domain.entity.Empresa();
        empresaProxy.setId(tenantId);

        return ResponseEntity.ok(comissaoService.listarAdiantamentos(empresaProxy));
    }

}
