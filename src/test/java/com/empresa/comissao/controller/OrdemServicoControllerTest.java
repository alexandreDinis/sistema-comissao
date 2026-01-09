package com.empresa.comissao.controller;

import com.empresa.comissao.dto.request.OrdemServicoRequest;
import com.empresa.comissao.dto.response.ClienteResponse;
import com.empresa.comissao.dto.response.OrdemServicoResponse;
import com.empresa.comissao.service.OrdemServicoService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrdemServicoController.class)
public class OrdemServicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrdemServicoService osService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser
    public void criarOS_DeveRetornar200() throws Exception {
        OrdemServicoRequest request = new OrdemServicoRequest();
        request.setClienteId(1L);
        request.setData(LocalDate.now());

        OrdemServicoResponse response = OrdemServicoResponse.builder()
                .id(1L)
                .data(LocalDate.now())
                .cliente(ClienteResponse.builder().id(1L).build())
                .build();

        when(osService.criarOS(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/ordens-servico")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }
}
