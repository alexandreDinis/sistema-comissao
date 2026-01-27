package com.empresa.comissao.domain.enums;

/**
 * Tipos de remuneração disponíveis para funcionários.
 * Permite ao admin da empresa definir como cada funcionário é pago.
 */
public enum TipoRemuneracao {
    /**
     * Funcionário recebe apenas comissão, calculada pela regra da empresa.
     */
    COMISSAO,

    /**
     * Funcionário recebe salário fixo mensal, sem comissão.
     */
    SALARIO_FIXO,

    /**
     * Funcionário recebe salário base + porcentagem de comissão adicional.
     */
    MISTA
}
