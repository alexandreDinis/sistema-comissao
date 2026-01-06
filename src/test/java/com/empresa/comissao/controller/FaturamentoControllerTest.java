package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.Faturamento;
import com.empresa.comissao.service.ComissaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FaturamentoController.class)
class FaturamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComissaoService comissaoService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void deveRegistrarFaturamentoComSucesso() throws Exception {
        FaturamentoController.FaturamentoRequest request = new FaturamentoController.FaturamentoRequest();
        request.setDataFaturamento(LocalDate.of(2026, 1, 15));
        request.setValor(BigDecimal.valueOf(5000));

        Faturamento faturamento = Faturamento.builder()
                .id(1L)
                .dataFaturamento(request.getDataFaturamento())
                .valor(request.getValor())
                .build();

        when(comissaoService.adicionarFaturamento(any(), any())).thenReturn(faturamento);

        mockMvc.perform(post("/api/v1/faturamento")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.valor").value(5000));
    }
}
