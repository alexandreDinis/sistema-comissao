package com.empresa.comissao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.repository.ContaPagarRepository;
import com.empresa.comissao.repository.ContaReceberRepository;
import com.empresa.comissao.repository.RecebimentoRepository;
import com.empresa.comissao.service.FinanceiroService.ResumoFinanceiro;

@ExtendWith(MockitoExtension.class)
public class LucroDistributionCarryoverTest {

    @Mock
    private ContaPagarRepository contaPagarRepository;

    @Mock
    private ContaReceberRepository contaReceberRepository;

    @Mock
    private RecebimentoRepository recebimentoRepository;

    @InjectMocks
    private FinanceiroService financeiroService;

    @Test
    void testReceitaMarcoPersisteEmAbril() {
        Empresa empresa = new Empresa();
        empresa.setId(1L);

        // Simulando que estamos gerando a visao do caixa HOJE (Abril):
        // A lógica do getResumoFinanceiro usa LocalDate.now().plusDays(1) para pegar tudo gerado até hoje.
        
        // Simular Entradas Globais (TUDO QUE ENTROU NA HISTÓRIA ATÉ HOJE)
        // O recebimentoRepository vai retornar a quantia somada de TODOS OS RECEBIMENTOS, incluindo 
        // a nova "receita de março" que não foi retirada.
        // Simulamos que R$ 2.000 entraram em Março e R$ 10.000 entraram em Abril.
        when(recebimentoRepository.sumByEmpresaAndDataPagamentoBefore(eq(empresa), any(LocalDate.class)))
            .thenReturn(new BigDecimal("12000.00"));

        // Simular Saidas Globais (TUDO QUE SAIU E FOI PAGO NA HISTÓRIA ATÉ HOJE)
        // Saidas totais de R$ 5.000
        when(contaPagarRepository.sumByPagamentoBefore(eq(empresa), any(LocalDate.class)))
            .thenReturn(new BigDecimal("5000.00"));

        // Dívidas pendentes TOTAIS (que deduzem do saldo livre) = R$ 1.000
        when(contaPagarRepository.sumPendentesByEmpresa(eq(empresa)))
            .thenReturn(new BigDecimal("1000.00"));

        when(contaReceberRepository.sumPendentesByEmpresa(any())).thenReturn(BigDecimal.ZERO);
        when(contaPagarRepository.countVencendoProximos(any(), any(), any())).thenReturn(0L);
        when(contaReceberRepository.countVencendoProximos(any(), any(), any())).thenReturn(0L);

        // Executa a lógica da Dashboard / Tela de Distribuição de Lucros
        ResumoFinanceiro resumo = financeiroService.getResumoFinanceiro(empresa);

        // O Saldo Atual na conta (Conta Bancária Real) deve ser: Entradas (12k) - Saídas Pagas (5k) = 7k
        assertEquals(new BigDecimal("7000.00"), resumo.saldoAtual(), "O saldo na conta DEVE conter os saldos deixados no caixa dos meses anteriores");

        // O Caixa Livre ("Diferença") para Distribuição = Saldo Atual - Dívidas Pendentes
        BigDecimal caixaLivre = resumo.saldoAtual().subtract(resumo.totalAPagar());
        assertEquals(new BigDecimal("6000.00"), caixaLivre, "O caixa livre considera o saldo acumulado total subtraído das dívidas atuais.");
    }
}
