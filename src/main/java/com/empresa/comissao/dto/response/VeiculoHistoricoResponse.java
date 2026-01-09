package com.empresa.comissao.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class VeiculoHistoricoResponse {
    private Long ordemServicoId;
    private LocalDate data;
    private String status;
    private BigDecimal valorTotalServico;
    private List<String> pecasOuServicos;
}
