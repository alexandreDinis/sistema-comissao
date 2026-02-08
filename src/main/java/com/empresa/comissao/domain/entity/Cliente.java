package com.empresa.comissao.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Entity
@Table(name = "clientes", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "cnpj", "empresa_id" }),
        @UniqueConstraint(columnNames = { "cpf", "empresa_id" }),
        @UniqueConstraint(columnNames = { "empresa_id", "local_id" })
})
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

    private String cnpj;

    @Column(length = 14)
    private String cpf;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pessoa", length = 20)
    @Builder.Default
    private com.empresa.comissao.domain.enums.TipoPessoa tipoPessoa = com.empresa.comissao.domain.enums.TipoPessoa.JURIDICA;

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

    @Column(name = "local_id", nullable = false)
    private String localId;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (this.localId == null) {
            this.localId = java.util.UUID.randomUUID().toString();
        }
        this.updatedAt = java.time.LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = java.time.LocalDateTime.now();
    }
}
