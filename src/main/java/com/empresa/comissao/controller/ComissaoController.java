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
            @PathVariable int mes,
            @RequestParam(required = false, defaultValue = "false") boolean force) {

        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        com.empresa.comissao.domain.entity.User usuario = null;

        // 1. Tentar AuthPrincipal (Novo padrão JWT otimizado)
        if (authentication != null
                && authentication.getPrincipal() instanceof com.empresa.comissao.security.AuthPrincipal) {
            com.empresa.comissao.security.AuthPrincipal principal = (com.empresa.comissao.security.AuthPrincipal) authentication
                    .getPrincipal();
            Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();

            // Reconstruir Proxy User/Empresa para o Service Legado
            if (principal.getUserId() != null) {
                usuario = new com.empresa.comissao.domain.entity.User();
                usuario.setId(principal.getUserId());
                usuario.setEmail(principal.getEmail());
                usuario.setParticipaComissao(true); // Default to true or fetch if critical. For now assume true to
                                                    // avoid blockage.
                                                    // Better: Service checks this. We just pass ID.

                if (tenantId != null) {
                    Empresa empresaProxy = new Empresa();
                    empresaProxy.setId(tenantId);
                    usuario.setEmpresa(empresaProxy);
                }
            }
        }
        // 2. Fallback Entity (Legado / Testes)
        else if (authentication != null
                && authentication.getPrincipal() instanceof com.empresa.comissao.domain.entity.User) {
            usuario = (com.empresa.comissao.domain.entity.User) authentication.getPrincipal();
        }

        ComissaoCalculada comissao;

        // Fetch fresh empresa from DB to get latest modoComissao configuration
        if (usuario != null && usuario.getEmpresa() != null) {
            // Se tivermos apenas o ID (proxy), o findById vai buscar os dados completos
            // (modoComissao, etc)
            Empresa empresaFresh = empresaRepository.findById(usuario.getEmpresa().getId())
                    .orElse(usuario.getEmpresa());

            // Re-attach empresa to user to ensure consistency
            usuario.setEmpresa(empresaFresh);

            com.empresa.comissao.domain.enums.ModoComissao modo = empresaFresh.getModoComissao();
            log.info("📊 Modo Comissão da empresa {}: {}", empresaFresh.getNome(), modo);

            if (modo == com.empresa.comissao.domain.enums.ModoComissao.COLETIVA) {
                // Company-wide: all employees see total empresa data
                comissao = comissaoService.calcularComissaoEmpresaMensal(ano, mes, empresaFresh, force);
            } else {
                // Individual: each employee sees only their own data
                comissao = comissaoService.calcularEObterComissaoMensal(ano, mes, usuario, force);
            }
        } else {
            // Fallback for users without empresa (shouldn't happen in normal flow)
            // Se for AuthPrincipal sem empresa, vai cair aqui e pode dar erro se o service
            // exigir empresa.
            // Mas com TenantContext, usuario.getEmpresa() deve estar preenchido acima.
            comissao = comissaoService.calcularEObterComissaoMensal(ano, mes, usuario, force);
        }

        ComissaoResponse response = new ComissaoResponse(
                comissao.getAnoMesReferencia().toString(),
                comissao.getFaturamentoMensalTotal(),
                comissao.getFaixaComissaoDescricao(),
                comissao.getPorcentagemComissaoAplicada(),
                comissao.getValorBrutoComissao(),
                comissao.getValorTotalAdiantamentos(),
                comissao.getValorQuitado(),
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

    /**
     * Endpoint para gerar conta a pagar (Financeiro) referente a uma comissão.
     */
    @PostMapping("/gerar-pagamento/{id}")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<Void> gerarPagamentoComissao(
            @PathVariable Long id,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate dataVencimento) {

        if (dataVencimento == null) {
            dataVencimento = java.time.LocalDate.now();
        }

        log.info("💸 Gerando pagamento financeiro para comissão ID: {} - Vencimento: {}", id, dataVencimento);
        comissaoService.gerarPagamentoComissao(id, dataVencimento);
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint para Admin/Financeiro listar comissões de todos os funcionários.
     */
    @GetMapping("/empresa/{ano}/{mes}")
    @PreAuthorize("hasRole('ADMIN_EMPRESA')")
    public ResponseEntity<java.util.List<ComissaoFuncionarioResponse>> listarComissoesEmpresa(
            @PathVariable int ano,
            @PathVariable int mes,
            @RequestParam(required = false, defaultValue = "false") boolean force) {

        org.springframework.security.core.Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        com.empresa.comissao.domain.entity.User usuario = null;

        // 1. AuthPrincipal
        if (authentication != null
                && authentication.getPrincipal() instanceof com.empresa.comissao.security.AuthPrincipal) {
            com.empresa.comissao.security.AuthPrincipal principal = (com.empresa.comissao.security.AuthPrincipal) authentication
                    .getPrincipal();
            Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();

            if (principal.getUserId() != null) {
                usuario = new com.empresa.comissao.domain.entity.User();
                usuario.setId(principal.getUserId());
                if (tenantId != null) {
                    Empresa emp = new Empresa();
                    emp.setId(tenantId);
                    usuario.setEmpresa(emp);
                }
            }
        }
        // 2. Legacy Entity
        else if (authentication != null
                && authentication.getPrincipal() instanceof com.empresa.comissao.domain.entity.User) {
            usuario = (com.empresa.comissao.domain.entity.User) authentication.getPrincipal();
        }

        if (usuario == null || usuario.getEmpresa() == null) {
            return ResponseEntity.badRequest().build();
        }

        Empresa empresa = empresaRepository.findById(usuario.getEmpresa().getId())
                .orElse(usuario.getEmpresa());

        java.util.List<com.empresa.comissao.domain.entity.ComissaoCalculada> comissoes = comissaoService
                .listarComissoesEmpresa(ano, mes, empresa, force);

        java.util.List<ComissaoFuncionarioResponse> response = comissoes.stream()
                .map(c -> new ComissaoFuncionarioResponse(
                        c.getId(),
                        c.getUsuario() != null ? c.getUsuario().getId() : null,
                        c.getUsuario() != null ? c.getUsuario().getEmail().split("@")[0] : "Funcionário",
                        c.getUsuario() != null ? c.getUsuario().getEmail() : "",
                        c.getAnoMesReferencia().toString(),
                        c.getFaturamentoMensalTotal(),
                        c.getPorcentagemComissaoAplicada(),
                        c.getValorBrutoComissao(),
                        c.getValorTotalAdiantamentos(),
                        c.getValorQuitado(), // NOVO: Já Pago
                        c.getSaldoAReceber(),
                        c.getQuitado() != null && c.getQuitado(),
                        c.getDataQuitacao() != null ? c.getDataQuitacao().toString() : null))
                .collect(java.util.stream.Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @Getter
    @AllArgsConstructor
    public static class ComissaoFuncionarioResponse {
        private Long id;
        private Long funcionarioId;
        private String funcionarioNome;
        private String funcionarioEmail;
        private String anoMesReferencia;
        private BigDecimal faturamento;
        private BigDecimal porcentagem;
        private BigDecimal valorBruto;
        private BigDecimal adiantamentos;
        private BigDecimal valorQuitado; // NOVO
        private BigDecimal saldoAPagar;
        private boolean quitado;
        private String dataQuitacao;
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
        private BigDecimal valorQuitado; // NOVO: O que já foi pago
        private BigDecimal saldoAReceber;
        private BigDecimal saldoAnterior; // NOVO: Saldo do mês anterior (carryover)
        private Boolean quitado; // NOVO: Se foi pago
        private String dataQuitacao; // NOVO: Data do pagamento
    }
}
