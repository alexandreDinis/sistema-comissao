package com.empresa.comissao.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VeiculoRequest {
    @NotNull(message = "ID da OS é obrigatório")
    private Long ordemServicoId;

    private String placa;
    private String modelo;
    private String cor;
}
