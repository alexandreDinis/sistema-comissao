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
            @RequestBody AdiantamentoRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal User usuarioAutenticado) {

        // ✅ OBRIGATÓRIO: Adiantamento deve ser vinculado a um funcionário específico
        if (request.getUsuarioId() == null) {
            throw new BusinessException(
                    "O campo 'usuarioId' é obrigatório. Selecione o funcionário que receberá o adiantamento.");
        }

        User targetUser = userRepository.findById(request.getUsuarioId())
                .orElseThrow(() -> new BusinessException(
                        "Funcionário não encontrado com ID: " + request.getUsuarioId()));

        // Verificar se o funcionário pertence à mesma empresa
        if (usuarioAutenticado.getEmpresa() != null && targetUser.getEmpresa() != null &&
                !targetUser.getEmpresa().getId().equals(usuarioAutenticado.getEmpresa().getId())) {
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
    public ResponseEntity<List<PagamentoAdiantado>> listar(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.empresa.comissao.domain.entity.User usuario) {
        return ResponseEntity.ok(comissaoService.listarAdiantamentos(usuario != null ? usuario.getEmpresa() : null));
    }

}
