package com.empresa.comissao.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClienteRequest {
    private String razaoSocial;

    @NotBlank(message = "Nome Fantasia é obrigatório")
    private String nomeFantasia;

    private String cnpj;

    private String cpf;

    private com.empresa.comissao.domain.enums.TipoPessoa tipoPessoa;

    private String endereco;

    private String contato;

    private String email;

    private com.empresa.comissao.domain.enums.StatusCliente status;

    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    private String cidade;
    private String estado;
    private String cep;

    private String correlationId;
}
