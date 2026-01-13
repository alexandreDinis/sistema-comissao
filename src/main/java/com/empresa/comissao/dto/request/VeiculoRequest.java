package com.empresa.comissao.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VeiculoRequest {
    @NotNull(message = "ID da OS é obrigatório")
    private Long ordemServicoId;

    @NotNull(message = "Placa é obrigatório")
    private String placa;
    @NotNull(message = "Modelo é obrigatório")
    private String modelo;
    private String cor;
}
