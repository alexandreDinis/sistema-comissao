package com.empresa.comissao.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardStatsResponse {
    private long activeOsCount;
    private long finalizedMonthCount;
    private long veiculosMonthCount;
    private long pecasMonthCount;
}
