package com.empresa.comissao.domain.entity;

import com.empresa.comissao.domain.enums.StatusOrdemServico;
import com.empresa.comissao.domain.enums.TipoDesconto;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ordens_servico")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdemServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate data;

    @Column(name = "data_vencimento")
    private LocalDate dataVencimento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusOrdemServico status = StatusOrdemServico.ABERTA;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "password", "features" })
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Empresa empresa;

    // Discount fields
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_desconto")
    private TipoDesconto tipoDesconto;

    @Column(name = "valor_desconto", precision = 19, scale = 2)
    private BigDecimal valorDesconto;

    // Persisted totals for performance
    @Column(name = "valor_total_sem_desconto", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal valorTotalSemDesconto = BigDecimal.ZERO;

    @Column(name = "valor_total_com_desconto", nullable = false, precision = 19, scale = 2)
    @Builder.Default
    private BigDecimal valorTotalComDesconto = BigDecimal.ZERO;

    // Legacy field - kept for backward compatibility
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    // Offline Sync Idempotency
    @Column(name = "local_id", nullable = false, unique = true)
    private String localId;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VeiculoServico> veiculos = new ArrayList<>();

    public void recalcularTotal() {
        // Calculate total without discount
        this.valorTotalSemDesconto = veiculos.stream()
                .map(VeiculoServico::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Apply discount if configured
        BigDecimal descontoAplicado = BigDecimal.ZERO;
        if (tipoDesconto != null && valorDesconto != null && valorDesconto.compareTo(BigDecimal.ZERO) > 0) {
            if (tipoDesconto == TipoDesconto.PERCENTUAL) {
                // Calculate percentage discount
                descontoAplicado = valorTotalSemDesconto
                        .multiply(valorDesconto)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            } else if (tipoDesconto == TipoDesconto.VALOR_FIXO) {
                // Fixed value discount
                descontoAplicado = valorDesconto;
            }
        }

        // Calculate final total with discount
        this.valorTotalComDesconto = valorTotalSemDesconto.subtract(descontoAplicado)
                .max(BigDecimal.ZERO) // Ensure non-negative
                .setScale(2, RoundingMode.HALF_UP);

        // Update legacy field for backward compatibility
        this.valorTotal = this.valorTotalComDesconto;
    }
}
