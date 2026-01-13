package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.dto.ComparacaoFaturamentoDTO;
import com.empresa.comissao.dto.response.DashboardStatsResponse;
import com.empresa.comissao.repository.EmpresaRepository;
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
    private final EmpresaRepository empresaRepository;

    @GetMapping("/stats")
    @Operation(summary = "Get Dashboard Stats", description = "Returns active OSs, finalized OSs (month), distinct vehicles (month), and total parts (month).")
    public ResponseEntity<DashboardStatsResponse> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    @GetMapping("/yoy/{ano}/{mes}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ADMIN_EMPRESA', 'USER')")
    @Operation(summary = "Obter comparação Year-over-Year", description = "Retorna comparação de faturamento entre o mês atual e o mesmo mês do ano anterior")
    public ResponseEntity<ComparacaoFaturamentoDTO> obterComparacaoYoY(
            @PathVariable int ano,
            @PathVariable int mes,
            @AuthenticationPrincipal User usuario) {

        // Fetch fresh empresa to ensure tenant isolation
        Empresa empresaFresh = null;
        if (usuario != null && usuario.getEmpresa() != null) {
            empresaFresh = empresaRepository.findById(usuario.getEmpresa().getId())
                    .orElse(usuario.getEmpresa());
        }

        ComparacaoFaturamentoDTO comparacao = comissaoService.obterComparacaoYoY(ano, mes, usuario, empresaFresh);
        return ResponseEntity.ok(comparacao);
    }
}
