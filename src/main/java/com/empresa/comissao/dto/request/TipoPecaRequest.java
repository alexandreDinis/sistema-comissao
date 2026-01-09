package com.empresa.comissao.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class TipoPecaRequest {
    @NotBlank(message = "Nome da peça é obrigatório")
    private String nome;

    @NotNull(message = "Valor padrão é obrigatório")
    private BigDecimal valorPadrao;
}
