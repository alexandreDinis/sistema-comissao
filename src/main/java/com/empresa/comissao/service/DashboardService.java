package com.empresa.comissao.service;

import com.empresa.comissao.domain.enums.StatusOrdemServico;
import com.empresa.comissao.dto.response.DashboardStatsResponse;
import com.empresa.comissao.repository.OrdemServicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

        private final OrdemServicoRepository osRepository;
        private final ComissaoService comissaoService;
        private final com.empresa.comissao.repository.ContaPagarRepository contaPagarRepository;
        private final com.empresa.comissao.repository.ContaReceberRepository contaReceberRepository;

        public com.empresa.comissao.dto.response.DashboardOverviewDTO getOverview(
                        com.empresa.comissao.domain.entity.User usuario, boolean isAdmin) {
                // 1. Resolve Company/Tenant
                com.empresa.comissao.domain.entity.Empresa empresa = resolveEmpresa(usuario);

                if (empresa == null) {
                        return com.empresa.comissao.dto.response.DashboardOverviewDTO.builder().build();
                }

                // 2. Get Stats (reuse internal logic with resolved empresa)
                DashboardStatsResponse stats;
                if (isAdmin) {
                        stats = getStatsGlobal(empresa);
                } else {
                        stats = getStatsPersonal(empresa, usuario);
                }

                // 3. Get YoY (Current Year/Month)
                LocalDate now = LocalDate.now();
                com.empresa.comissao.dto.ComparacaoFaturamentoDTO yoy = null;
                try {
                        // YoY permissions should also be checked? For now keep as is, but if employee
                        // sees global yoy it might be inconsistent.
                        // Assuming YoY follows stats logic:
                        yoy = comissaoService.obterComparacaoYoY(now.getYear(), now.getMonthValue(), usuario, empresa);
                } catch (Exception e) {
                        // Log error but don't fail the whole dashboard
                        System.err.println("Error calculating YoY for dashboard: " + e.getMessage());
                }

                // 4. Get Top 10 Pending Lists (Optimized)
                // Only for admins
                java.util.List<com.empresa.comissao.dto.list.ContaResumoDTO> contasPagar = java.util.Collections
                                .emptyList();
                java.util.List<com.empresa.comissao.dto.list.ContaResumoDTO> contasReceber = java.util.Collections
                                .emptyList();

                if (isAdmin) {
                        try {
                                org.springframework.data.domain.Pageable topTen = org.springframework.data.domain.PageRequest
                                                .of(0, 10);
                                contasPagar = contaPagarRepository.findTop10VencendoProximos(empresa, topTen);
                                contasReceber = contaReceberRepository.findTop10VencendoProximos(empresa, topTen);
                        } catch (Exception e) {
                                System.err.println("Error fetching dashboard lists: " + e.getMessage());
                        }
                }

                return com.empresa.comissao.dto.response.DashboardOverviewDTO.builder()
                                .stats(stats)
                                .faturamentoYoY(yoy)
                                .contasPagarVencendo(contasPagar)
                                .contasReceberVencendo(contasReceber)
                                .build();
        }

        public DashboardStatsResponse getStats(com.empresa.comissao.domain.entity.User usuario, boolean isAdmin) {
                com.empresa.comissao.domain.entity.Empresa empresa = resolveEmpresa(usuario);
                if (empresa == null) {
                        return DashboardStatsResponse.builder().build();
                }

                // Logic Split: Admin vs User
                if (isAdmin) {
                        return getStatsGlobal(empresa);
                } else {
                        return getStatsPersonal(empresa, usuario);
                }
        }

        private DashboardStatsResponse getStatsGlobal(com.empresa.comissao.domain.entity.Empresa empresa) {
                LocalDate now = LocalDate.now();
                LocalDate startOfMonth = YearMonth.from(now).atDay(1);
                LocalDate endOfMonth = YearMonth.from(now).atEndOfMonth();

                long activeOsCount = osRepository
                                .countByStatusInAndEmpresa(
                                                List.of(StatusOrdemServico.ABERTA, StatusOrdemServico.EM_EXECUCAO),
                                                empresa);

                long finalizedMonthCount = osRepository.countByStatusAndDataBetweenAndEmpresa(
                                StatusOrdemServico.FINALIZADA, startOfMonth, endOfMonth, empresa);

                long veiculosMonthCount = osRepository.countVeiculosByStatusAndDataAndEmpresa(
                                StatusOrdemServico.FINALIZADA, startOfMonth, endOfMonth, empresa);

                long pecasMonthCount = osRepository.countPecasByStatusAndDataAndEmpresa(
                                StatusOrdemServico.FINALIZADA, startOfMonth, endOfMonth, empresa);

                // Financial aggregated totals (using existing repository methods)
                java.math.BigDecimal totalAPagarPendente = contaPagarRepository.sumPendentesByEmpresa(empresa);
                java.math.BigDecimal totalAReceberPendente = contaReceberRepository.sumPendentesByEmpresa(empresa);

                LocalDate today = LocalDate.now();
                LocalDate plus7Days = today.plusDays(7);

                long contasPagarVencendo = contaPagarRepository.countVencendoProximos(empresa, today, plus7Days);
                long recebimentosVencendo = contaReceberRepository.countVencendoProximos(empresa, today, plus7Days);

                return buildResponse(activeOsCount, finalizedMonthCount, veiculosMonthCount, pecasMonthCount,
                                totalAPagarPendente, totalAReceberPendente, contasPagarVencendo, recebimentosVencendo);
        }

        private DashboardStatsResponse getStatsPersonal(com.empresa.comissao.domain.entity.Empresa empresa,
                        com.empresa.comissao.domain.entity.User usuario) {
                LocalDate now = LocalDate.now();
                LocalDate startOfMonth = YearMonth.from(now).atDay(1);
                LocalDate endOfMonth = YearMonth.from(now).atEndOfMonth();

                long activeOsCount = osRepository
                                .countByStatusInAndEmpresaAndUsuario(
                                                List.of(StatusOrdemServico.ABERTA, StatusOrdemServico.EM_EXECUCAO),
                                                empresa, usuario);

                long finalizedMonthCount = osRepository.countByStatusAndDataBetweenAndEmpresaAndUsuario(
                                StatusOrdemServico.FINALIZADA, startOfMonth, endOfMonth, empresa, usuario);

                long veiculosMonthCount = osRepository.countVeiculosByStatusAndDataAndEmpresaAndUsuario(
                                StatusOrdemServico.FINALIZADA, startOfMonth, endOfMonth, empresa, usuario);

                long pecasMonthCount = osRepository.countPecasByStatusAndDataAndEmpresaAndUsuario(
                                StatusOrdemServico.FINALIZADA, startOfMonth, endOfMonth, empresa, usuario);

                // Financials: Employees usually don't see full financials, but if they strictly
                // need to see 0 or filtered:
                // The requirement says "admin sees total". It implies employees see their own.
                // But Financials (Bills to Pay/Receive) are usually Company-level.
                // Strategy: Show 0 for personal view unless they have specific permission
                // (which we don't have granularity for yet).
                // Or, keep Global Financials but filtered OS stats?
                // Usually valid feedback: "Dashboard vehicle count".
                // Let's hide company financials for non-admins to be safe/consistent with
                // "Personal View".

                return buildResponse(activeOsCount, finalizedMonthCount, veiculosMonthCount, pecasMonthCount,
                                java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, 0, 0);
        }

        private DashboardStatsResponse buildResponse(long activeOs, long finalized, long veiculos, long pecas,
                        java.math.BigDecimal pagar, java.math.BigDecimal receber,
                        long pagarVencendo, long receberVencendo) {
                return DashboardStatsResponse.builder()
                                .activeOsCount(activeOs)
                                .finalizedMonthCount(finalized)
                                .veiculosMonthCount(veiculos)
                                .pecasMonthCount(pecas)
                                .totalAPagarPendente(pagar != null ? pagar : java.math.BigDecimal.ZERO)
                                .totalAReceberPendente(receber != null ? receber : java.math.BigDecimal.ZERO)
                                .contasPagarVencendoProximos7Dias(pagarVencendo)
                                .recebimentosVencendoProximos7Dias(receberVencendo)
                                .build();
        }

        private com.empresa.comissao.domain.entity.Empresa resolveEmpresa(
                        com.empresa.comissao.domain.entity.User usuario) {
                if (usuario != null && usuario.getEmpresa() != null) {
                        return usuario.getEmpresa();
                }
                Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
                if (tenantId != null) {
                        com.empresa.comissao.domain.entity.Empresa proxy = new com.empresa.comissao.domain.entity.Empresa();
                        proxy.setId(tenantId);
                        return proxy;
                }
                return null;
        }
}
