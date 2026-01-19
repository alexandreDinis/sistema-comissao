package com.empresa.comissao.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class PecaServicoResponse {
    private Long id;
    private String nomePeca; // Nome do TipoPeca
    private BigDecimal valorCobrado;
    private String descricao;
}
