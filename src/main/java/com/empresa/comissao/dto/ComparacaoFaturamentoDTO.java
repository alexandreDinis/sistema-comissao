package com.empresa.comissao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComparacaoFaturamentoDTO {
    private int anoAtual;
    private int mesAtual;
    private BigDecimal faturamentoAtual;
    private BigDecimal faturamentoAnoAnterior;
    private BigDecimal diferencaAbsoluta;
    private BigDecimal diferencaPercentual;
    private boolean temDadosAnoAnterior;
}
