package com.empresa.comissao.controller;

import com.empresa.comissao.dto.RelatorioFinanceiroDTO;
import com.empresa.comissao.service.ComissaoService;
import com.empresa.comissao.service.PdfService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RelatorioController.class)
public class RelatorioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComissaoService comissaoService;

    @MockBean
    private PdfService pdfService;

    @Test
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    public void gerarRelatorio_DeveRetornarRelatorio() throws Exception {
        RelatorioFinanceiroDTO dto = RelatorioFinanceiroDTO.builder()
                .ano(2024)
                .mes(1)
                .faturamentoTotal(BigDecimal.TEN)
                .build();

        when(comissaoService.gerarRelatorioFinanceiro(eq(2024), eq(1), any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/relatorios/2024/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ano").value(2024));
    }

    @Test
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    public void gerarPdf_DeveRetornarArquivoPdf() throws Exception {
        RelatorioFinanceiroDTO dto = RelatorioFinanceiroDTO.builder().build();
        byte[] fakePdf = new byte[] { 1, 2, 3 };

        when(comissaoService.gerarRelatorioFinanceiro(eq(2024), eq(1), any())).thenReturn(dto);
        when(pdfService.gerarRelatorioFinanceiroPdf(dto)).thenReturn(fakePdf);

        mockMvc.perform(get("/api/v1/relatorios/2024/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(org.springframework.http.MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(fakePdf));
    }
}
