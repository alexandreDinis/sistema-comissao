package com.empresa.comissao.service;

import com.empresa.comissao.dto.RelatorioFinanceiroDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;

public class PdfServiceTest {

    @Test
    public void gerarPdf_DeveGerarBytes() {
        PdfService service = new PdfService();
        RelatorioFinanceiroDTO dto = RelatorioFinanceiroDTO.builder()
                .ano(2024)
                .mes(1)
                .faturamentoTotal(BigDecimal.TEN)
                .adiantamentosTotal(BigDecimal.ZERO)
                .comissaoAlocada(BigDecimal.ONE)
                .saldoRemanescenteComissao(BigDecimal.ZERO)
                .despesasPorCategoria(new HashMap<>())
                .build();

        byte[] pdf = service.gerarRelatorioFinanceiroPdf(dto);
        Assertions.assertNotNull(pdf);
        Assertions.assertTrue(pdf.length > 0);
    }
}
