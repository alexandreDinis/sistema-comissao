package com.empresa.comissao.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para criação/atualização de comissão fixa por funcionário.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComissaoFixaFuncionarioRequest {

    @NotNull(message = "ID do funcionário é obrigatório")
    private Long usuarioId;

    @NotNull(message = "Porcentagem é obrigatória")
    @Positive(message = "Porcentagem deve ser positiva")
    private BigDecimal porcentagem;

    @NotNull(message = "Data de início é obrigatória")
    private LocalDate dataInicio;

    private LocalDate dataFim;
}
