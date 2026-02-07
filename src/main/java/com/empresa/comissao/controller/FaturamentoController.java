package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.Faturamento;
import com.empresa.comissao.service.ComissaoService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/faturamento")
@RequiredArgsConstructor
public class FaturamentoController {

    private final ComissaoService comissaoService;
    private final com.empresa.comissao.service.FinanceiroService financeiroService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Faturamento> registrarFaturamento(
            @RequestBody FaturamentoRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.empresa.comissao.domain.entity.User usuario) {
        Faturamento faturamento = comissaoService.adicionarFaturamento(request.getDataFaturamento(),
                request.getValor(), usuario);
        return new ResponseEntity<>(faturamento, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<Faturamento>> listar(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.empresa.comissao.domain.entity.User usuario) {
        return ResponseEntity.ok(comissaoService.listarFaturamentos(usuario != null ? usuario.getEmpresa() : null));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<Faturamento> buscarPorId(@PathVariable Long id) {
        // Usa o serviço financeiro que já chama o repositório com JOIN FETCH
        // (findByIdComGrafoCompleto)
        return financeiroService.getFaturamentoDetalhado(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Getter
    @Setter
    public static class FaturamentoRequest {
        private LocalDate dataFaturamento;
        private BigDecimal valor;
    }
}
