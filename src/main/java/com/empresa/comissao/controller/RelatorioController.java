package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.dto.RelatorioFinanceiroDTO;
import com.empresa.comissao.dto.RelatorioAnualDTO;
import com.empresa.comissao.repository.EmpresaRepository;
import com.empresa.comissao.service.ComissaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
        private final com.empresa.comissao.service.PdfService pdfService;
        private final EmpresaRepository empresaRepository;

        @GetMapping("/{ano}/{mes}")
        @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
        @Operation(summary = "Gerar relatório financeiro mensal", description = "Retorna um resumo contendo despesas por categoria, imposto (alíquota configurável por empresa) e comissão a pagar.")
        public ResponseEntity<RelatorioFinanceiroDTO> gerarRelatorio(
                        @PathVariable int ano,
                        @PathVariable int mes,
                        @AuthenticationPrincipal User usuario) {

                // Fetch fresh empresa to get latest modoComissao configuration
                Empresa empresaFresh = null;
                if (usuario != null && usuario.getEmpresa() != null) {
                        empresaFresh = empresaRepository.findById(usuario.getEmpresa().getId())
                                        .orElse(usuario.getEmpresa());
                }

                RelatorioFinanceiroDTO relatorio = comissaoService.gerarRelatorioFinanceiro(ano, mes, usuario,
                                empresaFresh);
                return ResponseEntity.ok(relatorio);
        }

        @GetMapping(value = "/{ano}/{mes}/pdf", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
        @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
        @Operation(summary = "Exportar relatório em PDF")
        public ResponseEntity<byte[]> gerarRelatorioPdf(
                        @PathVariable int ano,
                        @PathVariable int mes,
                        @AuthenticationPrincipal User usuario) {

                // Fetch fresh empresa to get latest modoComissao configuration
                Empresa empresaFresh = null;
                if (usuario != null && usuario.getEmpresa() != null) {
                        empresaFresh = empresaRepository.findById(usuario.getEmpresa().getId())
                                        .orElse(usuario.getEmpresa());
                }

                RelatorioFinanceiroDTO relatorio = comissaoService.gerarRelatorioFinanceiro(ano, mes, usuario,
                                empresaFresh);
                byte[] pdfBytes = pdfService.gerarRelatorioFinanceiroPdf(relatorio, empresaFresh);

                return ResponseEntity.ok()
                                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=relatorio-" + ano + "-" + mes + ".pdf")
                                .body(pdfBytes);
        }

        @GetMapping("/anual/{ano}")
        @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
        @Operation(summary = "Gerar relatório anual", description = "Retorna relatório consolidado de todos os meses do ano com comparações YoY")
        public ResponseEntity<RelatorioAnualDTO> gerarRelatorioAnual(
                        @PathVariable int ano,
                        @AuthenticationPrincipal User usuario) {

                // Fetch fresh empresa
                Empresa empresaFresh = null;
                if (usuario != null && usuario.getEmpresa() != null) {
                        empresaFresh = empresaRepository.findById(usuario.getEmpresa().getId())
                                        .orElse(usuario.getEmpresa());
                }

                RelatorioAnualDTO relatorio = comissaoService.gerarRelatorioAnual(ano, usuario, empresaFresh);
                return ResponseEntity.ok(relatorio);
        }

        @GetMapping(value = "/anual/{ano}/pdf", produces = org.springframework.http.MediaType.APPLICATION_PDF_VALUE)
        @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
        @Operation(summary = "Exportar relatório anual em PDF")
        public ResponseEntity<byte[]> gerarRelatorioAnualPdf(
                        @PathVariable int ano,
                        @AuthenticationPrincipal User usuario) {

                // Fetch fresh empresa
                Empresa empresaFresh = null;
                if (usuario != null && usuario.getEmpresa() != null) {
                        empresaFresh = empresaRepository.findById(usuario.getEmpresa().getId())
                                        .orElse(usuario.getEmpresa());
                }

                RelatorioAnualDTO relatorio = comissaoService.gerarRelatorioAnual(ano, usuario, empresaFresh);
                byte[] pdfBytes = pdfService.gerarRelatorioAnualPdf(relatorio, empresaFresh);

                return ResponseEntity.ok()
                                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION,
                                                "attachment; filename=relatorio-anual-" + ano + ".pdf")
                                .body(pdfBytes);
        }

        @GetMapping(value = "/ranking-clientes")
        @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA')")
        @Operation(summary = "Ranking de Clientes", description = "Retorna ranking de clientes por faturamento e quantidade de OSs")
        public ResponseEntity<java.util.List<com.empresa.comissao.dto.response.RankingClienteDTO>> getRankingClientes(
                        @org.springframework.web.bind.annotation.RequestParam int ano,
                        @org.springframework.web.bind.annotation.RequestParam(required = false) Integer mes,
                        @AuthenticationPrincipal User usuario) {

                // Fetch fresh empresa
                Empresa empresaFresh = null;
                if (usuario != null && usuario.getEmpresa() != null) {
                        empresaFresh = empresaRepository.findById(usuario.getEmpresa().getId())
                                        .orElse(usuario.getEmpresa());
                }

                return ResponseEntity.ok(comissaoService.gerarRankingClientes(ano, mes, usuario, empresaFresh));
        }
}
