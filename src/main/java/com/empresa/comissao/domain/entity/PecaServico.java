package com.empresa.comissao.domain.entity;

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
    private BigDecimal valor;

    @Column(length = 500)
    private String descricao;

    @ManyToOne(optional = false)
    @JoinColumn(name = "veiculo_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private VeiculoServico veiculo;
}
