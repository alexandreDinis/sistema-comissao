package com.empresa.comissao.domain.enums;

/**
 * Define o modo de cálculo e visualização de comissão para uma empresa.
 */
public enum ModoComissao {
    /**
     * Cada funcionário vê apenas seu próprio faturamento e comissão.
     */
    INDIVIDUAL,

    /**
     * Todos os funcionários veem o faturamento e comissão total da empresa.
     */
    COLETIVA
}
