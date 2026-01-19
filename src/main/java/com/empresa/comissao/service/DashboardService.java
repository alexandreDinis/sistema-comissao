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

        public DashboardStatsResponse getStats(com.empresa.comissao.domain.entity.User usuario) {
                if (usuario == null || usuario.getEmpresa() == null) {
                        // SaaS Admin or unauthenticated - return empty stats or throw?
                        // For now return empty to prevent leak
                        return DashboardStatsResponse.builder().build();
                }

                com.empresa.comissao.domain.entity.Empresa empresa = usuario.getEmpresa();

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

                return DashboardStatsResponse.builder()
                                .activeOsCount(activeOsCount)
                                .finalizedMonthCount(finalizedMonthCount)
                                .veiculosMonthCount(veiculosMonthCount)
                                .pecasMonthCount(pecasMonthCount)
                                .build();
        }
}
