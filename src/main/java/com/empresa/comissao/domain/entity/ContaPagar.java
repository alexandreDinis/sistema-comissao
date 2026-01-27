package com.empresa.comissao.domain.entity;

import com.empresa.comissao.domain.enums.MeioPagamento;
import com.empresa.comissao.domain.enums.StatusConta;
import com.empresa.comissao.domain.enums.TipoContaPagar;
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
 * Conta a Pagar (saída de caixa).
 * Representa um compromisso financeiro da empresa.
 */
@Entity
@Table(name = "contas_pagar")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContaPagar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 255)
    private String descricao;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    // DATAS IMPORTANTES
    @Column(name = "data_competencia", nullable = false)
    private LocalDate dataCompetencia; // Quando o gasto PERTENCE contabilmente

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento; // Quando DEVE ser pago

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento; // Quando FOI pago (null = pendente)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusConta status = StatusConta.PENDENTE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoContaPagar tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "meio_pagamento", length = 30)
    private MeioPagamento meioPagamento;

    // PARCELAMENTO
    @Column(name = "numero_parcela")
    private Integer numeroParcela;

    @Column(name = "total_parcelas")
    private Integer totalParcelas;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parcela_origem_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private ContaPagar parcelaOrigem;

    // REFERÊNCIAS (para rastrear origem)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "despesa_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Despesa despesa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comissao_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private ComissaoCalculada comissao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "password", "features" })
    private User funcionario;

    // CARTÃO DE CRÉDITO (para faturas)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cartao_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private CartaoCredito cartao;

    @Column(name = "mes_referencia", length = 7)
    private String mesReferencia; // "2026-01"

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
     * Marca a conta como paga.
     */
    public void marcarComoPago(LocalDate dataPagamento, MeioPagamento meioPagamento) {
        this.dataPagamento = dataPagamento;
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
