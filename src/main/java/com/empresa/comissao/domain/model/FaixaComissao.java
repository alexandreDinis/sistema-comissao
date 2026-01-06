package com.empresa.comissao.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class FaixaComissao {
    private final BigDecimal minFaturamento;
    private final BigDecimal maxFaturamento;
    private final BigDecimal porcentagem;
    private final String descricao;

    public boolean isInRange(BigDecimal faturamento) {
        boolean greaterThanOrEqualMin = faturamento.compareTo(minFaturamento) >= 0;
        boolean lessThanOrEqualMax = (maxFaturamento == null || faturamento.compareTo(maxFaturamento) <= 0);
        return greaterThanOrEqualMin && lessThanOrEqualMax;
    }
}
