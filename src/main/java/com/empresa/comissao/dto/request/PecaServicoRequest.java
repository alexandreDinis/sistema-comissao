package com.empresa.comissao.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class PecaServicoRequest {
    @NotNull(message = "ID do Veículo é obrigatório")
    private Long veiculoId;

    @NotNull(message = "ID do Tipo de Peça é obrigatório")
    private Long tipoPecaId;

    // Opcional, se nulo usa o valorPadrao do TipoPeca
    private BigDecimal valorCobrado;
}
