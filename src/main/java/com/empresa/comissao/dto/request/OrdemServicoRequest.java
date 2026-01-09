package com.empresa.comissao.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class OrdemServicoRequest {
    @NotNull(message = "ID do Cliente é obrigatório")
    private Long clienteId;

    @NotNull(message = "Data é obrigatória")
    private LocalDate data;
}
