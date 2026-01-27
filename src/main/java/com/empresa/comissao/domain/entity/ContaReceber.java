package com.empresa.comissao.domain.entity;

import com.empresa.comissao.domain.enums.MeioPagamento;
import com.empresa.comissao.domain.enums.StatusConta;
import com.empresa.comissao.domain.enums.TipoContaReceber;
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
 * Conta a Receber (entrada de caixa).
 * Representa um direito de recebimento da empresa.
 * 
 * IMPORTANTE: Comissão é calculada sobre ContaReceber.PAGO,
 * não sobre Faturamento (competência).
 */
@Entity
@Table(name = "contas_receber")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContaReceber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String descricao;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    // DATAS IMPORTANTES
    @Column(name = "data_competencia", nullable = false)
    private LocalDate dataCompetencia; // Quando a receita PERTENCE contabilmente

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento; // Quando DEVE ser recebido

    @Column(name = "data_recebimento")
    private LocalDate dataRecebimento; // Quando FOI recebido (null = pendente)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusConta status = StatusConta.PENDENTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoContaReceber tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "meio_pagamento", length = 30)
    private MeioPagamento meioPagamento;

    // REFERÊNCIAS
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "faturamento_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Faturamento faturamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_servico_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private OrdemServico ordemServico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_responsavel_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "password", "features" })
    private User funcionarioResponsavel; // Para calcular comissão individual

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Empresa empresa;

    // AUDITORIA
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
        dataAtualizacao = LocalDateTime.now();
        if (status == null) {
            status = StatusConta.PENDENTE;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }

    /**
     * Verifica se a conta está vencida.
     * VENCIDO é derivado, não persistido.
     */
    public boolean isVencido() {
        return status == StatusConta.PENDENTE
                && dataVencimento != null
                && LocalDate.now().isAfter(dataVencimento);
    }

    /**
     * Marca a conta como recebida.
     */
    public void marcarComoRecebido(LocalDate dataRecebimento, MeioPagamento meioPagamento) {
        this.dataRecebimento = dataRecebimento;
        this.meioPagamento = meioPagamento;
        this.status = StatusConta.PAGO;
    }

    /**
     * Cancela a conta.
     */
    public void cancelar() {
        this.status = StatusConta.CANCELADO;
    }
}
