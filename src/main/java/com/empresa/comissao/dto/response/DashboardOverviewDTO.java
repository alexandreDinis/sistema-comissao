package com.empresa.comissao.dto.response;

import com.empresa.comissao.dto.ComparacaoFaturamentoDTO;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardOverviewDTO {
    private DashboardStatsResponse stats;
    private ComparacaoFaturamentoDTO faturamentoYoY;

    // Optimized lists for "Top Pending" widgets
    private java.util.List<com.empresa.comissao.dto.list.ContaResumoDTO> contasPagarVencendo;
    private java.util.List<com.empresa.comissao.dto.list.ContaResumoDTO> contasReceberVencendo;
}
