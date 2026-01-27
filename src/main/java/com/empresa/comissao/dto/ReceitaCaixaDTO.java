package com.empresa.comissao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para um recebimento individual.
 * Utilizado no relatório de Receita por Caixa (base do DAS).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceitaCaixaDTO {
    private LocalDate dataRecebimento;
    private BigDecimal valor;
    private String origem; // "OS #123", "Faturamento Manual"
    private String cliente; // Nome do cliente (se aplicável)
    private String meioPagamento; // PIX, DINHEIRO, etc.
    private String documentoFiscal; // NF-e, NFS-e, Recibo (opcional)
}
