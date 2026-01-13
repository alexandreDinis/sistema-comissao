package com.empresa.comissao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioAnualDTO {
    private int ano;
    private List<MesFaturamentoDTO> mesesComFaturamento;
    private BigDecimal faturamentoTotalAno;
    private BigDecimal faturamentoTotalAnoAnterior;
    private BigDecimal diferencaAnual;
    private BigDecimal crescimentoPercentualAnual;
}
