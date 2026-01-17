package com.empresa.comissao.domain.entity;

import com.empresa.comissao.domain.enums.TipoRegraComissao;
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
 * Representa uma regra de comissionamento configurada por uma empresa.
 * Cada empresa pode ter múltiplas regras, mas apenas uma ativa por vez.
 */
@Entity
@Table(name = "regras_comissao")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegraComissao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Empresa empresa;

    @Column(nullable = false, length = 100)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_regra", nullable = false, length = 50)
    private TipoRegraComissao tipoRegra;

    @Builder.Default
    private boolean ativo = true;

    @Column(length = 500)
    private String descricao;

    /**
     * Percentual fixo utilizado quando tipoRegra = FIXA_EMPRESA
     */
    @Column(name = "percentual_fixo", precision = 5, scale = 2)
    private BigDecimal percentualFixo;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;

    @OneToMany(mappedBy = "regra", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnoreProperties({ "regra" })
    private List<FaixaComissaoConfig> faixas = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
        dataAtualizacao = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }

    /**
     * Adiciona uma faixa de comissão a esta regra.
     */
    public void addFaixa(FaixaComissaoConfig faixa) {
        faixas.add(faixa);
        faixa.setRegra(this);
    }

    /**
     * Remove uma faixa de comissão desta regra.
     */
    public void removeFaixa(FaixaComissaoConfig faixa) {
        faixas.remove(faixa);
        faixa.setRegra(null);
    }
}
