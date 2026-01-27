package com.empresa.comissao.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Representa uma faixa de comissão dentro de uma regra.
 * Define o intervalo de faturamento e a porcentagem aplicável.
 */
@Entity
@Table(name = "faixas_comissao_config")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaixaComissaoConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regra_id", nullable = false)
    @JsonIgnoreProperties({ "faixas", "hibernateLazyInitializer", "handler" })
    private RegraComissao regra;

    @Column(name = "min_faturamento", nullable = false, precision = 19, scale = 2)
    private BigDecimal minFaturamento;

    @Column(name = "max_faturamento", precision = 19, scale = 2)
    private BigDecimal maxFaturamento; // NULL = sem limite superior

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentagem;

    @Column(length = 255)
    private String descricao;

    @Column(nullable = false)
    @Builder.Default
    private int ordem = 0;

    /**
     * Verifica se um valor de faturamento está dentro desta faixa.
     */
    public boolean isInRange(BigDecimal faturamento) {
        boolean greaterThanOrEqualMin = faturamento.compareTo(minFaturamento) >= 0;
        boolean lessThanOrEqualMax = (maxFaturamento == null || faturamento.compareTo(maxFaturamento) <= 0);
        return greaterThanOrEqualMin && lessThanOrEqualMax;
    }
}
