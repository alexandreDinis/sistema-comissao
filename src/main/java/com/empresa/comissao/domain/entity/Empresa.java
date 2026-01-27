package com.empresa.comissao.domain.entity;

import com.empresa.comissao.domain.enums.ModoComissao;
import com.empresa.comissao.domain.enums.Plano;
import com.empresa.comissao.domain.enums.RegimeTributario;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "empresas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(name = "razao_social")
    private String razaoSocial;

    private String cnpj;

    @Column(name = "logo_path")
    private String logoPath;

    private String endereco;
    private String telefone;
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Plano plano = Plano.BRONZE;

    @Enumerated(EnumType.STRING)
    @Column(name = "modo_comissao", nullable = false)
    @Builder.Default
    private ModoComissao modoComissao = ModoComissao.INDIVIDUAL;

    // ========================================
    // CONFIGURAÇÃO TRIBUTÁRIA
    // ========================================

    @Column(name = "aliquota_imposto", precision = 5, scale = 4)
    @Builder.Default
    private java.math.BigDecimal aliquotaImposto = new java.math.BigDecimal("0.0600"); // 6% default

    @Enumerated(EnumType.STRING)
    @Column(name = "regime_tributario")
    @Builder.Default
    private RegimeTributario regimeTributario = RegimeTributario.SIMPLES_NACIONAL;

    @Column(length = 2)
    private String uf; // Estado da empresa

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "licenca_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Licenca licenca; // Current reseller (NULL = direct owner management)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "licenca_original_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Licenca licencaOriginal; // History: who originally brought this tenant

    @Column(name = "valor_mensal_pago", precision = 10, scale = 2)
    private java.math.BigDecimal valorMensalPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private com.empresa.comissao.domain.enums.StatusEmpresa status = com.empresa.comissao.domain.enums.StatusEmpresa.ATIVA;

    @Builder.Default
    private boolean ativo = true; // Mantido por compatibilidade, mas usar status preferencialmente

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
}
