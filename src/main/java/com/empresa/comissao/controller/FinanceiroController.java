package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.*;
import com.empresa.comissao.domain.enums.*;
import com.empresa.comissao.service.FinanceiroService;
import com.empresa.comissao.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import com.empresa.comissao.domain.entity.Recebimento;

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

    // ========================================
    // CONTAS A PAGAR
    // ========================================

    @GetMapping("/contas-pagar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<List<ContaPagar>> listarContasPagar(
            @RequestParam(required = false) String status) {

        Empresa empresa = resolveEmpresa();
        if (empresa == null) {
            throw new BusinessException("Empresa não encontrada no contexto");
        }

        List<ContaPagar> contas;
        if ("PENDENTE".equalsIgnoreCase(status)) {
            contas = financeiroService.listarContasPagarPendentes(empresa);
        } else if ("VENCIDO".equalsIgnoreCase(status)) {
            contas = financeiroService.listarContasPagarVencidas(empresa);
        } else if ("PAGO".equalsIgnoreCase(status)) {
            contas = financeiroService.listarContasPagarPagas(empresa);
        } else {
            // Sem filtro: retorna todas
            contas = financeiroService.listarTodasContasPagar(empresa);
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
            @RequestParam(required = false) String status) {

        Empresa empresa = resolveEmpresa();
        if (empresa == null) {
            throw new BusinessException("Empresa não encontrada no contexto");
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

    @PostMapping("/contas-receber/{id}/recebimento-parcial")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<ContaReceber> registrarRecebimentoParcial(
            @PathVariable Long id,
            @RequestBody RecebimentoParcialRequest request) {

        ContaReceber conta = financeiroService.registrarRecebimentoParcial(
                id,
                request.valor(),
                request.dataRecebimento() != null ? request.dataRecebimento() : LocalDate.now(),
                request.meioPagamento(),
                request.observacao());
        return ResponseEntity.ok(conta);
    }

    @PostMapping("/contas-receber/{id}/baixar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<ContaReceber> baixarSaldo(
            @PathVariable Long id,
            @RequestBody BaixarSaldoRequest request) {

        ContaReceber conta = financeiroService.baixarSaldo(id, request.motivo());
        return ResponseEntity.ok(conta);
    }

    @DeleteMapping("/contas-receber/recebimentos/{recebimentoId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<ContaReceber> estornarRecebimento(
            @PathVariable Long recebimentoId) {

        ContaReceber conta = financeiroService.estornarRecebimento(recebimentoId);
        return ResponseEntity.ok(conta);
    }

    @GetMapping("/contas-receber/{id}/recebimentos")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<List<Recebimento>> listarRecebimentos(
            @PathVariable Long id) {

        return ResponseEntity.ok(financeiroService.listarRecebimentos(id));
    }

    @PatchMapping("/contas-receber/{id}/vencimento")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<ContaReceber> atualizarVencimento(
            @PathVariable Long id,
            @RequestBody AtualizarVencimentoRequest request) {

        ContaReceber conta = financeiroService.atualizarVencimento(id, request.novaDataVencimento());
        return ResponseEntity.ok(conta);
    }

    // ========================================
    // FLUXO DE CAIXA
    // ========================================

    @GetMapping("/fluxo-caixa")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<FluxoCaixaResponse> getFluxoCaixa(
            @RequestParam int mes,
            @RequestParam int ano) {

        Empresa empresa = resolveEmpresa();
        if (empresa == null) {
            throw new BusinessException("Empresa não encontrada no contexto");
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
            @RequestParam int ano) {

        Empresa empresa = resolveEmpresa();
        if (empresa == null) {
            throw new BusinessException("Empresa não encontrada no contexto");
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
            @RequestParam int ano) {

        Empresa empresa = resolveEmpresa();
        if (empresa == null) {
            throw new BusinessException("Empresa não encontrada no contexto");
        }

        YearMonth periodo = YearMonth.of(ano, mes);
        return ResponseEntity.ok(financeiroService.getReceitasCaixa(empresa, periodo));
    }

    // ========================================
    // RESUMO FINANCEIRO
    // ========================================

    @GetMapping("/resumo")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<FinanceiroService.ResumoFinanceiro> getResumo() {

        Empresa empresa = resolveEmpresa();
        if (empresa == null) {
            throw new BusinessException("Empresa não encontrada no contexto");
        }

        return ResponseEntity.ok(financeiroService.getResumoFinanceiro(empresa));
    }

    // ========================================
    // DISTRIBUIÇÃO DE LUCROS
    // ========================================

    @GetMapping("/distribuicao-lucros")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<List<ContaPagar>> listarDistribuicoesLucro() {

        Empresa empresa = resolveEmpresa();
        if (empresa == null) {
            throw new BusinessException("Empresa não encontrada no contexto");
        }

        return ResponseEntity.ok(financeiroService.listarDistribuicoesLucro(empresa));
    }

    @PostMapping("/distribuicao-lucros")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<ContaPagar> criarDistribuicaoLucros(
            @RequestBody DistribuicaoLucrosRequest request) {

        Empresa empresa = resolveEmpresa();
        if (empresa == null) {
            throw new BusinessException("Empresa não encontrada no contexto");
        }

        ContaPagar conta = financeiroService.criarDistribuicaoLucros(
                empresa,
                request.valor(),
                request.dataCompetencia(),
                request.dataVencimento(),
                request.descricao());

        return ResponseEntity.ok(conta);
    }

    // ========================================
    // PAGAMENTO DE IMPOSTO (DAS)
    // ========================================

    @GetMapping("/imposto-pago")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<List<ContaPagar>> listarImpostosPagos() {

        Empresa empresa = resolveEmpresa();
        if (empresa == null) {
            throw new BusinessException("Empresa não encontrada no contexto");
        }

        return ResponseEntity.ok(financeiroService.listarImpostosPagos(empresa));
    }

    @PostMapping("/imposto-pago")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    public ResponseEntity<ContaPagar> criarImpostoPago(
            @RequestBody ImpostoPagoRequest request) {

        Empresa empresa = resolveEmpresa();
        if (empresa == null) {
            throw new BusinessException("Empresa não encontrada no contexto");
        }

        ContaPagar conta = financeiroService.criarImpostoPago(
                empresa,
                request.valor(),
                request.dataCompetencia(),
                request.dataVencimento(),
                request.descricao());

        return ResponseEntity.ok(conta);
    }

    private Empresa resolveEmpresa() {
        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
        if (tenantId != null) {
            // Return Proxy if services handle it, or fetch simple reference
            // Given the complexity of finance service, a proxy with ID is usually enough
            // UNLESS the service accesses properties like 'modoComissao' etc immediately
            // without reloading.
            // Safe bet: Proxy. If logic fails, we upgrade to fetch.
            Empresa e = new Empresa();
            e.setId(tenantId);
            return e;
        }
        return null;
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

    public record DistribuicaoLucrosRequest(
            BigDecimal valor,
            LocalDate dataCompetencia,
            LocalDate dataVencimento,
            String descricao) {
    }

    public record ImpostoPagoRequest(
            BigDecimal valor,
            LocalDate dataCompetencia,
            LocalDate dataVencimento,
            String descricao) {
    }

    public record RecebimentoParcialRequest(
            BigDecimal valor,
            LocalDate dataRecebimento,
            MeioPagamento meioPagamento,
            String observacao) {
    }

    public record BaixarSaldoRequest(
            String motivo) {
    }

    public record AtualizarVencimentoRequest(
            LocalDate novaDataVencimento) {
    }
}
