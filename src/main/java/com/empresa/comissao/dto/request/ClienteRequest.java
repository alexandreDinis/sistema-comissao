package com.empresa.comissao.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClienteRequest {
    @NotBlank(message = "Razão Social é obrigatória")
    private String razaoSocial;
    private String nomeFantasia;
    private String cnpj;
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
}
