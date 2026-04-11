package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.FaturaLicenca;
import com.empresa.comissao.domain.entity.Licenca;
import com.empresa.comissao.domain.enums.StatusEmpresa;
import com.empresa.comissao.domain.enums.StatusFatura;
import com.empresa.comissao.domain.enums.StatusLicenca;
import com.empresa.comissao.repository.EmpresaRepository;
import com.empresa.comissao.repository.FaturaLicencaRepository;
import com.empresa.comissao.repository.LicencaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BillingLicencaServiceTest {

    @Mock
    private FaturaLicencaRepository faturaLicencaRepository;

    @Mock
    private LicencaRepository licencaRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private LicencaService licencaService;

    @InjectMocks
    private BillingLicencaService billingLicencaService;

    private Licenca licencaAtiva;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        licencaAtiva = Licenca.builder()
                .id(1L)
                .razaoSocial("Revendedor Teste")
                .status(StatusLicenca.ATIVA)
                .valorMensalidade(new BigDecimal("100.00"))
                .valorPorTenant(new BigDecimal("10.00"))
                .build();
    }

    @Test
    @DisplayName("Deve gerar nova fatura mensal para revendedores ATIVOS com cobranças corretas")
    void deveGerarFaturaParaRevendedorAtivo() {
        when(licencaRepository.findByStatus(StatusLicenca.ATIVA)).thenReturn(Collections.singletonList(licencaAtiva));
        String mesRef = YearMonth.now().toString();
        when(faturaLicencaRepository.existsByLicencaIdAndMesReferencia(licencaAtiva.getId(), mesRef)).thenReturn(false);
        when(empresaRepository.countByLicencaIdAndStatus(licencaAtiva.getId(), StatusEmpresa.ATIVA)).thenReturn(5L);

        billingLicencaService.gerarFaturasMensais();

        ArgumentCaptor<FaturaLicenca> captor = ArgumentCaptor.forClass(FaturaLicenca.class);
        verify(faturaLicencaRepository).save(captor.capture());

        FaturaLicenca salva = captor.getValue();
        assertEquals(StatusFatura.PENDENTE, salva.getStatus());
        assertEquals(licencaAtiva, salva.getLicenca());
        assertEquals(5, salva.getQuantidadeTenants());
        // 100 mensalidade + (5 * 10 tenants) = 150
        assertEquals(new BigDecimal("150.00"), salva.getValorTotal());
    }

    @Test
    @DisplayName("NÃO deve gerar nova fatura se já existir fatura no mês")
    void naoDeveGerarFaturaDuplicada() {
        when(licencaRepository.findByStatus(StatusLicenca.ATIVA)).thenReturn(Collections.singletonList(licencaAtiva));
        String mesRef = YearMonth.now().toString();
        when(faturaLicencaRepository.existsByLicencaIdAndMesReferencia(licencaAtiva.getId(), mesRef)).thenReturn(true);

        billingLicencaService.gerarFaturasMensais();

        verify(faturaLicencaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve suspender revendedor se sua fatura tiver vencido (math atualizada)")
    void deveSuspenderRevendedorInadimplente() {
        FaturaLicenca faturaVencida = FaturaLicenca.builder()
                .id(100L)
                .licenca(licencaAtiva)
                .status(StatusFatura.PENDENTE)
                .dataVencimento(LocalDate.now().minusDays(1)) // Venceu ontem
                .build();

        // O scheduler passa 'hoje'
        when(faturaLicencaRepository.findByStatusAndDataVencimentoBefore(eq(StatusFatura.PENDENTE), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(faturaVencida));

        billingLicencaService.suspenderInadimplentes();

        // Verifica a propagação da suspensão
        verify(licencaService).suspenderLicenca(eq(1L), anyString());
        assertEquals(StatusFatura.VENCIDO, faturaVencida.getStatus());
        verify(faturaLicencaRepository).save(faturaVencida);
    }
}
