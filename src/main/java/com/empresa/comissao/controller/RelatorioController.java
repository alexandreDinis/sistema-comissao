package com.empresa.comissao.controller;

import com.empresa.comissao.dto.RelatorioFinanceiroDTO;
import com.empresa.comissao.service.ComissaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/relatorios")
@RequiredArgsConstructor
@Tag(name = "Relatórios", description = "Endpoints para geração de relatórios financeiros consolidados")
public class RelatorioController {

    private final ComissaoService comissaoService;

    @GetMapping("/{ano}/{mes}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Gerar relatório financeiro mensal", description = "Retorna um resumo contendo despesas por categoria, imposto (6%) e comissão a pagar.")
    public ResponseEntity<RelatorioFinanceiroDTO> gerarRelatorio(@PathVariable int ano, @PathVariable int mes) {
        RelatorioFinanceiroDTO relatorio = comissaoService.gerarRelatorioFinanceiro(ano, mes);
        return ResponseEntity.ok(relatorio);
    }
}
