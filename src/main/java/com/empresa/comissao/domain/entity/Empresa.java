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

    @Builder.Default
    private boolean ativo = true;

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
