package com.empresa.comissao.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
public class ComissaoResponse {
    private Long id;
    private String cliente;
    private BigDecimal valorFaturamento;
    private BigDecimal valorComissao;
    private LocalDate dataCalculo;
    private BigDecimal percentualAplicado;
}
