package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.ComissaoCalculada;
import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.repository.EmpresaRepository;
import com.empresa.comissao.service.ComissaoService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import org.springframework.security.access.prepost.PreAuthorize;

@Slf4j
@RestController
@RequestMapping("/api/v1/comissao")
@RequiredArgsConstructor
public class ComissaoController {

    private final ComissaoService comissaoService;
    private final EmpresaRepository empresaRepository;

    @GetMapping("/{ano}/{mes}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'ADMIN_EMPRESA', 'FUNCIONARIO')")
    public ResponseEntity<ComissaoResponse> obterComissaoMensal(
            @PathVariable int ano,
            @PathVariable int mes) {

        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        com.empresa.comissao.domain.entity.User usuario = null;

        if (authentication != null
                && authentication.getPrincipal() instanceof com.empresa.comissao.domain.entity.User) {
            usuario = (com.empresa.comissao.domain.entity.User) authentication.getPrincipal();
        }

        ComissaoCalculada comissao;

        // Fetch fresh empresa from DB to get latest modoComissao configuration
        if (usuario != null && usuario.getEmpresa() != null) {
            Empresa empresaFresh = empresaRepository.findById(usuario.getEmpresa().getId())
                    .orElse(usuario.getEmpresa());

            com.empresa.comissao.domain.enums.ModoComissao modo = empresaFresh.getModoComissao();
            log.info("📊 Modo Comissão da empresa {}: {}", empresaFresh.getNome(), modo);

            if (modo == com.empresa.comissao.domain.enums.ModoComissao.COLETIVA) {
                // Company-wide: all employees see total empresa data
                comissao = comissaoService.calcularComissaoEmpresaMensal(ano, mes, empresaFresh);
            } else {
                // Individual: each employee sees only their own data
                comissao = comissaoService.calcularEObterComissaoMensal(ano, mes, usuario);
            }
        } else {
            // Fallback for users without empresa (shouldn't happen in normal flow)
            comissao = comissaoService.calcularEObterComissaoMensal(ano, mes, usuario);
        }

        ComissaoResponse response = new ComissaoResponse(
                comissao.getAnoMesReferencia().toString(),
                comissao.getFaturamentoMensalTotal(),
                comissao.getFaixaComissaoDescricao(),
                comissao.getPorcentagemComissaoAplicada(),
                comissao.getValorBrutoComissao(),
                comissao.getValorTotalAdiantamentos(),
                comissao.getSaldoAReceber(),
                comissao.getSaldoAnterior(),
                comissao.getQuitado(),
                comissao.getDataQuitacao() != null ? comissao.getDataQuitacao().toString() : null);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint para quitar uma comissão (marcar como paga).
     * Disponível apenas para ADMIN_EMPRESA.
     */
    @PostMapping("/quitar/{id}")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<String> quitarComissao(@PathVariable Long id) {
        log.info("💸 Quitando comissão ID: {}", id);
        comissaoService.quitarComissao(id);
        return ResponseEntity.ok("Comissão quitada com sucesso.");
    }

    @Getter
    @AllArgsConstructor
    public static class ComissaoResponse {
        private String anoMesReferencia;
        private BigDecimal faturamentoMensal;
        private String faixaComissao;
        private BigDecimal porcentagemComissao;
        private BigDecimal valorBrutoComissao;
        private BigDecimal valorAdiantado;
        private BigDecimal saldoAReceber;
        private BigDecimal saldoAnterior; // NOVO: Saldo do mês anterior (carryover)
        private Boolean quitado; // NOVO: Se foi pago
        private String dataQuitacao; // NOVO: Data do pagamento
    }
}
