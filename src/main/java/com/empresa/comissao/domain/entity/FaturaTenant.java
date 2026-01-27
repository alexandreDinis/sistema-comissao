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
@Table(name = "faturas_tenant")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FaturaTenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licenca_id", nullable = false)
    private Licenca licenca;

    @Column(name = "mes_referencia", nullable = false, length = 7)
    private String mesReferencia;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "data_emissao", nullable = false)
    private LocalDate dataEmissao;

    @Column(name = "data_vencimento", nullable = false)
    private LocalDate dataVencimento;

    @Column(name = "data_pagamento")
    private LocalDate dataPagamento;

    @Column(name = "valor_pago", precision = 10, scale = 2)
    private BigDecimal valorPago;

    // Dados do Gateway do Revendedor
    @Column(name = "gateway_pagamento", length = 50)
    private String gatewayPagamento;

    @Column(name = "payment_id")
    private String paymentId;

    @Column(name = "preference_id")
    private String preferenceId;

    @Column(name = "qr_code_pix", columnDefinition = "TEXT")
    private String qrCodePix;

    @Column(name = "qr_code_image_url", length = 500)
    private String qrCodeImageUrl;

    @Column(name = "url_pagamento", length = 500)
    private String urlPagamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusFatura status = StatusFatura.PENDENTE;

    @Column(name = "tentativas_cobranca")
    private Integer tentativasCobranca;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (tentativasCobranca == null) {
            tentativasCobranca = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
