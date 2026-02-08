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
                        com.empresa.comissao.domain.entity.User usuario) {
                // 1. Resolve Company/Tenant
                com.empresa.comissao.domain.entity.Empresa empresa = resolveEmpresa(usuario);

                if (empresa == null) {
                        return com.empresa.comissao.dto.response.DashboardOverviewDTO.builder().build();
                }

                // 2. Get Stats (reuse internal logic with resolved empresa)
                DashboardStatsResponse stats = getStatsInternal(empresa);

                // 3. Get YoY (Current Year/Month)
                LocalDate now = LocalDate.now();
                com.empresa.comissao.dto.ComparacaoFaturamentoDTO yoy = null;
                try {
                        yoy = comissaoService.obterComparacaoYoY(now.getYear(), now.getMonthValue(), usuario, empresa);
                } catch (Exception e) {
                        // Log error but don't fail the whole dashboard
                        System.err.println("Error calculating YoY for dashboard: " + e.getMessage());
                }

                // 4. Get Top 10 Pending Lists (Optimized)
                java.util.List<com.empresa.comissao.dto.list.ContaResumoDTO> contasPagar = java.util.Collections
                                .emptyList();
                java.util.List<com.empresa.comissao.dto.list.ContaResumoDTO> contasReceber = java.util.Collections
                                .emptyList();

                try {
                        org.springframework.data.domain.Pageable topTen = org.springframework.data.domain.PageRequest
                                        .of(0, 10);
                        contasPagar = contaPagarRepository.findTop10VencendoProximos(empresa, topTen);
                        contasReceber = contaReceberRepository.findTop10VencendoProximos(empresa, topTen);
                } catch (Exception e) {
                        System.err.println("Error fetching dashboard lists: " + e.getMessage());
                }

                return com.empresa.comissao.dto.response.DashboardOverviewDTO.builder()
                                .stats(stats)
                                .faturamentoYoY(yoy)
                                .contasPagarVencendo(contasPagar)
                                .contasReceberVencendo(contasReceber)
                                .build();
        }

        public DashboardStatsResponse getStats(com.empresa.comissao.domain.entity.User usuario) {
                com.empresa.comissao.domain.entity.Empresa empresa = resolveEmpresa(usuario);
                if (empresa == null) {
                        return DashboardStatsResponse.builder().build();
                }
                return getStatsInternal(empresa);
        }

        private DashboardStatsResponse getStatsInternal(com.empresa.comissao.domain.entity.Empresa empresa) {
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

                return DashboardStatsResponse.builder()
                                .activeOsCount(activeOsCount)
                                .finalizedMonthCount(finalizedMonthCount)
                                .veiculosMonthCount(veiculosMonthCount)
                                .pecasMonthCount(pecasMonthCount)
                                .totalAPagarPendente(totalAPagarPendente != null ? totalAPagarPendente
                                                : java.math.BigDecimal.ZERO)
                                .totalAReceberPendente(totalAReceberPendente != null ? totalAReceberPendente
                                                : java.math.BigDecimal.ZERO)
                                .contasPagarVencendoProximos7Dias(contasPagarVencendo)
                                .recebimentosVencendoProximos7Dias(recebimentosVencendo)
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
