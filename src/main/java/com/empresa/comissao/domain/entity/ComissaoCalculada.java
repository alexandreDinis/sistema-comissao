package com.empresa.comissao.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.empresa.comissao.config.YearMonthConverter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Entity
@Table(name = "comissoes_calculadas")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComissaoCalculada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = YearMonthConverter.class)
    @Column(name = "ano_mes_referencia", nullable = false, length = 7)
    private YearMonth anoMesReferencia;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "password", "features" })
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Empresa empresa;

    @Column(name = "faturamento_mensal_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal faturamentoMensalTotal;

    @Column(name = "faixa_comissao_descricao", nullable = false, length = 255)
    private String faixaComissaoDescricao;

    @Column(name = "porcentagem_comissao_aplicada", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentagemComissaoAplicada;

    @Column(name = "valor_bruto_comissao", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorBrutoComissao;

    @Column(name = "valor_total_adiantamentos", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotalAdiantamentos;

    @Column(name = "saldo_a_receber", nullable = false, precision = 19, scale = 2)
    private BigDecimal saldoAReceber;

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