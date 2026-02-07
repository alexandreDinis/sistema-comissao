package com.empresa.comissao.dto;

import com.empresa.comissao.domain.enums.CategoriaDespesa;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DespesaRequestDTO {

    @NotNull(message = "Data da despesa é obrigatória")
    private LocalDate dataDespesa;

    @NotNull(message = "Valor é obrigatório")
    @Positive(message = "Valor deve ser positivo")
    private BigDecimal valor;

    @NotNull(message = "Categoria é obrigatória")
    private CategoriaDespesa categoria;

    private String descricao;

    private boolean pagoAgora = false;

    private LocalDate dataVencimento;

    private com.empresa.comissao.domain.enums.MeioPagamento meioPagamento;

    private Long cartaoId; // ID do cartão de crédito (se aplicável)

    private Integer numeroParcelas; // Número de parcelas (null = à vista)
}
