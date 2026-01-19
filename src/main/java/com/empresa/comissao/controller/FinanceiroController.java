package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.*;
import com.empresa.comissao.domain.enums.*;
import com.empresa.comissao.service.FinanceiroService;
import com.empresa.comissao.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Controller para o módulo financeiro.
 */
@RestController
@RequestMapping("/api/v1/financeiro")
@RequiredArgsConstructor
@Slf4j
public class FinanceiroController {

    private final FinanceiroService financeiroService;
    private final com.empresa.comissao.service.PdfService pdfService;

    // ========================================
    // CONTAS A PAGAR
    // ========================================

    @GetMapping("/contas-pagar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<List<ContaPagar>> listarContasPagar(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal User usuario) {

        Empresa empresa = usuario.getEmpresa();
        if (empresa == null) {
            throw new BusinessException("Usuário não está vinculado a uma empresa");
        }

        List<ContaPagar> contas;
        if ("PENDENTE".equalsIgnoreCase(status)) {
            contas = financeiroService.listarContasPagarPendentes(empresa);
        } else if ("VENCIDO".equalsIgnoreCase(status)) {
            contas = financeiroService.listarContasPagarVencidas(empresa);
        } else {
            contas = financeiroService.listarContasPagarPendentes(empresa);
        }

        return ResponseEntity.ok(contas);
    }

    @PostMapping("/contas-pagar/{id}/pagar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<ContaPagar> pagarConta(
            @PathVariable Long id,
            @RequestBody PagarContaRequest request) {

        ContaPagar conta = financeiroService.pagarConta(
                id,
                request.dataPagamento() != null ? request.dataPagamento() : LocalDate.now(),
                request.meioPagamento());
        return ResponseEntity.ok(conta);
    }

    // ========================================
    // CONTAS A RECEBER
    // ========================================

    @GetMapping("/contas-receber")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<List<ContaReceber>> listarContasReceber(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal User usuario) {

        Empresa empresa = usuario.getEmpresa();
        if (empresa == null) {
            throw new BusinessException("Usuário não está vinculado a uma empresa");
        }

        List<ContaReceber> contas;
        if ("VENCIDO".equalsIgnoreCase(status)) {
            contas = financeiroService.listarContasReceberVencidas(empresa);
        } else {
            StatusConta statusEnum = null;
            if (status != null && !status.isEmpty()) {
                try {
                    statusEnum = StatusConta.valueOf(status.toUpperCase());
                } catch (IllegalArgumentException e) {
                    log.warn("Status inválido filtro: {}", status);
                }
            }
            contas = financeiroService.listarContasReceber(empresa, statusEnum);
        }
        return ResponseEntity.ok(contas);
    }

    @PostMapping("/contas-receber/{id}/receber")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<ContaReceber> receberConta(
            @PathVariable Long id,
            @RequestBody ReceberContaRequest request) {

        ContaReceber conta = financeiroService.receberConta(
                id,
                request.dataRecebimento() != null ? request.dataRecebimento() : LocalDate.now(),
                request.meioPagamento());
        return ResponseEntity.ok(conta);
    }

    // ========================================
    // FLUXO DE CAIXA
    // ========================================

    @GetMapping("/fluxo-caixa")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<FluxoCaixaResponse> getFluxoCaixa(
            @RequestParam int mes,
            @RequestParam int ano,
            @AuthenticationPrincipal User usuario) {

        Empresa empresa = usuario.getEmpresa();
        if (empresa == null) {
            throw new BusinessException("Usuário não está vinculado a uma empresa");
        }

        YearMonth periodo = YearMonth.of(ano, mes);
        BigDecimal entradas = financeiroService.getTotalRecebidoNoPeriodo(empresa, periodo);
        BigDecimal saidas = financeiroService.getTotalPagoNoPeriodo(empresa, periodo);
        BigDecimal saldo = entradas.subtract(saidas);

        return ResponseEntity.ok(new FluxoCaixaResponse(
                periodo.toString(),
                entradas,
                saidas,
                saldo));
    }

    @GetMapping(value = "/fluxo-caixa/pdf", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    @io.swagger.v3.oas.annotations.Operation(summary = "Exportar Fluxo de Caixa em PDF", description = "Gera PDF do fluxo de caixa do período com entradas, saídas e saldo.")
    public ResponseEntity<byte[]> exportarFluxoCaixaPdf(
            @RequestParam int mes,
            @RequestParam int ano,
            @AuthenticationPrincipal User usuario) {

        Empresa empresa = usuario.getEmpresa();
        if (empresa == null) {
            throw new BusinessException("Usuário não está vinculado a uma empresa");
        }

        YearMonth periodo = YearMonth.of(ano, mes);
        BigDecimal entradas = financeiroService.getTotalRecebidoNoPeriodo(empresa, periodo);
        BigDecimal saidas = financeiroService.getTotalPagoNoPeriodo(empresa, periodo);

        byte[] pdfBytes = pdfService.gerarFluxoCaixaPdf(periodo, entradas, saidas, empresa);

        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=fluxo-caixa-" + ano + "-" + mes + ".pdf")
                .body(pdfBytes);
    }

    // ========================================
    // RECEITA POR CAIXA (BASE DAS)
    // ========================================

    @GetMapping("/receitas-caixa")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    @io.swagger.v3.oas.annotations.Operation(summary = "Relatório de Receita por Caixa", description = "Retorna lista detalhada de recebimentos do mês. BASE PARA O DAS (Simples Nacional).")
    public ResponseEntity<com.empresa.comissao.dto.ReceitaCaixaReportDTO> getReceitasCaixa(
            @RequestParam int mes,
            @RequestParam int ano,
            @AuthenticationPrincipal User usuario) {

        Empresa empresa = usuario.getEmpresa();
        if (empresa == null) {
            throw new BusinessException("Usuário não está vinculado a uma empresa");
        }

        YearMonth periodo = YearMonth.of(ano, mes);
        return ResponseEntity.ok(financeiroService.getReceitasCaixa(empresa, periodo));
    }

    // ========================================
    // RESUMO FINANCEIRO
    // ========================================

    @GetMapping("/resumo")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<FinanceiroService.ResumoFinanceiro> getResumo(
            @AuthenticationPrincipal User usuario) {

        Empresa empresa = usuario.getEmpresa();
        if (empresa == null) {
            throw new BusinessException("Usuário não está vinculado a uma empresa");
        }

        return ResponseEntity.ok(financeiroService.getResumoFinanceiro(empresa));
    }

    // ========================================
    // DTOs
    // ========================================

    public record PagarContaRequest(
            LocalDate dataPagamento,
            MeioPagamento meioPagamento) {
    }

    public record ReceberContaRequest(
            LocalDate dataRecebimento,
            MeioPagamento meioPagamento) {
    }

    public record FluxoCaixaResponse(
            String periodo,
            BigDecimal entradas,
            BigDecimal saidas,
            BigDecimal saldo) {
    }
}
