package com.empresa.comissao.dto.list;

import com.empresa.comissao.domain.enums.StatusConta;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContaResumoDTO {
    private Long id;
    private String descricao;
    private BigDecimal valor;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate dataVencimento;

    private StatusConta status;
    private String pessoaNome; // Name of supplier (pagar) or customer (receber)
}
