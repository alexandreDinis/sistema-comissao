package com.empresa.comissao.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class RelatorioReceitaCaixaDTO {
    private LocalDate dataInicio;
    private LocalDate dataFim;
    private BigDecimal totalRecebido;
    private List<ItemReceitaDTO> itens;

    @Data
    @Builder
    public static class ItemReceitaDTO {
        private LocalDate dataRecebimento;
        private BigDecimal valor;
        private String origem; // "OS #123" ou "Venda Direta"
        private String cliente;
        private String meioPagamento;
        private String descricao;
    }
}
