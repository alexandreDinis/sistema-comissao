package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.FaturaTenant;
import com.empresa.comissao.domain.entity.Licenca;
import com.empresa.comissao.domain.enums.StatusEmpresa;
import com.empresa.comissao.domain.enums.StatusFatura;
import com.empresa.comissao.dto.PaymentLinkResponse;
import com.empresa.comissao.repository.EmpresaRepository;
import com.empresa.comissao.repository.FaturaTenantRepository;
import com.empresa.comissao.repository.LicencaRepository;
import com.empresa.comissao.service.gateway.IPaymentGateway;
import com.empresa.comissao.service.gateway.PaymentGatewayFactory;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BillingTenantServiceTest {

    @Mock
    private FaturaTenantRepository faturaRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private LicencaRepository licencaRepository;

    @Mock
    private PaymentGatewayFactory gatewayFactory;

    @Mock
    private IPaymentGateway paymentGateway;

    @InjectMocks
    private BillingTenantService billingTenantService;

    private Licenca licencaAtiva;
    private Empresa tenantAtivo;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        licencaAtiva = Licenca.builder()
                .id(1L)
                .razaoSocial("Revendedor Teste")
                .gatewayPagamento("MERCADO_PAGO")
                .build();

        tenantAtivo = Empresa.builder()
                .id(10L)
                .nome("Inquilino Teste")
                .status(StatusEmpresa.ATIVA)
                .ativo(true)
                .valorMensalPago(new BigDecimal("50.00"))
                .build();
    }

    @Test
    @DisplayName("Deve gerar fatura para tenant com gateway de pagamento")
    void deveGerarFaturaParaTenant() {
        when(licencaRepository.findById(1L)).thenReturn(Optional.of(licencaAtiva));
        when(empresaRepository.findByLicencaIdAndStatus(1L, StatusEmpresa.ATIVA))
                .thenReturn(Collections.singletonList(tenantAtivo));

        String mesRef = YearMonth.now().toString();
        when(faturaRepository.existsByEmpresaIdAndMesReferencia(10L, mesRef)).thenReturn(false);

        PaymentLinkResponse mockLink = PaymentLinkResponse.builder()
                .paymentId("pay_123")
                .preferenceId("pref_123")
                .url("https://pagamento.com")
                .build();

        when(gatewayFactory.getGateway(licencaAtiva)).thenReturn(paymentGateway);
        when(paymentGateway.criarLinkPagamento(any(FaturaTenant.class), eq(licencaAtiva))).thenReturn(mockLink);

        when(faturaRepository.save(any(FaturaTenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        billingTenantService.gerarFaturasTenants(1L);

        ArgumentCaptor<FaturaTenant> captor = ArgumentCaptor.forClass(FaturaTenant.class);
        verify(faturaRepository, times(2)).save(captor.capture()); // First save then update with link

        FaturaTenant salva = captor.getValue();
        assertEquals(StatusFatura.PENDENTE, salva.getStatus());
        assertEquals(new BigDecimal("50.00"), salva.getValor());
        assertEquals(tenantAtivo, salva.getEmpresa());
        assertEquals("pay_123", salva.getPaymentId());
        assertEquals("https://pagamento.com", salva.getUrlPagamento());
    }

    @Test
    @DisplayName("Deve bloquear tenant inadimplente se a fatura está vencida")
    void deveBloquearTenantInadimplente() {
        FaturaTenant faturaAtrasada = FaturaTenant.builder()
                .id(99L)
                .empresa(tenantAtivo)
                .status(StatusFatura.PENDENTE)
                .dataVencimento(LocalDate.now().minusDays(1)) // Venceu
                .build();

        when(faturaRepository.findByStatusAndDataVencimentoBefore(eq(StatusFatura.PENDENTE), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(faturaAtrasada));

        billingTenantService.bloquearInadimplentes();

        assertEquals(StatusEmpresa.BLOQUEADA, tenantAtivo.getStatus());
        verify(empresaRepository).save(tenantAtivo);

        assertEquals(StatusFatura.VENCIDO, faturaAtrasada.getStatus());
        verify(faturaRepository).save(faturaAtrasada);
    }
}
