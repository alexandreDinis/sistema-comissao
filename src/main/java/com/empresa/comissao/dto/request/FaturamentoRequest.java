package com.empresa.comissao.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FaturamentoRequest {

    @NotBlank(message = "O cliente é obrigatório")
    private String cliente;

    @NotNull(message = "O valor é obrigatório")
    @Positive(message = "O valor deve ser positivo")
    private BigDecimal valor;

    @NotNull(message = "A data de faturamento é obrigatória")
    private LocalDate dataFaturamento;

    private String numeroNotaFiscal;
}
