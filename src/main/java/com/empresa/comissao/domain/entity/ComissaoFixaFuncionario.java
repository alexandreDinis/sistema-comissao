package com.empresa.comissao.domain.entity;

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
 * Representa uma porcentagem de comissão fixa atribuída a um funcionário
 * específico.
 * Usado quando a empresa deseja dar uma porcentagem diferente da regra geral
 * para alguém.
 */
@Entity
@Table(name = "comissao_fixa_funcionario")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComissaoFixaFuncionario {

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

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentagem;

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
}
