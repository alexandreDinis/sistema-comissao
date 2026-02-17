package com.empresa.comissao.domain.enums;

/**
 * Status de uma conta a pagar ou receber.
 * VENCIDO é derivado automaticamente na camada de serviço.
 */
public enum StatusConta {
    PENDENTE, // Aguardando pagamento/recebimento (nenhum valor recebido)
    PARCIAL, // Recebimento parcial: valor pago < valor total
    PAGO, // Totalmente quitado
    CANCELADO, // Cancelado/estornado
    BAIXADO // Calote/perdão: saldo zerado sem entrada de caixa
}
