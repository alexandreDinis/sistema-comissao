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
import java.util.ArrayList;
import java.util.List;

/**
 * Conta a Receber (entrada de caixa).
 * Representa um direito de recebimento da empresa.
 *
 * IMPORTANTE: Comissão é calculada sobre Recebimento.valorPago
 * (registros na tabela recebimentos), não sobre ContaReceber.valor.
 *
 * Suporta pagamentos parciais:
 * - PENDENTE: nada recebido (saldoRestante == valor)
 * - PARCIAL: recebeu algo (0 < saldoRestante < valor)
 * - PAGO: totalmente quitado (saldoRestante == 0)
 * - BAIXADO: calote/perdão (saldoRestante zerado sem entrada de caixa)
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

    // Optimistic Locking: impede gravações concorrentes em saldoRestante
    @Version
    private Long version;

    @Column(length = 255)
    private String descricao;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    // Campos de controle de pagamento parcial
    @Column(name = "valor_pago_acumulado", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal valorPagoAcumulado = BigDecimal.ZERO;

    @Column(name = "saldo_restante", precision = 19, scale = 2)
    private BigDecimal saldoRestante;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    // DATAS IMPORTANTES
    @Column(name = "data_competencia", nullable = false)
    private LocalDate dataCompetencia; // Quando a receita PERTENCE contabilmente

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento; // Quando DEVE ser recebido

    @Column(name = "data_recebimento")
    private LocalDate dataRecebimento; // Quando FOI totalmente recebido (null = não quitado)

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

    // HISTÓRICO DE RECEBIMENTOS
    @OneToMany(mappedBy = "contaReceber", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnoreProperties({ "contaReceber" })
    @Builder.Default
    private List<Recebimento> recebimentos = new ArrayList<>();

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
        if (valorPagoAcumulado == null) {
            valorPagoAcumulado = BigDecimal.ZERO;
        }
        if (saldoRestante == null) {
            saldoRestante = valor;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }

    /**
     * Verifica se a conta está vencida.
     * VENCIDO é derivado, não persistido.
     * Aplica-se a contas PENDENTE e PARCIAL.
     */
    public boolean isVencido() {
        return (status == StatusConta.PENDENTE || status == StatusConta.PARCIAL)
                && dataVencimento != null
                && LocalDate.now().isAfter(dataVencimento);
    }

    /**
     * Registra um recebimento parcial ou total.
     * Atualiza valorPagoAcumulado, saldoRestante e status.
     *
     * @param valorRecebido valor efetivamente recebido
     * @throws IllegalArgumentException se valor > saldoRestante
     */
    public void registrarRecebimento(BigDecimal valorRecebido) {
        if (valorRecebido.compareTo(saldoRestante) > 0) {
            throw new IllegalArgumentException(
                    "Valor recebido (" + valorRecebido + ") excede o saldo restante (" + saldoRestante + ")");
        }

        this.valorPagoAcumulado = this.valorPagoAcumulado.add(valorRecebido);
        this.saldoRestante = this.saldoRestante.subtract(valorRecebido);

        if (this.saldoRestante.compareTo(BigDecimal.ZERO) == 0) {
            this.status = StatusConta.PAGO;
            this.dataRecebimento = LocalDate.now(); // Data de quitação total
        } else {
            this.status = StatusConta.PARCIAL;
        }
    }

    /**
     * Estorna um recebimento: devolve valor ao saldo.
     *
     * @param valorEstornado valor a devolver ao saldo
     */
    public void estornarRecebimento(BigDecimal valorEstornado) {
        this.valorPagoAcumulado = this.valorPagoAcumulado.subtract(valorEstornado);
        this.saldoRestante = this.saldoRestante.add(valorEstornado);
        this.dataRecebimento = null; // Não está mais quitado

        if (this.valorPagoAcumulado.compareTo(BigDecimal.ZERO) == 0) {
            this.status = StatusConta.PENDENTE;
        } else {
            this.status = StatusConta.PARCIAL;
        }
    }

    /**
     * Baixa o saldo restante (calote/perdão).
     * NÃO gera Recebimento, portanto NÃO entra no cálculo de comissão.
     */
    public void baixarSaldo(String motivo) {
        this.saldoRestante = BigDecimal.ZERO;
        this.status = StatusConta.BAIXADO;
        this.observacao = motivo;
    }

    /**
     * Marca a conta como recebida (backward compatibility).
     * 
     * @deprecated Use registrarRecebimento() para suporte a pagamentos parciais.
     */
    @Deprecated
    public void marcarComoRecebido(LocalDate dataRecebimento, MeioPagamento meioPagamento) {
        this.dataRecebimento = dataRecebimento;
        this.meioPagamento = meioPagamento;
        this.status = StatusConta.PAGO;
        this.valorPagoAcumulado = this.valor;
        this.saldoRestante = BigDecimal.ZERO;
    }

    /**
     * Cancela a conta.
     */
    public void cancelar() {
        this.status = StatusConta.CANCELADO;
    }
}
