package com.empresa.comissao.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class RelatorioDistribuicaoLucrosDTO {
    private String periodo;
    private BigDecimal totalMes;
    private BigDecimal totalAcumuladoAno;

    private List<ItemDistribuicaoDTO> distribuicoes;

    @Data
    @Builder
    public static class ItemDistribuicaoDTO {
        private LocalDate data;
        private BigDecimal valor;
        private String socio; // Beneficiario
        private String descricao;
    }
}
