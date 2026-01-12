package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.Despesa;
import com.empresa.comissao.dto.DespesaRequestDTO;
import com.empresa.comissao.service.ComissaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/despesas")
@RequiredArgsConstructor
@Tag(name = "Despesas", description = "Endpoints para gerenciamento de gastos (alimentação, combustível, etc.)")
public class DespesaController {

    private final ComissaoService comissaoService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registrar uma nova despesa", description = "Adiciona um gasto categorizado ao sistema.")
    public ResponseEntity<Despesa> criar(
            @Valid @RequestBody DespesaRequestDTO request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.empresa.comissao.domain.entity.User usuario) {
        Despesa salva = comissaoService.adicionarDespesa(
                request.getDataDespesa(),
                request.getValor(),
                request.getCategoria(),
                request.getDescricao(),
                usuario);
        return ResponseEntity.ok(salva);
    }

    @GetMapping
    @Operation(summary = "Listar despesas", description = "Retorna todas as despesas cadastradas")
    public ResponseEntity<List<Despesa>> listar() {
        return ResponseEntity.ok(comissaoService.listarDespesas());
    }
}
