package com.empresa.comissao.dto;

import com.empresa.comissao.domain.enums.CategoriaDespesa;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelatorioFinanceiroDTO {
    private int ano;
    private int mes;
    private Map<CategoriaDespesa, BigDecimal> despesasPorCategoria;
    private BigDecimal faturamentoTotal;
    private BigDecimal despesasTotal;
    private BigDecimal imposto;
    private BigDecimal adiantamentosTotal;
    private BigDecimal comissaoAlocada;
    private BigDecimal saldoRemanescenteComissao;
    private BigDecimal totalGeral;
    private BigDecimal lucroLiquido;
}
