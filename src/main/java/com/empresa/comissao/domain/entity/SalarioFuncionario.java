package com.empresa.comissao.domain.entity;

import com.empresa.comissao.domain.enums.TipoRemuneracao;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa a configuração de remuneração de um funcionário.
 * Permite ao admin definir se o funcionário recebe comissão, salário fixo ou
 * misto.
 */
@Entity
@Table(name = "salario_funcionario")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalarioFuncionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "password", "features" })
    private User usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_remuneracao", nullable = false, length = 20)
    @Builder.Default
    private TipoRemuneracao tipoRemuneracao = TipoRemuneracao.COMISSAO;

    /**
     * Valor do salário base mensal.
     * Usado quando tipoRemuneracao = SALARIO_FIXO ou MISTA.
     */
    @Column(name = "salario_base", precision = 19, scale = 2)
    private BigDecimal salarioBase;

    /**
     * Percentual de comissão adicional.
     * Usado quando tipoRemuneracao = MISTA.
     */
    @Column(name = "percentual_comissao", precision = 5, scale = 2)
    private BigDecimal percentualComissao;

    @Builder.Default
    private boolean ativo = true;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
        dataAtualizacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }

    /**
     * Verifica se esta configuração está ativa na data especificada.
     */
    public boolean isActiveOn(LocalDate date) {
        if (!ativo)
            return false;
        if (date.isBefore(dataInicio))
            return false;
        if (dataFim != null && date.isAfter(dataFim))
            return false;
        return true;
    }

    /**
     * Retorna o valor total a receber para um mês, dado o faturamento.
     */
    public BigDecimal calcularRemuneracao(BigDecimal faturamento, BigDecimal comissaoEmpresa) {
        return switch (tipoRemuneracao) {
            case SALARIO_FIXO -> salarioBase != null ? salarioBase : BigDecimal.ZERO;
            case MISTA -> {
                BigDecimal base = salarioBase != null ? salarioBase : BigDecimal.ZERO;
                BigDecimal comissao = BigDecimal.ZERO;
                if (percentualComissao != null && faturamento != null) {
                    comissao = faturamento.multiply(percentualComissao.divide(new BigDecimal("100")));
                }
                yield base.add(comissao);
            }
            case COMISSAO -> comissaoEmpresa != null ? comissaoEmpresa : BigDecimal.ZERO;
        };
    }
}
