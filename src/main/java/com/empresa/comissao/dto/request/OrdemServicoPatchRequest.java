package com.empresa.comissao.dto.request;

import com.empresa.comissao.domain.enums.TipoDesconto;
import jakarta.validation.constraints.DecimalMin;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class OrdemServicoPatchRequest {

    private LocalDate data;

    // Optional discount fields
    private TipoDesconto tipoDesconto;

    @DecimalMin(value = "0.0", message = "Valor do desconto deve ser positivo")
    private BigDecimal valorDesconto;

    // Payment due date
    private LocalDate dataVencimento;
}
