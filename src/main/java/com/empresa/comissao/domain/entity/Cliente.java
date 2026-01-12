package com.empresa.comissao.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "clientes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String razaoSocial;

    private String nomeFantasia;

    @Column(unique = true)
    private String cnpj;

    private String endereco;
    private String contato;
    private String email;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private com.empresa.comissao.domain.enums.StatusCliente status = com.empresa.comissao.domain.enums.StatusCliente.ATIVO;

    // Endereço Completo
    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String estado;
    private String cep;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Empresa empresa;
}
