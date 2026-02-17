package com.empresa.comissao.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private long activeOsCount;
    private long finalizedMonthCount;
    private long veiculosMonthCount;
    private long pecasMonthCount;

    // Financial aggregated totals
    private BigDecimal totalAPagarPendente;
    private BigDecimal totalAReceberPendente;
    private long contasPagarVencendoProximos7Dias;
    private long recebimentosVencendoProximos7Dias;
}
