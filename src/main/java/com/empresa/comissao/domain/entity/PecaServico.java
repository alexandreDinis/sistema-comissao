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
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private VeiculoServico veiculo;

    @Transient
    @lombok.Builder.Default
    @lombok.EqualsAndHashCode.Include
    private java.util.UUID tempId = java.util.UUID.randomUUID();

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

    @Column(name = "data_vencimento_prestador")
    private java.time.LocalDate dataVencimentoPrestador; // Data de vencimento do pagamento ao prestador

    // Offline Sync Idempotency
    @Column(name = "local_id", nullable = false)
    private String localId;

    @PrePersist
    public void prePersist() {
        if (this.localId == null) {
            this.localId = java.util.UUID.randomUUID().toString();
        }
        if (this.tempId == null) {
            this.tempId = java.util.UUID.randomUUID();
        }
    }

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
