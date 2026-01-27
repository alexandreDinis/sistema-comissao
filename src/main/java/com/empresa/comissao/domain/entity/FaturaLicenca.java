package com.empresa.comissao.domain.entity;

import com.empresa.comissao.domain.enums.StatusFatura;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "faturas_licenca")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaturaLicenca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licenca_id", nullable = false)
    private Licenca licenca;

    // Composição da Fatura
    @Column(name = "mes_referencia", nullable = false, length = 7)
    private String mesReferencia; // '2026-01'

    @Column(name = "valor_mensalidade", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorMensalidade;

    @Column(name = "quantidade_tenants", nullable = false)
    private Integer quantidadeTenants;

    @Column(name = "valor_por_tenant", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorPorTenant;

    @Column(name = "valor_tenants", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTenants;

    @Column(name = "valor_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorTotal;

    // Pagamento
    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Column(name = "valor_pago", precision = 10, scale = 2)
    private BigDecimal valorPago;

    @Column(name = "forma_pagamento", length = 50)
    private String formaPagamento; // PIX, BOLETO, TED

    @Column(name = "comprovante_url", length = 500)
    private String comprovanteUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusFatura status = StatusFatura.PENDENTE;

    @Column(columnDefinition = "TEXT")
    private String observacoes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
