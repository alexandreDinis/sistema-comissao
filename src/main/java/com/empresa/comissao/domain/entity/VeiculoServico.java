package com.empresa.comissao.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;

@Entity
@Table(name = "veiculos_servico", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "ordem_servico_id", "placa" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VeiculoServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 20, nullable = false)
    private String placa;
    private String modelo;
    private String cor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ordem_servico_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private OrdemServico ordemServico;

    // Persisted total for performance
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @OneToMany(mappedBy = "veiculo", cascade = CascadeType.ALL, orphanRemoval = true)
    @jakarta.persistence.OrderBy("id ASC")
    @Builder.Default
    @lombok.ToString.Exclude
    @lombok.EqualsAndHashCode.Exclude
    private java.util.Set<PecaServico> pecas = new java.util.LinkedHashSet<>();

    @Column(name = "local_id", nullable = false, unique = true)
    private String localId;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.localId == null) {
            this.localId = java.util.UUID.randomUUID().toString();
        }
    }

    public void recalcularTotal() {
        this.valorTotal = pecas.stream()
                .map(PecaServico::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Propagate update to OS
        if (ordemServico != null) {
            ordemServico.recalcularTotal();
        }
    }
}
