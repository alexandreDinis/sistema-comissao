package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.ComissaoCalculada;
import com.empresa.comissao.service.ComissaoService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("/api/v1/comissao")
@RequiredArgsConstructor
public class ComissaoController {

    private final ComissaoService comissaoService;

    @GetMapping("/{ano}/{mes}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<ComissaoResponse> obterComissaoMensal(@PathVariable int ano, @PathVariable int mes) {
        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        com.empresa.comissao.domain.entity.User usuario = null;

        if (authentication != null
                && authentication.getPrincipal() instanceof com.empresa.comissao.domain.entity.User) {
            usuario = (com.empresa.comissao.domain.entity.User) authentication.getPrincipal();
        }

        ComissaoCalculada comissao = comissaoService.calcularEObterComissaoMensal(ano, mes, usuario);

        ComissaoResponse response = new ComissaoResponse(
                comissao.getAnoMesReferencia().toString(),
                comissao.getFaturamentoMensalTotal(),
                comissao.getFaixaComissaoDescricao(),
                comissao.getPorcentagemComissaoAplicada(),
                comissao.getValorBrutoComissao(),
                comissao.getValorTotalAdiantamentos(),
                comissao.getSaldoAReceber());
        return ResponseEntity.ok(response);
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
    }
}
