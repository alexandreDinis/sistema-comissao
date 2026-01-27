package com.empresa.comissao.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class RelatorioFluxoCaixaDTO {
    private String periodo; // "Janeiro/2024"
    private BigDecimal saldoInicial;
    private BigDecimal totalEntradas;
    private BigDecimal totalSaidas;
    private BigDecimal saldoFinal;

    // Optional: Detailed list needed for PDF?
    // User requested "Entradas do mês" and "Saídas do mês" in the PDF.
    // Usually a summary but user said "Detalhado" for Receita, and
    // "Entradas/Saidas" for Fluxo.
    // Let's provide lists just in case we want to list them.
    private List<ItemFluxoDTO> entradas;
    private List<ItemFluxoDTO> saidas;

    @Data
    @Builder
    public static class ItemFluxoDTO {
        private LocalDate data;
        private String descricao;
        private String categoria; // "Vendas", "Despesa Operacional", etc.
        private BigDecimal valor;
    }
}
