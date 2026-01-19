package com.empresa.comissao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO para relatório consolidado de Receita por Caixa.
 * Este relatório é a BASE PARA O DAS (Simples Nacional).
 * Agrupa todos os recebimentos efetivos do mês.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceitaCaixaReportDTO {
    private int ano;
    private int mes;
    private List<ReceitaCaixaDTO> recebimentos;
    private BigDecimal totalRecebido; // ← BASE DO DAS
    private int quantidadeRecebimentos;
}
