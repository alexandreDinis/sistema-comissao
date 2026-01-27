package com.empresa.comissao.scheduler;

import com.empresa.comissao.service.BillingLicencaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class BillingMasterScheduler {

    private final BillingLicencaService billingLicencaService;

    // Gerar faturas para revendedores - Dia 1º às 02:00
    @Scheduled(cron = "0 0 2 1 * ?")
    public void gerarFaturasLicencas() {
        log.info("Running job: Generate License Invoices");
        billingLicencaService.gerarFaturasMensais();
    }

    // Suspender revendedores inadimplentes - Dia 10 às 03:00
    @Scheduled(cron = "0 0 3 10 * ?")
    public void suspenderRevendedoresInadimplentes() {
        log.info("Running job: Suspend Delinquent Resellers");
        billingLicencaService.suspenderInadimplentes();
    }

    // ========== REVENDEDOR → TENANT ==========

    private final com.empresa.comissao.service.BillingTenantService billingTenantService;
    private final com.empresa.comissao.repository.LicencaRepository licencaRepository;

    // Gerar faturas dos tenants - Dia 1º às 02:30 (para cada revendedor)
    @Scheduled(cron = "0 30 2 1 * ?")
    public void gerarFaturasTenants() {
        log.info("Running job: Generate Tenant Invoices");

        java.util.List<com.empresa.comissao.domain.entity.Licenca> licencasAtivas = licencaRepository
                .findByStatus(com.empresa.comissao.domain.enums.StatusLicenca.ATIVA);

        for (com.empresa.comissao.domain.entity.Licenca licenca : licencasAtivas) {
            billingTenantService.gerarFaturasTenants(licenca.getId());
        }
    }

    // Bloquear tenants inadimplentes - Dia 6 às 01:00
    @Scheduled(cron = "0 0 1 6 * ?")
    public void bloquearTenantsInadimplentes() {
        log.info("Running job: Block Delinquent Tenants");
        billingTenantService.bloquearInadimplentes();
    }
}
