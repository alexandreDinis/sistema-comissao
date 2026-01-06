package com.empresa.comissao.domain.model;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

public class TabelaComissao {

        public static final List<FaixaComissao> FAIXAS_COMISSAO = Arrays.asList(
                        new FaixaComissao(new BigDecimal("0.00"), new BigDecimal("19999.99"), new BigDecimal("0.12"),
                                        "Até R$ 19.999,99"),
                        new FaixaComissao(new BigDecimal("20000.00"), new BigDecimal("24999.99"),
                                        new BigDecimal("0.15"),
                                        "R$ 20.000,00 a R$ 24.999,99"),
                        new FaixaComissao(new BigDecimal("25000.00"), new BigDecimal("29999.99"),
                                        new BigDecimal("0.18"),
                                        "R$ 25.000,00 a R$ 29.999,99"),
                        new FaixaComissao(new BigDecimal("30000.00"), new BigDecimal("34999.99"),
                                        new BigDecimal("0.20"),
                                        "R$ 30.000,00 a R$ 34.999,99"),
                        new FaixaComissao(new BigDecimal("35000.00"), null, new BigDecimal("0.25"),
                                        "Acima de R$ 35.000,00"));

        public static FaixaComissao getFaixaByFaturamento(BigDecimal faturamento) {
                for (FaixaComissao faixa : FAIXAS_COMISSAO) {
                        if (faixa.isInRange(faturamento)) {
                                return faixa;
                        }
                }
                throw new IllegalStateException("Faturamento fora das faixas de comissão definidas: " + faturamento);
        }
}
