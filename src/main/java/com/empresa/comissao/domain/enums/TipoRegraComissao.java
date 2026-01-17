package com.empresa.comissao.domain.enums;

/**
 * Tipos de regras de comissionamento disponíveis para uma empresa.
 */
public enum TipoRegraComissao {
    /**
     * Comissão calculada por faixas de faturamento.
     * Quanto maior o faturamento, maior a porcentagem.
     */
    FAIXA_FATURAMENTO,

    /**
     * Porcentagem fixa definida individualmente para cada funcionário.
     */
    FIXA_FUNCIONARIO,

    /**
     * Porcentagem única aplicada a todos os funcionários da empresa.
     */
    FIXA_EMPRESA,

    /**
     * Combinação de faixas com ajustes individuais por funcionário.
     */
    HIBRIDA
}
