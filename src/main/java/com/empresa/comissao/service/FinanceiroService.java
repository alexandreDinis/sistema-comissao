package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.*;
import com.empresa.comissao.domain.enums.*;
import com.empresa.comissao.exception.BusinessException;
import com.empresa.comissao.repository.ContaPagarRepository;
import com.empresa.comissao.repository.ContaReceberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Serviço para gestão financeira.
 * Gerencia contas a pagar, contas a receber e fluxo de caixa.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FinanceiroService {

    private final ContaPagarRepository contaPagarRepository;
    private final ContaReceberRepository contaReceberRepository;
    private final ComissaoService comissaoService;

    // ========================================
    // CONTAS A PAGAR
    // ========================================

    /**
     * Cria uma conta a pagar a partir de uma despesa.
     */
    @Transactional
    public ContaPagar criarContaPagarDeDespesa(Despesa despesa, boolean pagoAgora, LocalDate dataVencimento,
            MeioPagamento meioPagamento) {
        log.info("💸 Criando conta a pagar para despesa ID: {}", despesa.getId());

        StatusConta status = pagoAgora ? StatusConta.PAGO : StatusConta.PENDENTE;
        LocalDate vencimento = pagoAgora ? despesa.getDataDespesa()
                : (dataVencimento != null ? dataVencimento : despesa.getDataDespesa().plusDays(30));
        LocalDate dataPagamento = pagoAgora ? despesa.getDataDespesa() : null;

        ContaPagar conta = ContaPagar.builder()
                .empresa(despesa.getEmpresa())
                .descricao(despesa.getDescricao() != null ? despesa.getDescricao() : despesa.getCategoria().name())
                .valor(despesa.getValor())
                .dataCompetencia(despesa.getDataDespesa())
                .dataVencimento(vencimento)
                .dataPagamento(dataPagamento)
                .meioPagamento(meioPagamento)
                .status(status)
                .tipo(TipoContaPagar.DESPESA_OPERACIONAL)
                .despesa(despesa)
                .build();

        ContaPagar salva = contaPagarRepository.save(conta);
        log.info("✅ Conta a pagar criada com ID: {} | Status: {}", salva.getId(), status);
        return salva;
    }

    /**
     * Cria uma conta a pagar para comissão de funcionário.
     */
    @Transactional
    public ContaPagar criarContaPagarDeComissao(ComissaoCalculada comissao, User funcionario,
            LocalDate dataVencimento) {
        log.info("💸 Criando conta a pagar de comissão para funcionário: {}", funcionario.getEmail());

        ContaPagar conta = ContaPagar.builder()
                .empresa(comissao.getEmpresa())
                .descricao("Comissão " + comissao.getAnoMesReferencia() + " - " + funcionario.getEmail())
                .valor(comissao.getSaldoAReceber())
                .dataCompetencia(comissao.getAnoMesReferencia().atDay(1))
                .dataVencimento(dataVencimento)
                .status(StatusConta.PENDENTE)
                .tipo(TipoContaPagar.COMISSAO_FUNCIONARIO)
                .comissao(comissao)
                .funcionario(funcionario)
                .build();

        ContaPagar salva = contaPagarRepository.save(conta);
        log.info("✅ Conta a pagar de comissão criada com ID: {}", salva.getId());
        return salva;
    }

    /**
     * Cria múltiplas contas a pagar (parcelamento).
     */
    @Transactional
    public List<ContaPagar> criarContaPagarParcelada(Despesa despesa, int quantidadeParcelas,
            LocalDate dataVencimentoPrimeira) {
        log.info("💸 Criando {} parcelas para despesa ID: {}", quantidadeParcelas, despesa.getId());

        BigDecimal valorParcela = despesa.getValor().divide(
                BigDecimal.valueOf(quantidadeParcelas), 2, java.math.RoundingMode.HALF_UP);

        List<ContaPagar> parcelas = new ArrayList<>();
        ContaPagar primeiraParcela = null;

        for (int i = 0; i < quantidadeParcelas; i++) {
            LocalDate vencimento = dataVencimentoPrimeira.plusMonths(i);

            ContaPagar parcela = ContaPagar.builder()
                    .empresa(despesa.getEmpresa())
                    .descricao(despesa.getDescricao() + " - Parcela " + (i + 1) + "/" + quantidadeParcelas)
                    .valor(valorParcela)
                    .dataCompetencia(despesa.getDataDespesa())
                    .dataVencimento(vencimento)
                    .status(StatusConta.PENDENTE)
                    .tipo(TipoContaPagar.DESPESA_OPERACIONAL)
                    .despesa(despesa)
                    .numeroParcela(i + 1)
                    .totalParcelas(quantidadeParcelas)
                    .parcelaOrigem(primeiraParcela)
                    .build();

            ContaPagar salva = contaPagarRepository.save(parcela);

            if (i == 0) {
                primeiraParcela = salva;
            }

            parcelas.add(salva);
        }

        log.info("✅ {} parcelas criadas", parcelas.size());
        return parcelas;
    }

    /**
     * Marca uma conta a pagar como paga.
     */
    @Transactional
    public ContaPagar pagarConta(Long contaId, LocalDate dataPagamento, MeioPagamento meioPagamento) {
        ContaPagar conta = contaPagarRepository.findById(contaId)
                .orElseThrow(() -> new BusinessException("Conta a pagar não encontrada: " + contaId));

        if (conta.getStatus() == StatusConta.PAGO) {
            throw new BusinessException("Conta já está paga");
        }

        conta.marcarComoPago(dataPagamento, meioPagamento);
        ContaPagar salva = contaPagarRepository.save(conta);
        log.info("✅ Conta {} marcada como paga em {}", contaId, dataPagamento);
        return salva;
    }

    /**
     * Lista contas a pagar pendentes.
     */
    public List<ContaPagar> listarContasPagarPendentes(Empresa empresa) {
        return contaPagarRepository.findByEmpresaAndStatusOrderByDataVencimentoAsc(empresa, StatusConta.PENDENTE);
    }

    /**
     * Lista contas a pagar vencidas.
     */
    public List<ContaPagar> listarContasPagarVencidas(Empresa empresa) {
        return contaPagarRepository.findVencidasByEmpresa(empresa, LocalDate.now());
    }

    // ========================================
    // CONTAS A RECEBER
    // ========================================

    /**
     * Cria uma conta a receber a partir de um faturamento.
     * Chamado automaticamente ao finalizar OS.
     */
    @Transactional
    public ContaReceber criarContaReceberDeFaturamento(Faturamento faturamento, LocalDate dataVencimento,
            boolean pagamentoAvista, MeioPagamento meioPagamento) {
        log.info("💰 Criando conta a receber para faturamento ID: {}", faturamento.getId());

        // Verificar se já existe
        if (contaReceberRepository.findByFaturamentoId(faturamento.getId()).isPresent()) {
            throw new BusinessException("Já existe conta a receber para este faturamento");
        }

        ContaReceber.ContaReceberBuilder builder = ContaReceber.builder()
                .empresa(faturamento.getEmpresa())
                .descricao("OS #"
                        + (faturamento.getOrdemServico() != null ? faturamento.getOrdemServico().getId() : "N/A"))
                .valor(faturamento.getValor())
                .dataCompetencia(faturamento.getDataFaturamento())
                .dataVencimento(dataVencimento)
                .tipo(TipoContaReceber.ORDEM_SERVICO)
                .faturamento(faturamento)
                .ordemServico(faturamento.getOrdemServico())
                .funcionarioResponsavel(faturamento.getUsuario()) // Para cálculo de comissão individual
                .meioPagamento(meioPagamento);

        if (pagamentoAvista) {
            // Pagamento à vista: já está pago
            builder.status(StatusConta.PAGO)
                    .dataRecebimento(faturamento.getDataFaturamento());
            log.info("💵 Pagamento à vista detectado");
        } else {
            builder.status(StatusConta.PENDENTE);
            log.info("📅 Pagamento a prazo - vencimento: {}", dataVencimento);
        }

        ContaReceber conta = builder.build();
        ContaReceber salva = contaReceberRepository.save(conta);
        log.info("✅ Conta a receber criada com ID: {} - Status: {}", salva.getId(), salva.getStatus());
        return salva;
    }

    /**
     * Marca uma conta a receber como recebida.
     */
    @Transactional
    public ContaReceber receberConta(Long contaId, LocalDate dataRecebimento, MeioPagamento meioPagamento) {
        ContaReceber conta = contaReceberRepository.findById(contaId)
                .orElseThrow(() -> new BusinessException("Conta a receber não encontrada: " + contaId));

        if (conta.getStatus() == StatusConta.PAGO) {
            throw new BusinessException("Conta já está recebida");
        }

        conta.marcarComoRecebido(dataRecebimento, meioPagamento);
        ContaReceber salva = contaReceberRepository.save(conta);
        log.info("✅ Conta {} marcada como recebida em {}", contaId, dataRecebimento);

        // INVALIDAR CACHE DE COMISSÃO
        try {
            java.time.YearMonth mesReferencia = java.time.YearMonth.from(dataRecebimento);

            // 1. Invalidate Company Cache (Coletiva mode uses this)
            if (conta.getEmpresa() != null) {
                comissaoService.invalidarCacheEmpresa(conta.getEmpresa(), mesReferencia);
            }

            // 2. Invalidate User Cache (Individual mode uses this)
            if (conta.getFuncionarioResponsavel() != null) {
                comissaoService.invalidarCache(conta.getFuncionarioResponsavel(), mesReferencia);
            }
        } catch (Exception e) {
            log.warn("⚠️ Falha ao invalidar cache de comissão: {}", e.getMessage());
        }

        return salva;
    }

    /**
     * Lista contas a receber pendentes.
     */
    public List<ContaReceber> listarContasReceberPendentes(Empresa empresa) {
        return contaReceberRepository.findByEmpresaAndStatusOrderByDataVencimentoAsc(empresa, StatusConta.PENDENTE);
    }

    /**
     * Lista contas a receber filtrando por status (opcional).
     */
    public List<ContaReceber> listarContasReceber(Empresa empresa, StatusConta status) {
        if (status != null) {
            return contaReceberRepository.findByEmpresaAndStatusOrderByDataVencimentoAsc(empresa, status);
        }
        return contaReceberRepository.findByEmpresaOrderByDataVencimentoAsc(empresa);
    }

    /**
     * Lista contas a receber vencidas.
     */
    public List<ContaReceber> listarContasReceberVencidas(Empresa empresa) {
        return contaReceberRepository.findVencidasByEmpresa(empresa, LocalDate.now());
    }

    // ========================================
    // FLUXO DE CAIXA
    // ========================================

    /**
     * Obtém o total recebido em um período (para cálculo de comissão).
     * CRÍTICO: Base para o novo cálculo de comissão.
     */
    public BigDecimal getTotalRecebidoNoPeriodo(Empresa empresa, YearMonth periodo) {
        LocalDate inicio = periodo.atDay(1);
        LocalDate fim = periodo.atEndOfMonth();
        return contaReceberRepository.sumByRecebimentoBetween(empresa, inicio, fim);
    }

    /**
     * Obtém o total recebido por funcionário em um período (comissão individual).
     */
    public BigDecimal getTotalRecebidoPorFuncionario(Empresa empresa, User funcionario, YearMonth periodo) {
        LocalDate inicio = periodo.atDay(1);
        LocalDate fim = periodo.atEndOfMonth();
        return contaReceberRepository.sumByRecebimentoBetweenAndFuncionario(empresa, funcionario, inicio, fim);
    }

    /**
     * Obtém o total pago em um período (saídas de caixa).
     */
    public BigDecimal getTotalPagoNoPeriodo(Empresa empresa, YearMonth periodo) {
        LocalDate inicio = periodo.atDay(1);
        LocalDate fim = periodo.atEndOfMonth();
        return contaPagarRepository.sumByPagamentoBetween(empresa, inicio, fim);
    }

    /**
     * Obtém o saldo do período (entradas - saídas).
     */
    public BigDecimal getSaldoCaixaPeriodo(Empresa empresa, YearMonth periodo) {
        BigDecimal entradas = getTotalRecebidoNoPeriodo(empresa, periodo);
        BigDecimal saidas = getTotalPagoNoPeriodo(empresa, periodo);
        return entradas.subtract(saidas);
    }

    // ========================================
    // RECEITA POR CAIXA (BASE DAS)
    // ========================================

    /**
     * Gera relatório detalhado de receitas por caixa.
     * Este é o relatório BASE PARA O DAS (Simples Nacional).
     * Considera apenas recebimentos com status PAGO no período.
     */
    public com.empresa.comissao.dto.ReceitaCaixaReportDTO getReceitasCaixa(Empresa empresa, YearMonth periodo) {
        LocalDate inicio = periodo.atDay(1);
        LocalDate fim = periodo.atEndOfMonth();

        // Buscar todas as contas recebidas no período
        List<ContaReceber> contasRecebidas = contaReceberRepository
                .findRecebidosBetween(empresa, inicio, fim);

        // Converter para DTOs
        List<com.empresa.comissao.dto.ReceitaCaixaDTO> recebimentos = contasRecebidas.stream()
                .map(conta -> com.empresa.comissao.dto.ReceitaCaixaDTO.builder()
                        .dataRecebimento(conta.getDataRecebimento())
                        .valor(conta.getValor())
                        .origem(gerarOrigemConta(conta))
                        .cliente(conta.getOrdemServico() != null && conta.getOrdemServico().getCliente() != null
                                ? conta.getOrdemServico().getCliente().getRazaoSocial()
                                : null)
                        .meioPagamento(conta.getMeioPagamento() != null ? conta.getMeioPagamento().name() : null)
                        .documentoFiscal(null) // Campo para preenchimento manual futuro
                        .build())
                .toList();

        // Calcular total
        BigDecimal totalRecebido = recebimentos.stream()
                .map(com.empresa.comissao.dto.ReceitaCaixaDTO::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        log.info("📊 Receita por Caixa {}/{}: {} recebimentos, total R$ {}",
                periodo.getMonthValue(), periodo.getYear(), recebimentos.size(), totalRecebido);

        return com.empresa.comissao.dto.ReceitaCaixaReportDTO.builder()
                .ano(periodo.getYear())
                .mes(periodo.getMonthValue())
                .recebimentos(recebimentos)
                .totalRecebido(totalRecebido)
                .quantidadeRecebimentos(recebimentos.size())
                .build();
    }

    private String gerarOrigemConta(ContaReceber conta) {
        if (conta.getOrdemServico() != null) {
            return "OS #" + conta.getOrdemServico().getId();
        }
        if (conta.getFaturamento() != null) {
            return "Faturamento #" + conta.getFaturamento().getId();
        }
        return conta.getDescricao() != null ? conta.getDescricao() : "Recebimento";
    }

    // ========================================
    // RESUMO FINANCEIRO
    // ========================================

    /**
     * Obtém resumo financeiro da empresa.
     */
    public ResumoFinanceiro getResumoFinanceiro(Empresa empresa) {
        BigDecimal totalAPagar = contaPagarRepository.sumPendentesByEmpresa(empresa);
        BigDecimal totalAReceber = contaReceberRepository.sumPendentesByEmpresa(empresa);
        long contasVencendo = contaPagarRepository.countVencendoProximos(
                empresa, LocalDate.now(), LocalDate.now().plusDays(7));
        long recebimentosVencendo = contaReceberRepository.countVencendoProximos(
                empresa, LocalDate.now(), LocalDate.now().plusDays(7));

        return new ResumoFinanceiro(totalAPagar, totalAReceber, contasVencendo, recebimentosVencendo);
    }

    /**
     * DTO para resumo financeiro.
     */
    public record ResumoFinanceiro(
            BigDecimal totalAPagar,
            BigDecimal totalAReceber,
            long contasVencendoProximos7Dias,
            long recebimentosVencendoProximos7Dias) {
        public BigDecimal getSaldoProjetado() {
            return totalAReceber.subtract(totalAPagar);
        }
    }
}
