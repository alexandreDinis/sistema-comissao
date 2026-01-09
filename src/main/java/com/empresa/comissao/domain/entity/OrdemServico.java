package com.empresa.comissao.domain.entity;

import com.empresa.comissao.domain.enums.StatusOrdemServico;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.math.BigDecimal;
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusOrdemServico status = StatusOrdemServico.ABERTA;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id")
    private
    Cliente cliente;

    // Persisted total for performance
    @Column(nullable = false)
    @Builder.Default
    private BigDecimal valorTotal = BigDecimal.ZERO;

    @OneToMany(mappedBy = "ordemServico", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<VeiculoServico> veiculos = new ArrayList<>();

    public void recalcularTotal() {
        this.valorTotal = veiculos.stream()
                .map(VeiculoServico::getValorTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
