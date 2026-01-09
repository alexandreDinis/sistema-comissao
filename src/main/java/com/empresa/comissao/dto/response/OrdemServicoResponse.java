package com.empresa.comissao.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class OrdemServicoResponse {
    private Long id;
    private LocalDate data;
    private com.empresa.comissao.domain.enums.StatusOrdemServico status;
    private ClienteResponse cliente;
    private BigDecimal valorTotal;
    private List<VeiculoResponse> veiculos;
}
