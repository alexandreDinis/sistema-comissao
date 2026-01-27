package com.empresa.comissao.domain.enums;

/**
 * Tipo de conta a pagar (Visão Caixa).
 */
public enum TipoContaPagar {
    OPERACIONAL, // Fornecedores, Aluguel, Luz, etc.
    FOLHA_PAGAMENTO, // Salários, Adiantamentos, Comissões
    IMPOSTOS, // Pagamento de guias (DAS, DARF)
    EMPRESTIMOS, // Amortização de empréstimos
    DISTRIBUICAO_LUCROS, // Retirada de sócios (Dividendos)

    // Legacy / Specific
    FATURA_CARTAO, // Fatura de cartão (Operational wrapper)
    OUTROS
}
