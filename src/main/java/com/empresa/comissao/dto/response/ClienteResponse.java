package com.empresa.comissao.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClienteResponse {
    private Long id;
    private String razaoSocial;
    private String nomeFantasia;
    private String cnpj;
    private String cpf;
    private com.empresa.comissao.domain.enums.TipoPessoa tipoPessoa;
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
