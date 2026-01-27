package com.empresa.comissao.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "planos_licenca")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanoLicenca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome; // BASIC, PRO, ENTERPRISE

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "valor_mensalidade", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorMensalidade;

    @Column(name = "valor_por_tenant", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorPorTenant;

    @Column(name = "limite_tenants")
    private Integer limiteTenants; // NULL = ilimitado

    @Column(name = "limite_usuarios_por_tenant")
    private Integer limiteUsuariosPorTenant;

    @Column(name = "suporte_prioritario")
    @Builder.Default
    private boolean suportePrioritario = false;

    @Column(name = "white_label")
    @Builder.Default
    private boolean whiteLabel = true;

    @Column(name = "dominio_customizado")
    @Builder.Default
    private boolean dominioCustomizado = false;

    @Builder.Default
    private boolean ativo = true;

    private Integer ordem;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
