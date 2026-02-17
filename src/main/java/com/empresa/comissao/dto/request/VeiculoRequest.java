package com.empresa.comissao.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VeiculoRequest {
    // @NotNull(message = "ID da OS é obrigatório") -- Removido para suportar
    // localId
    private Long ordemServicoId;

    // Offline Sync
    private String ordemServicoLocalId;
    private String localId; // Para upsert do veículo

    public void validar() {
        if (ordemServicoId == null && ordemServicoLocalId == null) {
            throw new IllegalArgumentException("ID ou LocalID da OS é obrigatório");
        }
    }

    @NotNull(message = "Placa é obrigatório")
    private String placa;
    @NotNull(message = "Modelo é obrigatório")
    private String modelo;
    private String cor;
}
