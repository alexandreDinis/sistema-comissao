package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.FaturaLicenca;
import com.empresa.comissao.domain.entity.Licenca;
import com.empresa.comissao.domain.enums.StatusEmpresa;
import com.empresa.comissao.domain.enums.StatusFatura;
import com.empresa.comissao.domain.enums.StatusLicenca;
import com.empresa.comissao.repository.EmpresaRepository;
import com.empresa.comissao.repository.FaturaLicencaRepository;
import com.empresa.comissao.repository.LicencaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingLicencaService {

    private final FaturaLicencaRepository faturaLicencaRepository;
    private final LicencaRepository licencaRepository;
    private final EmpresaRepository empresaRepository;
    private final LicencaService licencaService;

    /**
     * Gerar faturas mensais para todos os revendedores
     * Roda dia 1º de cada mês
     */
    @Transactional
    public void gerarFaturasMensais() {
        YearMonth mesReferencia = YearMonth.now();
        List<Licenca> licencasAtivas = licencaRepository.findByStatus(StatusLicenca.ATIVA);

        log.info("Generating license invoices for {} resellers", licencasAtivas.size());

        for (Licenca licenca : licencasAtivas) {
            try {
                gerarFaturaParaLicenca(licenca, mesReferencia);
            } catch (Exception e) {
                log.error("Error generating invoice for license {}", licenca.getId(), e);
            }
        }
    }

    private void gerarFaturaParaLicenca(Licenca licenca, YearMonth mesReferencia) {
        String mesRef = mesReferencia.toString();
        if (faturaLicencaRepository.existsByLicencaIdAndMesReferencia(licenca.getId(), mesRef)) {
            log.info("Invoice already exists for license {} in month {}", licenca.getId(), mesRef);
            return;
        }

        // Contar tenants ativos (Assumindo que apenas ATIVOS contam para cobrança)
        // Contar tenants ativos conforme definição do plano
        int tenantsAtivos = (int) empresaRepository.countByLicencaIdAndStatus(licenca.getId(), StatusEmpresa.ATIVA);

        // Calcular valores
        BigDecimal valorMensalidade = licenca.getValorMensalidade();
        BigDecimal valorPorTenant = licenca.getValorPorTenant();
        BigDecimal valorTenants = valorPorTenant.multiply(new BigDecimal(tenantsAtivos));
        BigDecimal valorTotal = valorMensalidade.add(valorTenants);

        // Criar fatura
        FaturaLicenca fatura = FaturaLicenca.builder()
                .licenca(licenca)
                .mesReferencia(mesRef)
                .valorMensalidade(valorMensalidade)
                .quantidadeTenants(tenantsAtivos)
                .valorPorTenant(valorPorTenant)
                .valorTenants(valorTenants)
                .valorTotal(valorTotal)
                .dataEmissao(LocalDate.now())
                .dataVencimento(LocalDate.now().plusDays(7)) // Vence em 7 dias
                .status(StatusFatura.PENDENTE)
                .build();

        faturaLicencaRepository.save(fatura);

        log.info("Invoice created: License {} - {} tenants - R$ {}",
                licenca.getId(), tenantsAtivos, valorTotal);
    }

    /**
     * Suspender licenças inadimplentes
     * Roda dia 10 (3 dias após vencimento padrão de 7 dias se gerado dia 1)
     */
    @Transactional
    public void suspenderInadimplentes() {
        LocalDate hoje = LocalDate.now();
        LocalDate limiteVencimento = hoje.minusDays(3); // Tolerância de 3 dias

        List<FaturaLicenca> vencidas = faturaLicencaRepository
                .findByStatusAndDataVencimentoBefore(StatusFatura.PENDENTE, limiteVencimento);

        for (FaturaLicenca fatura : vencidas) {
            Licenca licenca = fatura.getLicenca();
            if (licenca.getStatus() == StatusLicenca.ATIVA) {
                licencaService.suspenderLicenca(
                        licenca.getId(),
                        "INADIMPLÊNCIA - Fatura " + fatura.getId());

                // Atualizar status da fatura para VENCIDO caso ainda esteja PENDENTE
                // (O findByStatus busca PENDENTE na chamada acima, mas é bom deixar
                // explicitado)
                fatura.setStatus(StatusFatura.VENCIDO);
                faturaLicencaRepository.save(fatura);
            }
        }
    }
}
