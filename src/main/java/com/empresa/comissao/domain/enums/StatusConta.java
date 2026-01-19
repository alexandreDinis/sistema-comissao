package com.empresa.comissao.domain.enums;

/**
 * Status de uma conta a pagar ou receber.
 * VENCIDO é derivado automaticamente na camada de serviço.
 */
public enum StatusConta {
    PENDENTE, // Aguardando pagamento/recebimento
    PAGO, // Já foi pago/recebido
    CANCELADO // Cancelado/estornado
}
