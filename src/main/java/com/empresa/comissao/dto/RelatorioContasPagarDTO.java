package com.empresa.comissao.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class RelatorioContasPagarDTO {
    private String periodo;
    private BigDecimal totalPago;
    private BigDecimal totalPendente;
    private BigDecimal totalVencido;

    private List<ItemContaPagarDTO> itens;

    @Data
    @Builder
    public static class ItemContaPagarDTO {
        private LocalDate dataVencimento;
        private LocalDate dataPagamento; // Null if pending
        private BigDecimal valor;
        private String descricao;
        private String fornecedor; // Or Beneficiario
        private String tipo; // OPERACIONAL, FOLHA, ETC.
        private String status; // PAGO, PENDENTE, ATRASADO
    }
}
