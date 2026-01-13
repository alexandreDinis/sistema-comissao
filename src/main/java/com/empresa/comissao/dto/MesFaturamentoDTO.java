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
public class MesFaturamentoDTO {
    private int mes;
    private String nomeMes;
    private BigDecimal faturamento;
    private BigDecimal faturamentoAnoAnterior;
    private BigDecimal variacao;
    private BigDecimal variacaoPercentual;
}
