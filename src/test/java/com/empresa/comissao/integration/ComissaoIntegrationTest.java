package com.empresa.comissao.integration;

import com.empresa.comissao.controller.FaturamentoController;
import com.empresa.comissao.controller.ComissaoController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ComissaoIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void deveExecutarFluxoCompletoDeCalculoDeComissao() {
        // 1. Criar Faturamentos
        FaturamentoController.FaturamentoRequest f1 = new FaturamentoController.FaturamentoRequest();
        f1.setDataFaturamento(LocalDate.of(2026, 1, 10));
        f1.setValor(new BigDecimal("10000.00"));
        restTemplate.postForEntity("/api/v1/faturamento", f1, Object.class);

        FaturamentoController.FaturamentoRequest f2 = new FaturamentoController.FaturamentoRequest();
        f2.setDataFaturamento(LocalDate.of(2026, 1, 20));
        f2.setValor(new BigDecimal("12500.50"));
        restTemplate.postForEntity("/api/v1/faturamento", f2, Object.class);

        // 2. Obter Comissão
        ResponseEntity<ComissaoController.ComissaoResponse> response = restTemplate.getForEntity(
                "/api/v1/comissao/2026/01",
                ComissaoController.ComissaoResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ComissaoController.ComissaoResponse comissao = response.getBody();
        assertThat(comissao).isNotNull();
        assertThat(comissao.getFaturamentoMensal()).isEqualByComparingTo(new BigDecimal("22500.50"));
        assertThat(comissao.getFaixaComissao()).isEqualTo("R$ 20.000,01 a R$ 24.996,00");
    }
}
