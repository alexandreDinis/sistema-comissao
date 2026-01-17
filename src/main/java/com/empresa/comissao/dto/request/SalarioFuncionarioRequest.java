package com.empresa.comissao.dto.request;

import com.empresa.comissao.domain.enums.TipoRemuneracao;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO para criação/atualização de configuração de salário/remuneração de
 * funcionário.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalarioFuncionarioRequest {

    @NotNull(message = "ID do funcionário é obrigatório")
    private Long usuarioId;

    @NotNull(message = "Tipo de remuneração é obrigatório")
    private TipoRemuneracao tipoRemuneracao;

    /**
     * Valor do salário base mensal (obrigatório para SALARIO_FIXO e MISTA).
     */
    @PositiveOrZero(message = "Salário base deve ser positivo ou zero")
    private BigDecimal salarioBase;

    /**
     * Percentual de comissão adicional (usado para MISTA).
     */
    @PositiveOrZero(message = "Percentual de comissão deve ser positivo ou zero")
    private BigDecimal percentualComissao;

    @NotNull(message = "Data de início é obrigatória")
    private LocalDate dataInicio;

    private LocalDate dataFim;
}
