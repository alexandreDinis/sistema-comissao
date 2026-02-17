package com.empresa.comissao.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class VeiculoResponse {
    private Long id;
    private String localId;
    private String placa;
    private String modelo;
    private String cor;
    private BigDecimal valorTotal;
    private List<PecaServicoResponse> pecas;
}
