package com.empresa.comissao.domain.entity;

import com.empresa.comissao.domain.enums.StatusLicenca;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "licencas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Licenca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Dados da Empresa Revendedora
    @Column(name = "razao_social", nullable = false)
    private String razaoSocial;

    @Column(name = "nome_fantasia")
    private String nomeFantasia;

    @Column(unique = true, nullable = false, length = 18)
    private String cnpj;

    @Column(nullable = false)
    private String email;

    @Column(length = 20)
    private String telefone;

    // Endereço
    private String logradouro;
    private String numero;
    private String bairro;
    private String cidade;
    @Column(length = 2)
    private String estado;
    @Column(length = 10)
    private String cep;

    // Plano Contratado (Snapshot do momento da contratação)
    @Column(name = "plano_tipo", nullable = false)
    private String planoTipo; // BASIC, PRO, ENTERPRISE

    @Column(name = "valor_mensalidade", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorMensalidade;

    @Column(name = "valor_por_tenant", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorPorTenant;

    @Column(name = "limite_tenants")
    private Integer limiteTenants;

    // White Label Config
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "cor_primaria", length = 7)
    private String corPrimaria;

    @Column(name = "cor_secundaria", length = 7)
    private String corSecundaria;

    @Column(name = "dominio_customizado")
    private String dominioCustomizado;

    // Dados Bancários (para receber dos tenants)
    private String banco;
    @Column(name = "tipo_conta")
    private String tipoConta;
    private String agencia;
    private String conta;
    @Column(name = "pix_tipo")
    private String pixTipo;
    @Column(name = "pix_chave")
    private String pixChave;

    // Gateway de Pagamento (Revendedor cobra seus tenants)
    @Column(name = "gateway_pagamento")
    private String gatewayPagamento; // MERCADO_PAGO, ASAAS, MANUAL

    @Column(name = "gateway_access_token", columnDefinition = "TEXT")
    private String gatewayAccessToken;

    @Column(name = "gateway_public_key", columnDefinition = "TEXT")
    private String gatewayPublicKey;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusLicenca status = StatusLicenca.ATIVA;

    @Column(name = "data_ativacao")
    private LocalDate dataAtivacao;

    @Column(name = "data_suspensao")
    private LocalDate dataSuspensao;

    @Column(name = "motivo_suspensao", columnDefinition = "TEXT")
    private String motivoSuspensao;

    // Auditoria
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (dataAtivacao == null) {
            dataAtivacao = LocalDate.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
