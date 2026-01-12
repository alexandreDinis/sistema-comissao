package com.empresa.comissao.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ClienteRequest {
    @NotBlank(message = "Razão Social é obrigatória")
    private String razaoSocial;

    private String nomeFantasia;

    @NotBlank(message = "CNPJ é obrigatório")
    private String cnpj;

    @NotBlank(message = "Endereço é obrigatório")
    private String endereco;

    @NotBlank(message = "Contato é obrigatório")
    private String contato;

    @NotBlank(message = "Email é obrigatório")
    @jakarta.validation.constraints.Email(message = "Email inválido")
    private String email;

    private com.empresa.comissao.domain.enums.StatusCliente status;

    private String logradouro;
    private String numero;
    private String complemento;
    private String bairro;
    @NotBlank(message = "Cidade é obrigatória")
    private String cidade;
    @NotBlank(message = "Estado é obrigatório")
    private String estado;
    private String cep;
}
