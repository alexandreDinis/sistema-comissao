package com.empresa.comissao.domain.entity;

import com.empresa.comissao.domain.enums.MeioPagamento;
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
 * Registro individual de recebimento (entrada de caixa).
 * Cada ContaReceber pode ter múltiplos recebimentos (pagamento parcial).
 *
 * IMPORTANTE: Este é o registro que alimenta o cálculo de comissão.
 * Valor BAIXADO (calote) NÃO gera Recebimento, portanto não entra na comissão.
 */
@Entity
@Table(name = "recebimentos", indexes = {
        @Index(name = "idx_recebimentos_empresa_data", columnList = "empresa_id, data_pagamento"),
        @Index(name = "idx_recebimentos_conta", columnList = "conta_receber_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Recebimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conta_receber_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "recebimentos" })
    private ContaReceber contaReceber;

    @Column(name = "valor_pago", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorPago;

    @Column(name = "data_pagamento", nullable = false)
    private LocalDate dataPagamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "meio_pagamento", length = 30)
    private MeioPagamento meioPagamento;

    @Column(columnDefinition = "TEXT")
    private String observacao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "password", "features" })
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Empresa empresa;

    // Referência ao funcionário responsável (para comissão individual)
    // Copiado da ContaReceber no momento do recebimento para performance
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "funcionario_responsavel_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "password", "features" })
    private User funcionarioResponsavel;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
    }
}
