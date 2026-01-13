package com.empresa.comissao.service;

import com.empresa.comissao.dto.RelatorioFinanceiroDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;

public class PdfServiceTest {

    @Test
    public void gerarPdf_DeveGerarBytes() {
        // Mock dependencies
        org.thymeleaf.TemplateEngine templateEngine = org.mockito.Mockito.mock(org.thymeleaf.TemplateEngine.class);
        PdfService service = new PdfService(templateEngine);

        RelatorioFinanceiroDTO dto = RelatorioFinanceiroDTO.builder()
                .ano(2024)
                .mes(1)
                .faturamentoTotal(BigDecimal.TEN)
                .adiantamentosTotal(BigDecimal.ZERO)
                .comissaoAlocada(BigDecimal.ONE)
                .saldoRemanescenteComissao(BigDecimal.ZERO)
                .despesasPorCategoria(new HashMap<>())
                .build();

        // Mock thymeleaf processing
        org.mockito.Mockito
                .when(templateEngine.process(org.mockito.ArgumentMatchers.eq("pdf/relatorio-financeiro"),
                        org.mockito.ArgumentMatchers.any(org.thymeleaf.context.Context.class)))
                .thenReturn("<html><body>Mock PDF Content</body></html>");

        // Pass null as Empresa since it's allowed/handled (or mock it if needed)
        // Since the method is protected against null empresa, null is fine for basic
        // test
        byte[] pdf = service.gerarRelatorioFinanceiroPdf(dto, null);

        Assertions.assertNotNull(pdf);
        Assertions.assertTrue(pdf.length > 0);
    }
}
