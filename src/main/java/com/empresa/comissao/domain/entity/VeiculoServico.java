package com.empresa.comissao.domain.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "veiculos_servico")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VeiculoServico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String placa;
    private String modelo;
    private String cor;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ordem_servico_id")
    private OrdemServico ordemServico;

    // Persisted total for performance
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @OneToMany(mappedBy = "veiculo", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PecaServico> pecas = new ArrayList<>();

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
