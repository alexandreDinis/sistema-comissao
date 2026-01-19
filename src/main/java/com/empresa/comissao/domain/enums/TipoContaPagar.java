package com.empresa.comissao.domain.enums;

/**
 * Tipo de conta a pagar.
 */
public enum TipoContaPagar {
    DESPESA_OPERACIONAL, // Alimentação, combustível, etc.
    COMISSAO_FUNCIONARIO, // Pagamento de comissão
    ADIANTAMENTO, // Adiantamento a funcionário
    SALARIO, // Salário fixo
    FORNECEDOR, // Pagamento a fornecedor
    IMPOSTO, // Impostos
    FATURA_CARTAO, // Fatura de cartão de crédito
    OUTROS
}
