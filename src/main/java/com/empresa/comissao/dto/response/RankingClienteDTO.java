package com.empresa.comissao.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankingClienteDTO {
    private Long clienteId;
    private String nomeFantasia;
    private Long quantidadeOS;
    private BigDecimal valorTotal;
}
