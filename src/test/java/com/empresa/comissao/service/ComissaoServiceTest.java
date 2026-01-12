package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.ComissaoCalculada;
import com.empresa.comissao.repository.ComissaoCalculadaRepository;
import com.empresa.comissao.repository.FaturamentoRepository;
import com.empresa.comissao.repository.PagamentoAdiantadoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComissaoServiceTest {

    @Mock
    private FaturamentoRepository faturamentoRepository;
    @Mock
    private PagamentoAdiantadoRepository adiantamentoRepository;
    @Mock
    private ComissaoCalculadaRepository comissaoRepository;

    @InjectMocks
    private ComissaoService comissaoService;

    @Test
    void deveCalcularComissaoComSucesso() {
        YearMonth anoMes = YearMonth.of(2026, 1);
        BigDecimal faturamentoTotal = new BigDecimal("20000.00");

        when(comissaoRepository.findFirstByAnoMesReferencia(anoMes)).thenReturn(Optional.empty());
        when(faturamentoRepository.sumValorByDataFaturamentoBetween(any(), any()))
                .thenReturn(Optional.of(faturamentoTotal));
        when(adiantamentoRepository.sumValorByDataPagamentoBetween(any(), any()))
                .thenReturn(Optional.of(new BigDecimal("1000.00")));

        ComissaoCalculada resultado = comissaoService.calcularEObterComissaoMensal(2026, 1);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getValorBrutoComissao()).isEqualTo(new BigDecimal("3000.00")); // 15% de 20000
        assertThat(resultado.getSaldoAReceber()).isEqualTo(new BigDecimal("2000.00")); // 3000 - 1000
    }
}
