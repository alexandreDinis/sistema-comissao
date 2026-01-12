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

    public DashboardStatsResponse getStats() {
        LocalDate now = LocalDate.now();
        LocalDate startOfMonth = YearMonth.from(now).atDay(1);
        LocalDate endOfMonth = YearMonth.from(now).atEndOfMonth();

        long activeOsCount = osRepository
                .countByStatusIn(List.of(StatusOrdemServico.ABERTA, StatusOrdemServico.EM_EXECUCAO));

        long finalizedMonthCount = osRepository.countByStatusAndDataBetween(
                StatusOrdemServico.FINALIZADA, startOfMonth, endOfMonth);

        long veiculosMonthCount = osRepository.countVeiculosByStatusAndData(
                StatusOrdemServico.FINALIZADA, startOfMonth, endOfMonth);

        long pecasMonthCount = osRepository.countPecasByStatusAndData(
                StatusOrdemServico.FINALIZADA, startOfMonth, endOfMonth);

        return DashboardStatsResponse.builder()
                .activeOsCount(activeOsCount)
                .finalizedMonthCount(finalizedMonthCount)
                .veiculosMonthCount(veiculosMonthCount)
                .pecasMonthCount(pecasMonthCount)
                .build();
    }
}
