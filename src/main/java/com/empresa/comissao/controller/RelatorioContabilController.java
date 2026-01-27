package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.dto.*;
import com.empresa.comissao.repository.EmpresaRepository;
import com.empresa.comissao.service.FinanceiroService;
import com.empresa.comissao.service.PdfService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/relatorios/contabeis")
@RequiredArgsConstructor
@Tag(name = "Relatórios Contábeis", description = "Endpoints para geração de relatórios contábeis e fiscais (PDF)")
@Slf4j
public class RelatorioContabilController {

    private final FinanceiroService financeiroService;
    private final PdfService pdfService;
    private final EmpresaRepository empresaRepository;

    @GetMapping(value = "/receita-caixa", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'CONTADOR')")
    @Operation(summary = "Exportar Relatório de Receita por Caixa (Base DAS)")
    public ResponseEntity<byte[]> gerarRelatorioReceitaCaixa(
            @RequestParam int ano,
            @RequestParam int mes,
            @AuthenticationPrincipal User usuario) {

        log.info("📄 Solicitado Relatório de Receita por Caixa: {}/{}", mes, ano);
        Empresa empresa = getEmpresaFresh(usuario);
        YearMonth periodo = YearMonth.of(ano, mes);

        RelatorioReceitaCaixaDTO relatorio = financeiroService.getRelatorioReceitaCaixaDetalhada(empresa, periodo);
        byte[] pdfBytes = pdfService.gerarRelatorioReceitaCaixaPdf(relatorio, empresa);

        return gerarResponsePdf(pdfBytes, "receita-caixa-" + ano + "-" + mes + ".pdf");
    }

    @GetMapping(value = "/fluxo-caixa", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Exportar Fluxo de Caixa Mensal Detalhado")
    public ResponseEntity<byte[]> gerarRelatorioFluxoCaixa(
            @RequestParam int ano,
            @RequestParam int mes,
            @AuthenticationPrincipal User usuario) {

        log.info("📄 Solicitado Fluxo de Caixa Detalhado: {}/{}", mes, ano);
        Empresa empresa = getEmpresaFresh(usuario);
        YearMonth periodo = YearMonth.of(ano, mes);

        RelatorioFluxoCaixaDTO relatorio = financeiroService.getRelatorioFluxoCaixaMensal(empresa, periodo);
        byte[] pdfBytes = pdfService.gerarRelatorioFluxoCaixaDetalhadoPdf(relatorio, empresa);

        return gerarResponsePdf(pdfBytes, "fluxo-caixa-" + ano + "-" + mes + ".pdf");
    }

    @GetMapping(value = "/contas-pagar", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Exportar Relatório de Contas a Pagar")
    public ResponseEntity<byte[]> gerarRelatorioContasPagar(
            @RequestParam int ano,
            @RequestParam int mes,
            @AuthenticationPrincipal User usuario) {

        log.info("📄 Solicitado Relatório Contas a Pagar: {}/{}", mes, ano);
        Empresa empresa = getEmpresaFresh(usuario);
        YearMonth periodo = YearMonth.of(ano, mes);

        RelatorioContasPagarDTO relatorio = financeiroService.getRelatorioContasPagar(empresa, periodo);
        byte[] pdfBytes = pdfService.gerarRelatorioContasPagarPdf(relatorio, empresa);

        return gerarResponsePdf(pdfBytes, "contas-pagar-" + ano + "-" + mes + ".pdf");
    }

    @GetMapping(value = "/distribuicao-lucros", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
    @Operation(summary = "Exportar Demonstrativo de Distribuição de Lucros")
    public ResponseEntity<byte[]> gerarRelatorioDistribuicaoLucros(
            @RequestParam int ano,
            @RequestParam int mes,
            @AuthenticationPrincipal User usuario) {

        log.info("📄 Solicitado Relatório Distribuição Lucros: {}/{}", mes, ano);
        Empresa empresa = getEmpresaFresh(usuario);
        YearMonth periodo = YearMonth.of(ano, mes);

        RelatorioDistribuicaoLucrosDTO relatorio = financeiroService.getRelatorioDistribuicaoLucros(empresa, periodo);
        byte[] pdfBytes = pdfService.gerarRelatorioDistribuicaoLucrosPdf(relatorio, empresa);

        return gerarResponsePdf(pdfBytes, "distribuicao-lucros-" + ano + "-" + mes + ".pdf");
    }

    private Empresa getEmpresaFresh(User usuario) {
        if (usuario != null && usuario.getEmpresa() != null) {
            return empresaRepository.findById(usuario.getEmpresa().getId())
                    .orElse(usuario.getEmpresa());
        }
        return null;
    }

    private ResponseEntity<byte[]> gerarResponsePdf(byte[] pdfBytes, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }
}
