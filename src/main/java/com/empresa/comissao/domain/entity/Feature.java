package com.empresa.comissao.domain.entity;

import com.empresa.comissao.domain.enums.Plano;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "features")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Feature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "plano_minimo")
    private Plano planoMinimo;
}
