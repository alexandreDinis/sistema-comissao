package com.empresa.comissao.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "faturamentos")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Faturamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate dataFaturamento;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    @OneToOne
    @JoinColumn(name = "ordem_servico_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private OrdemServico ordemServico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler", "password", "features" })
    private User usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Empresa empresa;

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
