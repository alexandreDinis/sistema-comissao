package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.CartaoCredito;
import com.empresa.comissao.domain.entity.ContaPagar;
import com.empresa.comissao.domain.enums.StatusConta;
import com.empresa.comissao.domain.enums.TipoContaPagar;
import com.empresa.comissao.repository.ContaPagarRepository;
import com.empresa.comissao.repository.DespesaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class FaturaServiceTest {

    @Mock
    private ContaPagarRepository contaPagarRepository;

    @Mock
    private DespesaRepository despesaRepository;

    @InjectMocks
    private FaturaService faturaService;

    private CartaoCredito cartao;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        cartao = CartaoCredito.builder()
                .id(1L)
                .nome("Nubank")
                .diaFechamento(25)
                .diaVencimento(5)
                .limite(new BigDecimal("5000.00"))
                .build();
    }

    @Test
    @DisplayName("Deve usar fatura existente se houver uma PENDENTE no mês")
    void deveUsarFaturaPendenteExistente() {
        LocalDate dataDespesa = LocalDate.of(2026, 1, 10);
        String mesRef = "2026-01";

        ContaPagar faturaPendente = ContaPagar.builder()
                .id(1L)
                .status(StatusConta.PENDENTE)
                .mesReferencia(mesRef)
                .valor(new BigDecimal("100.00"))
                .build();

        when(contaPagarRepository.findByCartaoAndMesReferenciaAndTipo(eq(cartao), eq(mesRef),
                eq(TipoContaPagar.FATURA_CARTAO)))
                .thenReturn(Collections.singletonList(faturaPendente));

        ContaPagar resultado = faturaService.buscarOuCriarFatura(cartao, dataDespesa);

        assertEquals(faturaPendente.getId(), resultado.getId());
        verify(contaPagarRepository, never()).save(any(ContaPagar.class));
    }

    @Test
    @DisplayName("Deve criar NOVA fatura (Complementar) se todas as existentes estiverem PAGAS")
    void deveCriarNovaFaturaSeExistenteEstiverPaga() {
        LocalDate dataDespesa = LocalDate.of(2026, 1, 20); // Antes do fechamento (25)
        String mesRef = "2026-01";

        ContaPagar faturaPaga = ContaPagar.builder()
                .id(1L)
                .status(StatusConta.PAGO)
                .mesReferencia(mesRef)
                .valor(new BigDecimal("1000.00"))
                .build();

        when(contaPagarRepository.findByCartaoAndMesReferenciaAndTipo(eq(cartao), eq(mesRef),
                eq(TipoContaPagar.FATURA_CARTAO)))
                .thenReturn(Collections.singletonList(faturaPaga));

        when(contaPagarRepository.save(any(ContaPagar.class))).thenAnswer(i -> {
            ContaPagar c = i.getArgument(0);
            c.setId(2L);
            return c;
        });

        ContaPagar resultado = faturaService.buscarOuCriarFatura(cartao, dataDespesa);

        assertNotEquals(faturaPaga.getId(), resultado.getId());
        assertEquals(StatusConta.PENDENTE, resultado.getStatus());
        assertEquals(mesRef, resultado.getMesReferencia());
        verify(contaPagarRepository).save(any(ContaPagar.class));
    }

    @Test
    @DisplayName("Deve calcular saldo restante corretamente ao atualizar fatura complementar")
    void deveCalcularSaldoRestanteFaturaComplementar() {
        String mesRef = "2026-01";

        // Cenário: Gastei 1500 total. Paguei 1000 na primeira fatura.
        // A nova fatura deve ser de 500.

        ContaPagar faturaComplementar = ContaPagar.builder()
                .id(2L)
                .cartao(cartao)
                .mesReferencia(mesRef)
                .status(StatusConta.PENDENTE)
                .build();

        // Mock: Total Despesas do Mês = 1500
        when(despesaRepository.sumByCartaoAndPeriodo(eq(cartao), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(new BigDecimal("1500.00"));

        // Mock: Total Já Pago no Mês = 1000
        when(contaPagarRepository.sumValorPagoByCartaoAndMes(eq(cartao), eq(mesRef), eq(StatusConta.PAGO)))
                .thenReturn(new BigDecimal("1000.00"));

        faturaService.atualizarValorFatura(faturaComplementar);

        assertEquals(new BigDecimal("500.00"), faturaComplementar.getValor());
        verify(contaPagarRepository).save(faturaComplementar);
    }
}
