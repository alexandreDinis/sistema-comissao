package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.Despesa;
import com.empresa.comissao.dto.DespesaRequestDTO;
import com.empresa.comissao.service.ComissaoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DespesaController.class)
public class DespesaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ComissaoService comissaoService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin", roles = { "ADMIN" })
    public void criarDespesa_DeveRetornar200_QuandoAdmin() throws Exception {
        DespesaRequestDTO dto = new DespesaRequestDTO();
        dto.setDescricao("Almoço");
        dto.setValor(new BigDecimal("50.00"));
        dto.setDataDespesa(LocalDate.now());
        dto.setCategoria(com.empresa.comissao.domain.enums.CategoriaDespesa.ALIMENTACAO);

        Despesa despesa = Despesa.builder()
                .id(1L)
                .descricao("Almoço")
                .valor(new BigDecimal("50.00"))
                .build();

        when(comissaoService.adicionarDespesa(any(), any(), any(), any(), any())).thenReturn(despesa);

        mockMvc.perform(post("/api/v1/despesas")
                .with(csrf()) // important for security test
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
