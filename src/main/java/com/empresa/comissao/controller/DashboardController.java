package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.dto.ComparacaoFaturamentoDTO;
import com.empresa.comissao.dto.response.DashboardStatsResponse;

import com.empresa.comissao.service.ComissaoService;
import com.empresa.comissao.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Tag(name = "Dashboard", description = "Dashboard Statistics")
public class DashboardController {

    private final DashboardService dashboardService;
    private final ComissaoService comissaoService;

    @GetMapping("/stats")
    @Operation(summary = "Get Dashboard Stats", description = "Returns active OSs, finalized OSs (month), distinct vehicles (month), and total parts (month).")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'USER')")
    public ResponseEntity<DashboardStatsResponse> getStats(
            @AuthenticationPrincipal com.empresa.comissao.security.AuthPrincipal principal,
            org.springframework.security.core.Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_ADMIN_EMPRESA")
                        || a.getAuthority().equals("ROLE_SUPER_ADMIN")
                        || a.getAuthority().equals("ROLE_ADMIN_LICENCA"));

        User usuario = new User();
        usuario.setId(principal.getUserId());
        usuario.setEmail(principal.getEmail());
        Empresa empresa = new Empresa();
        empresa.setId(principal.getTenantId());
        usuario.setEmpresa(empresa);

        return ResponseEntity.ok(dashboardService.getStats(usuario, isAdmin));
    }

    @GetMapping("/overview")
    @Operation(summary = "Get Dashboard Overview", description = "Returns consolidated dashboard data (stats + YoY) in a single request.")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'USER')")
    public ResponseEntity<com.empresa.comissao.dto.response.DashboardOverviewDTO> getOverview(
            @AuthenticationPrincipal com.empresa.comissao.security.AuthPrincipal principal,
            org.springframework.security.core.Authentication authentication) {

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                        || a.getAuthority().equals("ROLE_ADMIN_EMPRESA")
                        || a.getAuthority().equals("ROLE_SUPER_ADMIN")
                        || a.getAuthority().equals("ROLE_ADMIN_LICENCA"));

        User usuario = new User();
        usuario.setId(principal.getUserId());
        usuario.setEmail(principal.getEmail());
        Empresa empresa = new Empresa();
        empresa.setId(principal.getTenantId());
        usuario.setEmpresa(empresa);

        return ResponseEntity.ok(dashboardService.getOverview(usuario, isAdmin));
    }

    @GetMapping("/yoy/{ano}/{mes}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'USER', 'FUNCIONARIO')")
    @Operation(summary = "Obter comparação Year-over-Year", description = "Retorna comparação de faturamento entre o mês atual e o mesmo mês do ano anterior")
    public ResponseEntity<ComparacaoFaturamentoDTO> obterComparacaoYoY(
            @PathVariable int ano,
            @PathVariable int mes) {

        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
        if (tenantId == null) {
            return ResponseEntity.badRequest().build();
        }

        // Create proxy for Service/Repository
        Empresa empresaProxy = new Empresa();
        empresaProxy.setId(tenantId);

        // Service now handles null user correctly by prioritizing empresa
        ComparacaoFaturamentoDTO comparacao = comissaoService.obterComparacaoYoY(ano, mes, null, empresaProxy);
        return ResponseEntity.ok(comparacao);
    }
}
