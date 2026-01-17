package com.empresa.comissao.dto.request;

import com.empresa.comissao.domain.enums.TipoRegraComissao;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DTO para criação/atualização de regras de comissão.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegraComissaoRequest {

    @NotBlank(message = "Nome da regra é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    private String nome;

    @NotNull(message = "Tipo de regra é obrigatório")
    private TipoRegraComissao tipoRegra;

    @Size(max = 500, message = "Descrição deve ter no máximo 500 caracteres")
    private String descricao;

    /**
     * Percentual fixo (usado quando tipoRegra = FIXA_EMPRESA).
     */
    private BigDecimal percentualFixo;

    @NotNull(message = "Data de início é obrigatória")
    private LocalDate dataInicio;

    private LocalDate dataFim;

    /**
     * Lista de faixas (usado quando tipoRegra = FAIXA_FATURAMENTO ou HIBRIDA).
     */
    @Valid
    private List<FaixaComissaoRequest> faixas;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FaixaComissaoRequest {

        @NotNull(message = "Faturamento mínimo é obrigatório")
        private BigDecimal minFaturamento;

        private BigDecimal maxFaturamento;

        @NotNull(message = "Porcentagem é obrigatória")
        private BigDecimal porcentagem;

        @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
        private String descricao;

        private Integer ordem;
    }
}
