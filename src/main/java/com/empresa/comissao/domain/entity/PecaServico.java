package com.empresa.comissao.domain.entity;

import com.empresa.comissao.domain.enums.TipoExecucao;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;

@Entity
@Table(name = "pecas_servico")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PecaServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tipo_peca_id")
    private TipoPeca tipoPeca;

    @Column(nullable = false)
    private BigDecimal valor; // Valor cobrado do cliente

    @Column(length = 500)
    private String descricao;

    @ManyToOne(optional = false)
    @JoinColumn(name = "veiculo_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private VeiculoServico veiculo;

    // ========================================
    // SERVIÇO TERCEIRIZADO
    // ========================================

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_execucao")
    @Builder.Default
    private TipoExecucao tipoExecucao = TipoExecucao.INTERNO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prestador_id")
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Prestador prestador; // NULL = serviço interno

    @Column(name = "custo_prestador", precision = 19, scale = 2)
    private BigDecimal custoPrestador; // Quanto pagar ao prestador

    /**
     * Verifica se é serviço terceirizado.
     */
    public boolean isTerceirizado() {
        return tipoExecucao == TipoExecucao.TERCEIRIZADO && prestador != null;
    }

    /**
     * Calcula margem de lucro do serviço.
     */
    public BigDecimal getMargem() {
        if (!isTerceirizado() || custoPrestador == null) {
            return valor; // Serviço interno: margem = valor total
        }
        return valor.subtract(custoPrestador);
    }
}
