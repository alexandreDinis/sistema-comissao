package com.empresa.comissao.domain.model;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class TabelaComissao {

    public static final List<FaixaComissao> FAIXAS_COMISSAO = Arrays.asList(
            new FaixaComissao(new BigDecimal("0.00"), new BigDecimal("16000.00"), new BigDecimal("0.12"),
                    "Até R$ 16.000,00"),
            new FaixaComissao(new BigDecimal("16000.01"), new BigDecimal("19996.00"), new BigDecimal("0.12"),
                    "R$ 16.000,01 a R$ 19.996,00"),
            new FaixaComissao(new BigDecimal("19996.01"), new BigDecimal("20000.00"), new BigDecimal("0.15"),
                    "R$ 19.996,01 a R$ 20.000,00"),
            new FaixaComissao(new BigDecimal("20000.01"), new BigDecimal("24996.00"), new BigDecimal("0.15"),
                    "R$ 20.000,01 a R$ 24.996,00"),
            new FaixaComissao(new BigDecimal("24996.01"), new BigDecimal("25020.00"), new BigDecimal("0.18"),
                    "R$ 24.996,01 a R$ 25.020,00"),
            new FaixaComissao(new BigDecimal("25020.01"), new BigDecimal("29996.00"), new BigDecimal("0.18"),
                    "R$ 25.020,01 a R$ 29.996,00"),
            new FaixaComissao(new BigDecimal("29996.01"), new BigDecimal("30000.00"), new BigDecimal("0.20"),
                    "R$ 29.996,01 a R$ 30.000,00"),
            new FaixaComissao(new BigDecimal("30000.01"), new BigDecimal("34996.00"), new BigDecimal("0.20"),
                    "R$ 30.000,01 a R$ 34.996,00"),
            new FaixaComissao(new BigDecimal("35000.00"), null, new BigDecimal("0.25"), "Acima de R$ 35.000,00"));

    public static FaixaComissao getFaixaByFaturamento(BigDecimal faturamento) {
        for (FaixaComissao faixa : FAIXAS_COMISSAO) {
            if (faixa.isInRange(faturamento)) {
                return faixa;
            }
        }
        throw new IllegalStateException("Faturamento fora das faixas de comissão definidas: " + faturamento);
    }
}
