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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingTenantService {

    private final FaturaTenantRepository faturaRepository;
    private final EmpresaRepository empresaRepository;
    private final LicencaRepository licencaRepository;
    private final PaymentGatewayFactory gatewayFactory;
    // private final EmailService emailService;

    /**
     * Gerar faturas para tenants de um revendedor específico
     */
    @Transactional
    public void gerarFaturasTenants(Long licencaId) {
        YearMonth mesReferencia = YearMonth.now();

        List<Empresa> tenants = empresaRepository.findByLicencaIdAndStatus(licencaId, StatusEmpresa.ATIVA);

        Licenca licenca = licencaRepository.findById(licencaId)
                .orElseThrow(() -> new IllegalArgumentException("Licença não encontrada"));

        log.info("Generating tenant invoices for Reseller: {}", licenca.getRazaoSocial());

        for (Empresa tenant : tenants) {
            if (tenant.getStatus() != StatusEmpresa.ATIVA)
                continue;

            try {
                gerarFaturaParaTenant(tenant, licenca, mesReferencia);
            } catch (Exception e) {
                log.error("Error generating invoice for tenant {}", tenant.getId(), e);
            }
        }
    }

    private void gerarFaturaParaTenant(Empresa tenant, Licenca licenca, YearMonth mes) {
        String mesRef = mes.toString();

        if (faturaRepository.existsByEmpresaIdAndMesReferencia(tenant.getId(), mesRef)) {
            return;
        }

        if (tenant.getValorMensalPago() == null || tenant.getValorMensalPago().signum() == 0) {
            log.warn("Tenant {} has no monthly value configured, skipping billing.", tenant.getId());
            return;
        }

        FaturaTenant fatura = FaturaTenant.builder()
                .empresa(tenant)
                .licenca(licenca)
                .mesReferencia(mesRef)
                .valor(tenant.getValorMensalPago())
                .dataEmissao(LocalDate.now())
                .dataVencimento(LocalDate.now().withDayOfMonth(5))
                .status(StatusFatura.PENDENTE)
                .gatewayPagamento(licenca.getGatewayPagamento())
                .tentativasCobranca(0)
                .build();

        fatura = faturaRepository.save(fatura);

        // Gerar link de pagamento no gateway do revendedor
        try {
            IPaymentGateway gateway = gatewayFactory.getGateway(licenca);
            PaymentLinkResponse link = gateway.criarLinkPagamento(fatura, licenca);

            fatura.setPaymentId(link.getPaymentId());
            fatura.setPreferenceId(link.getPreferenceId());
            fatura.setUrlPagamento(link.getUrl());
            fatura.setQrCodePix(link.getQrCodePix());
            fatura.setQrCodeImageUrl(link.getQrCodeImageUrl());

            faturaRepository.save(fatura);

            // emailService.enviarCobrancaTenant(tenant, fatura, licenca);
            log.info("Invoice created for Tenant {} with Gateway {}", tenant.getId(), licenca.getGatewayPagamento());

        } catch (Exception e) {
            log.error("Failed to generate payment link for tenant {}", tenant.getId(), e);
        }
    }

    @Transactional
    public void bloquearInadimplentes() {
        LocalDate hoje = LocalDate.now();
        // Bloquear quem venceu há mais de 1 dia (vence dia 5, dia 6 bloqueia)
        List<FaturaTenant> atrasadas = faturaRepository
                .findByStatusAndDataVencimentoBefore(StatusFatura.PENDENTE, hoje.minusDays(0));

        for (FaturaTenant fatura : atrasadas) {
            Empresa empresa = fatura.getEmpresa();
            if (empresa.getStatus() == StatusEmpresa.ATIVA) {
                empresa.setStatus(StatusEmpresa.BLOQUEADA);
                empresaRepository.save(empresa);

                // Atualizar status da fatura
                fatura.setStatus(StatusFatura.VENCIDO);
                faturaRepository.save(fatura);

                log.warn("Tenant {} BLOCKED due to delinquency", empresa.getId());
            }
        }
    }
}
