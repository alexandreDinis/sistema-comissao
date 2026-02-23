package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.*;
import com.empresa.comissao.domain.enums.*;
import com.empresa.comissao.exception.BusinessException;
import com.empresa.comissao.repository.ContaPagarRepository;
import com.empresa.comissao.repository.ContaReceberRepository;
import com.empresa.comissao.repository.RecebimentoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import com.empresa.comissao.dto.RelatorioReceitaCaixaDTO;
import com.empresa.comissao.dto.RelatorioFluxoCaixaDTO;
import com.empresa.comissao.dto.RelatorioContasPagarDTO;
import com.empresa.comissao.dto.RelatorioDistribuicaoLucrosDTO;

import java.util.List;
import java.util.stream.Collectors;

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
        private final RecebimentoRepository recebimentoRepository;
        private final com.empresa.comissao.repository.FaturamentoRepository faturamentoRepository;
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
                                .descricao(despesa.getDescricao() != null ? despesa.getDescricao()
                                                : despesa.getCategoria().name())
                                .valor(despesa.getValor())
                                .dataCompetencia(despesa.getDataDespesa())
                                .dataVencimento(vencimento)
                                .dataPagamento(dataPagamento)
                                .meioPagamento(meioPagamento)
                                .status(status)
                                .tipo(TipoContaPagar.OPERACIONAL)
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
                                .descricao("Comissão " + comissao.getAnoMesReferencia() + " - "
                                                + funcionario.getEmail())
                                .valor(comissao.getSaldoAReceber())
                                .dataCompetencia(comissao.getAnoMesReferencia().atDay(1))
                                .dataVencimento(dataVencimento)
                                .status(StatusConta.PENDENTE)
                                .tipo(TipoContaPagar.FOLHA_PAGAMENTO)
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
                                        .descricao(despesa.getDescricao() + " - Parcela " + (i + 1) + "/"
                                                        + quantidadeParcelas)
                                        .valor(valorParcela)
                                        .dataCompetencia(despesa.getDataDespesa())
                                        .dataVencimento(vencimento)
                                        .status(StatusConta.PENDENTE)
                                        .tipo(TipoContaPagar.OPERACIONAL)
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
                return contaPagarRepository.findByEmpresaAndStatusOrderByDataVencimentoAsc(empresa,
                                StatusConta.PENDENTE);
        }

        /**
         * Lista contas a pagar já pagas.
         */
        public List<ContaPagar> listarContasPagarPagas(Empresa empresa) {
                return contaPagarRepository.findByEmpresaAndStatusOrderByDataVencimentoAsc(empresa, StatusConta.PAGO);
        }

        /**
         * Lista todas as contas a pagar.
         */
        public List<ContaPagar> listarTodasContasPagar(Empresa empresa) {
                return contaPagarRepository.findByEmpresaOrderByDataVencimentoAsc(empresa);
        }

        /**
         * Lista contas a pagar vencidas.
         */
        public List<ContaPagar> listarContasPagarVencidas(Empresa empresa) {
                return contaPagarRepository.findVencidasByEmpresa(empresa, LocalDate.now());
        }

        /**
         * Cria conta a pagar para prestador de serviço terceirizado.
         * Chamado automaticamente ao finalizar OS com serviços terceirizados.
         * 
         * @param prestador       O prestador de serviço
         * @param valor           Valor a pagar
         * @param descricao       Descrição da conta
         * @param dataVencimento  Data de vencimento
         * @param dataCompetencia Data de competência (normalmente a data da OS)
         * @param empresa         Empresa do contexto
         */
        @Transactional
        public ContaPagar criarContaPagarPrestador(
                        com.empresa.comissao.domain.entity.Prestador prestador,
                        BigDecimal valor,
                        String descricao,
                        LocalDate dataVencimento,
                        LocalDate dataCompetencia,
                        Empresa empresa) {

                log.info("💸 Criando conta a pagar para prestador: {} - R$ {} (competência: {}, vencimento: {})",
                                prestador.getNome(), valor, dataCompetencia, dataVencimento);

                ContaPagar conta = ContaPagar.builder()
                                .empresa(empresa)
                                .descricao(descricao)
                                .valor(valor)
                                .dataCompetencia(dataCompetencia)
                                .dataVencimento(dataVencimento)
                                .status(StatusConta.PENDENTE)
                                .tipo(TipoContaPagar.OPERACIONAL) // Prestador = Fornecedor de serviço
                                .build();

                ContaPagar salva = contaPagarRepository.save(conta);
                log.info("✅ Conta a pagar ID {} criada para prestador {}", salva.getId(), prestador.getNome());
                return salva;
        }

        /**
         * Cria conta a pagar automaticamente para adiantamento (STATUS: PAGO).
         */
        @Transactional
        public ContaPagar criarDespesaAdiantamento(
                        com.empresa.comissao.domain.entity.PagamentoAdiantado adiantamento,
                        Empresa empresa) {

                log.info("💸 Criando despesa automática para adiantamento ID: {}", adiantamento.getId());

                String descricao = String.format("Adiantamento Salarial - %s - %s",
                                adiantamento.getUsuario() != null ? adiantamento.getUsuario().getUsername() : "N/A",
                                adiantamento.getDescricao() != null ? adiantamento.getDescricao() : "");

                ContaPagar conta = ContaPagar.builder()
                                .empresa(empresa)
                                .funcionario(adiantamento.getUsuario())
                                .descricao(descricao)
                                .valor(adiantamento.getValor())
                                .dataCompetencia(adiantamento.getDataPagamento()) // Dia do adiantamento
                                .dataVencimento(adiantamento.getDataPagamento()) // Venceu no dia
                                .dataPagamento(adiantamento.getDataPagamento()) // Já foi pago
                                .status(StatusConta.PAGO)
                                .tipo(TipoContaPagar.FOLHA_PAGAMENTO)
                                .meioPagamento(MeioPagamento.PIX) // Default para adiantamento
                                .build();

                ContaPagar salva = contaPagarRepository.save(conta);
                log.info("✅ Despesa/Conta Paga ID {} criada para adiantamento", salva.getId());
                return salva;
        }

        /**
         * Cria conta a pagar pendente para saldo de comissão.
         */
        @Transactional
        public ContaPagar criarContaPagarComissao(
                        com.empresa.comissao.domain.entity.ComissaoCalculada comissao,
                        Empresa empresa,
                        LocalDate dataVencimento) {

                log.info("💸 Criando conta a pagar para comissão ID: {}", comissao.getId());

                String nomeUsuario = comissao.getUsuario() != null ? comissao.getUsuario().getUsername()
                                : (empresa != null ? empresa.getNome() : "GLOBAL");

                String descricao = String.format("Comissão %s - Ref: %s",
                                nomeUsuario,
                                comissao.getAnoMesReferencia().toString());

                ContaPagar conta = ContaPagar.builder()
                                .empresa(empresa)
                                .funcionario(comissao.getUsuario())
                                .descricao(descricao)
                                .valor(comissao.getSaldoAReceber()) // Valor Líquido
                                .dataCompetencia(comissao.getAnoMesReferencia().atEndOfMonth())
                                .dataVencimento(dataVencimento)
                                .status(StatusConta.PENDENTE)
                                .tipo(TipoContaPagar.FOLHA_PAGAMENTO)
                                .build();

                ContaPagar salva = contaPagarRepository.save(conta);
                log.info("✅ Conta a pagar ID {} criada para comissão", salva.getId());
                return salva;
        }

        /**
         * Cria conta a pagar JÁ PAGA para saldo de comissão.
         * Usado quando o usuário quita a comissão diretamente ('Apenas Marcar como
         * Pago').
         */
        @Transactional
        public ContaPagar criarContaPagarComissaoQuitada(
                        com.empresa.comissao.domain.entity.ComissaoCalculada comissao,
                        Empresa empresa) {

                log.info("💸 Criando conta a pagar QUITADA para comissão ID: {}", comissao.getId());

                String nomeUsuario = comissao.getUsuario() != null ? comissao.getUsuario().getUsername()
                                : (empresa != null ? empresa.getNome() : "GLOBAL");

                String descricao = String.format("Comissão %s - Ref: %s (Quitada)",
                                nomeUsuario,
                                comissao.getAnoMesReferencia().toString());

                ContaPagar conta = ContaPagar.builder()
                                .empresa(empresa)
                                .funcionario(comissao.getUsuario())
                                .descricao(descricao)
                                .valor(comissao.getSaldoAReceber()) // Valor Líquido
                                .dataCompetencia(comissao.getAnoMesReferencia().atEndOfMonth())
                                .dataVencimento(LocalDate.now()) // Vence hoje (pois foi pago hoje)
                                .dataPagamento(LocalDate.now()) // Pago hoje
                                .status(StatusConta.PAGO)
                                .tipo(TipoContaPagar.FOLHA_PAGAMENTO)
                                .meioPagamento(MeioPagamento.OUTROS) // Default, pois não sabemos o meio exato na
                                                                     // quitação simples
                                .comissao(comissao) // VINCULAÇÃO IMPORTANTE
                                .build();

                ContaPagar salva = contaPagarRepository.save(conta);
                log.info("✅ Conta a pagar (PAGA) ID {} criada para comissão", salva.getId());
                return salva;
        }

        /**
         * Busca conta a pagar associada a uma comissão.
         */
        public java.util.Optional<ContaPagar> buscarContaPagarPorComissao(
                        com.empresa.comissao.domain.entity.ComissaoCalculada comissao) {
                return contaPagarRepository.findFirstByComissao(comissao);
        }

        /**
         * Cria conta a pagar para distribuição de lucros (dividendos).
         * IMPORTANTE: Sempre cria como PENDENTE. Tipo é fixo e não editável.
         * NÃO afeta DRE, apenas fluxo de caixa.
         */
        @Transactional
        public ContaPagar criarDistribuicaoLucros(
                        Empresa empresa,
                        BigDecimal valor,
                        LocalDate dataCompetencia,
                        LocalDate dataVencimento,
                        String descricao) {

                log.info("💰 Criando distribuição de lucros para empresa: {} - R$ {}",
                                empresa.getNome(), valor);

                if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new BusinessException("Valor da distribuição deve ser maior que zero.");
                }

                String descricaoFinal = descricao != null && !descricao.isBlank()
                                ? descricao
                                : "Distribuição de Lucros - " + dataCompetencia.getMonth() + "/"
                                                + dataCompetencia.getYear();

                ContaPagar conta = ContaPagar.builder()
                                .empresa(empresa)
                                .descricao(descricaoFinal)
                                .valor(valor)
                                .dataCompetencia(dataCompetencia)
                                .dataVencimento(dataVencimento)
                                .status(StatusConta.PENDENTE) // SEMPRE PENDENTE
                                .tipo(TipoContaPagar.DISTRIBUICAO_LUCROS) // TIPO FIXO
                                .build();

                ContaPagar salva = contaPagarRepository.save(conta);
                log.info("✅ Distribuição de lucros ID {} criada. Status: PENDENTE", salva.getId());
                return salva;
        }

        /**
         * Lista distribuições de lucro da empresa.
         */
        public List<ContaPagar> listarDistribuicoesLucro(Empresa empresa) {
                return contaPagarRepository.findByEmpresaAndTipoOrderByDataVencimentoDesc(empresa,
                                TipoContaPagar.DISTRIBUICAO_LUCROS);
        }

        // ========================================
        // PAGAMENTO DE IMPOSTO (DAS)
        // ========================================

        /**
         * Cria conta a pagar para pagamento de imposto (DAS).
         * IMPORTANTE: Sempre cria como PENDENTE. Tipo é fixo IMPOSTO_PAGO.
         * NÃO afeta DRE diretamente (DRE usa imposto calculado), apenas fluxo de caixa.
         */
        @Transactional
        public ContaPagar criarImpostoPago(
                        Empresa empresa,
                        BigDecimal valor,
                        LocalDate dataCompetencia,
                        LocalDate dataVencimento,
                        String descricao) {

                log.info("💰 Criando pagamento de imposto (DAS) para empresa: {} - R$ {}",
                                empresa.getNome(), valor);

                if (valor == null || valor.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new BusinessException("Valor do imposto deve ser maior que zero.");
                }

                String descricaoFinal = descricao != null && !descricao.isBlank()
                                ? descricao
                                : "DAS - Competência " + dataCompetencia.getMonth() + "/" + dataCompetencia.getYear();

                ContaPagar conta = ContaPagar.builder()
                                .empresa(empresa)
                                .descricao(descricaoFinal)
                                .valor(valor)
                                .dataCompetencia(dataCompetencia)
                                .dataVencimento(dataVencimento)
                                .status(StatusConta.PENDENTE) // SEMPRE PENDENTE
                                .tipo(TipoContaPagar.IMPOSTOS) // TIPO FIXO
                                .build();

                ContaPagar salva = contaPagarRepository.save(conta);
                log.info("✅ Pagamento de DAS ID {} criado. Status: PENDENTE", salva.getId());
                return salva;
        }

        /**
         * Lista pagamentos de imposto (DAS) da empresa.
         */
        public List<ContaPagar> listarImpostosPagos(Empresa empresa) {
                return contaPagarRepository.findByEmpresaAndTipoOrderByDataVencimentoDesc(empresa,
                                TipoContaPagar.IMPOSTOS);
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
                                                + (faturamento.getOrdemServico() != null
                                                                ? faturamento.getOrdemServico().getId()
                                                                : "N/A"))
                                .valor(faturamento.getValor())
                                .valorPagoAcumulado(BigDecimal.ZERO)
                                .saldoRestante(faturamento.getValor())
                                .dataCompetencia(faturamento.getDataFaturamento())
                                .dataVencimento(dataVencimento)
                                .tipo(TipoContaReceber.ORDEM_SERVICO)
                                .faturamento(faturamento)
                                .ordemServico(faturamento.getOrdemServico())
                                .cliente(faturamento.getOrdemServico() != null
                                                ? faturamento.getOrdemServico().getCliente()
                                                : null)
                                .funcionarioResponsavel(faturamento.getUsuario())
                                .meioPagamento(meioPagamento);

                if (pagamentoAvista) {
                        builder.status(StatusConta.PAGO)
                                        .dataRecebimento(faturamento.getDataFaturamento())
                                        .valorPagoAcumulado(faturamento.getValor())
                                        .saldoRestante(BigDecimal.ZERO);
                        log.info("💵 Pagamento à vista detectado");
                } else {
                        builder.status(StatusConta.PENDENTE);
                        log.info("📅 Pagamento a prazo - vencimento: {}", dataVencimento);
                }

                ContaReceber conta = builder.build();
                ContaReceber salva = contaReceberRepository.save(conta);

                // Se à vista, criar Recebimento automático
                if (pagamentoAvista) {
                        Recebimento recebimento = Recebimento.builder()
                                        .contaReceber(salva)
                                        .valorPago(faturamento.getValor())
                                        .dataPagamento(faturamento.getDataFaturamento())
                                        .meioPagamento(meioPagamento)
                                        .empresa(faturamento.getEmpresa())
                                        .funcionarioResponsavel(faturamento.getUsuario())
                                        .observacao("Pagamento à vista na finalização da OS")
                                        .build();
                        recebimentoRepository.save(recebimento);
                        log.info("💰 Recebimento automático criado para pagamento à vista");
                }

                log.info("✅ Conta a receber criada com ID: {} - Status: {}", salva.getId(), salva.getStatus());
                return salva;
        }

        /**
         * Marca uma conta a receber como recebida.
         */
        /**
         * Backward-compatible: recebe o valor total restante.
         * Delega para registrarRecebimentoParcial.
         */
        @Transactional
        public ContaReceber receberConta(Long contaId, LocalDate dataRecebimento, MeioPagamento meioPagamento) {
                ContaReceber conta = contaReceberRepository.findById(contaId)
                                .orElseThrow(() -> new BusinessException("Conta a receber não encontrada: " + contaId));
                return registrarRecebimentoParcial(contaId, conta.getSaldoRestante(),
                                dataRecebimento, meioPagamento, null);
        }

        /**
         * Registra um recebimento parcial ou total.
         * Cria registro na tabela recebimentos e atualiza saldo da conta.
         * 
         * @Transactional garante atomicidade.
         */
        @Transactional
        public ContaReceber registrarRecebimentoParcial(Long contaId, BigDecimal valorRecebido,
                        LocalDate dataRecebimento, MeioPagamento meioPagamento, String observacao) {

                ContaReceber conta = contaReceberRepository.findById(contaId)
                                .orElseThrow(() -> new BusinessException("Conta a receber não encontrada: " + contaId));

                if (conta.getStatus() == StatusConta.PAGO || conta.getStatus() == StatusConta.BAIXADO) {
                        throw new BusinessException("Conta já está quitada ou baixada");
                }

                if (valorRecebido == null || valorRecebido.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new BusinessException("Valor do recebimento deve ser maior que zero");
                }

                if (valorRecebido.compareTo(conta.getSaldoRestante()) > 0) {
                        throw new BusinessException("Valor recebido (" + valorRecebido
                                        + ") excede o saldo restante (" + conta.getSaldoRestante() + ")");
                }

                // 1. Criar registro de recebimento
                Recebimento recebimento = Recebimento.builder()
                                .contaReceber(conta)
                                .valorPago(valorRecebido)
                                .dataPagamento(dataRecebimento != null ? dataRecebimento : LocalDate.now())
                                .meioPagamento(meioPagamento)
                                .observacao(observacao)
                                .empresa(conta.getEmpresa())
                                .funcionarioResponsavel(conta.getFuncionarioResponsavel())
                                .build();
                recebimentoRepository.save(recebimento);

                // 2. Atualizar saldo da conta (domain logic)
                conta.registrarRecebimento(valorRecebido);
                if (meioPagamento != null) {
                        conta.setMeioPagamento(meioPagamento);
                }

                ContaReceber salva;
                try {
                        salva = contaReceberRepository.save(conta);
                } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                        throw new BusinessException(
                                        "Conta atualizada por outro usuário. Recarregue e tente novamente.");
                }

                log.info("✅ Recebimento de R$ {} registrado para conta {} | Status: {} | Saldo: {}",
                                valorRecebido, contaId, salva.getStatus(), salva.getSaldoRestante());

                // 3. Invalidar cache de comissão
                invalidarCacheComissao(conta, dataRecebimento != null ? dataRecebimento : LocalDate.now());

                return salva;
        }

        /**
         * Baixa o saldo restante de uma conta (calote/perdão).
         * NÃO cria Recebimento, portanto NÃO entra no cálculo de comissão.
         */
        @Transactional
        public ContaReceber baixarSaldo(Long contaId, String motivo) {
                ContaReceber conta = contaReceberRepository.findById(contaId)
                                .orElseThrow(() -> new BusinessException("Conta a receber não encontrada: " + contaId));

                if (conta.getStatus() == StatusConta.PAGO) {
                        throw new BusinessException("Conta já está totalmente paga");
                }
                if (conta.getStatus() == StatusConta.BAIXADO) {
                        throw new BusinessException("Conta já foi baixada");
                }

                if (motivo == null || motivo.isBlank()) {
                        throw new BusinessException("Motivo da baixa é obrigatório");
                }

                BigDecimal valorBaixado = conta.getSaldoRestante();
                conta.baixarSaldo(motivo);

                ContaReceber salva = contaReceberRepository.save(conta);
                log.info("📝 Conta {} BAIXADA. Valor perdido: R$ {} | Motivo: {}",
                                contaId, valorBaixado, motivo);
                return salva;
        }

        /**
         * Estorna um recebimento: devolve o valor ao saldo da conta.
         */
        @Transactional
        public ContaReceber estornarRecebimento(Long recebimentoId) {
                Recebimento recebimento = recebimentoRepository.findById(recebimentoId)
                                .orElseThrow(() -> new BusinessException(
                                                "Recebimento não encontrado: " + recebimentoId));

                ContaReceber conta = recebimento.getContaReceber();
                BigDecimal valorEstornado = recebimento.getValorPago();

                // Devolver valor ao saldo
                conta.estornarRecebimento(valorEstornado);

                // Deletar o registro de recebimento
                recebimentoRepository.delete(recebimento);
                ContaReceber salva = contaReceberRepository.save(conta);

                log.info("↩️ Estorno de R$ {} na conta {} | Novo status: {} | Novo saldo: {}",
                                valorEstornado, conta.getId(), salva.getStatus(), salva.getSaldoRestante());

                // Invalidar cache de comissão
                invalidarCacheComissao(conta, recebimento.getDataPagamento());

                return salva;
        }

        /**
         * Atualiza a data de vencimento de uma conta (renegociação de prazo).
         */
        @Transactional
        public ContaReceber atualizarVencimento(Long contaId, LocalDate novaDataVencimento) {
                ContaReceber conta = contaReceberRepository.findById(contaId)
                                .orElseThrow(() -> new BusinessException("Conta a receber não encontrada: " + contaId));

                if (conta.getStatus() == StatusConta.PAGO || conta.getStatus() == StatusConta.BAIXADO) {
                        throw new BusinessException("Não é possível renegociar conta já quitada ou baixada");
                }

                if (novaDataVencimento == null) {
                        throw new BusinessException("Nova data de vencimento é obrigatória");
                }

                conta.setDataVencimento(novaDataVencimento);
                ContaReceber salva = contaReceberRepository.save(conta);
                log.info("📅 Vencimento da conta {} atualizado para {}", contaId, novaDataVencimento);
                return salva;
        }

        /**
         * Lista histórico de recebimentos de uma conta.
         */
        public List<Recebimento> listarRecebimentos(Long contaReceberId) {
                return recebimentoRepository.findByContaReceberIdOrderByDataPagamentoAsc(contaReceberId);
        }

        /**
         * Invalida cache de comissão para empresa e funcionário.
         */
        private void invalidarCacheComissao(ContaReceber conta, LocalDate dataReferencia) {
                try {
                        YearMonth mesReferencia = YearMonth.from(dataReferencia);
                        if (conta.getEmpresa() != null) {
                                comissaoService.invalidarCacheEmpresa(conta.getEmpresa(), mesReferencia);
                        }
                        if (conta.getFuncionarioResponsavel() != null) {
                                comissaoService.invalidarCache(conta.getFuncionarioResponsavel(), mesReferencia);
                        }
                } catch (Exception e) {
                        log.warn("⚠️ Falha ao invalidar cache de comissão: {}", e.getMessage());
                }
        }

        /**
         * Lista contas a receber pendentes.
         */
        public List<ContaReceber> listarContasReceberPendentes(Empresa empresa) {
                return contaReceberRepository.findByEmpresaAndStatusOrderByDataVencimentoAsc(empresa,
                                StatusConta.PENDENTE);
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
                // Usa tabela recebimentos: soma real de caixa, não valor do título
                return recebimentoRepository.sumByEmpresaAndDataPagamentoBetween(empresa, inicio, fim);
        }

        /**
         * Obtém o total recebido por funcionário em um período (comissão individual).
         */
        public BigDecimal getTotalRecebidoPorFuncionario(Empresa empresa, User funcionario, YearMonth periodo) {
                LocalDate inicio = periodo.atDay(1);
                LocalDate fim = periodo.atEndOfMonth();
                // Usa tabela recebimentos: comissão individual baseada em caixa real
                return recebimentoRepository.sumByEmpresaAndFuncionarioAndDataPagamentoBetween(
                                empresa, funcionario, inicio, fim);
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
                                                .cliente(conta.getOrdemServico() != null
                                                                && conta.getOrdemServico().getCliente() != null
                                                                                ? conta.getOrdemServico().getCliente()
                                                                                                .getRazaoSocial()
                                                                                : null)
                                                .meioPagamento(conta.getMeioPagamento() != null
                                                                ? conta.getMeioPagamento().name()
                                                                : null)
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
        // RELATÓRIOS CONTÁBEIS (PDF)
        // ========================================

        /**
         * Gera relatório detalhado de Receita por Caixa (DAS).
         */
        public RelatorioReceitaCaixaDTO getRelatorioReceitaCaixaDetalhada(Empresa empresa, YearMonth periodo) {
                LocalDate inicio = periodo.atDay(1);
                LocalDate fim = periodo.atEndOfMonth();

                List<ContaReceber> recebimentos = contaReceberRepository.findRecebidosBetween(empresa, inicio, fim);

                List<RelatorioReceitaCaixaDTO.ItemReceitaDTO> itens = recebimentos.stream()
                                .map(c -> RelatorioReceitaCaixaDTO.ItemReceitaDTO.builder()
                                                .dataRecebimento(c.getDataRecebimento())
                                                .valor(c.getValor())
                                                .origem(gerarOrigemConta(c))
                                                .cliente(c.getOrdemServico() != null
                                                                && c.getOrdemServico().getCliente() != null
                                                                                ? c.getOrdemServico().getCliente()
                                                                                                .getRazaoSocial()
                                                                                : (c.getCliente() != null ? c
                                                                                                .getCliente()
                                                                                                .getRazaoSocial()
                                                                                                : "N/A"))
                                                .meioPagamento(c.getMeioPagamento() != null
                                                                ? c.getMeioPagamento().name()
                                                                : "N/A")
                                                .descricao(c.getDescricao())
                                                .build())
                                .collect(Collectors.toList());

                BigDecimal total = itens.stream()
                                .map(RelatorioReceitaCaixaDTO.ItemReceitaDTO::getValor)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                return RelatorioReceitaCaixaDTO.builder()
                                .dataInicio(inicio)
                                .dataFim(fim)
                                .totalRecebido(total)
                                .itens(itens)
                                .build();
        }

        /**
         * Gera relatório de Fluxo de Caixa Mensal.
         * Inclui Saldo Inicial (Acumulado), Entradas, Saídas e Saldo Final.
         */
        public RelatorioFluxoCaixaDTO getRelatorioFluxoCaixaMensal(Empresa empresa, YearMonth periodo) {
                LocalDate inicio = periodo.atDay(1);
                LocalDate fim = periodo.atEndOfMonth();

                // 1. Calcular Saldo Inicial (Acumulado até o dia anterior ao inicio)
                BigDecimal totalRecebidoAntes = contaReceberRepository.sumByRecebimentoBefore(empresa, inicio);
                BigDecimal totalPagoAntes = contaPagarRepository.sumByPagamentoBefore(empresa, inicio);
                BigDecimal saldoInicial = totalRecebidoAntes.subtract(totalPagoAntes);

                // 2. Movimentações do Mês
                List<ContaReceber> entradasList = contaReceberRepository.findRecebidosBetween(empresa, inicio, fim);
                List<ContaPagar> saidasList = contaPagarRepository.findPagasBetween(empresa, inicio, fim);

                List<RelatorioFluxoCaixaDTO.ItemFluxoDTO> entradasDTO = entradasList.stream()
                                .map(c -> RelatorioFluxoCaixaDTO.ItemFluxoDTO.builder()
                                                .data(c.getDataRecebimento())
                                                .descricao(c.getDescricao())
                                                .categoria(c.getTipo().name())
                                                .valor(c.getValor())
                                                .build())
                                .collect(Collectors.toList());

                List<RelatorioFluxoCaixaDTO.ItemFluxoDTO> saidasDTO = saidasList.stream()
                                .map(c -> RelatorioFluxoCaixaDTO.ItemFluxoDTO.builder()
                                                .data(c.getDataPagamento())
                                                .descricao(c.getDescricao())
                                                .categoria(c.getTipo().name()) // Uses TipoContaPagar (OPERACIONAL, etc)
                                                .valor(c.getValor())
                                                .build())
                                .collect(Collectors.toList());

                BigDecimal totalEntradas = entradasDTO.stream()
                                .map(RelatorioFluxoCaixaDTO.ItemFluxoDTO::getValor)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalSaidas = saidasDTO.stream()
                                .map(RelatorioFluxoCaixaDTO.ItemFluxoDTO::getValor)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal saldoFinal = saldoInicial.add(totalEntradas).subtract(totalSaidas);

                return RelatorioFluxoCaixaDTO.builder()
                                .periodo(periodo.getMonth().name() + "/" + periodo.getYear())
                                .saldoInicial(saldoInicial)
                                .totalEntradas(totalEntradas)
                                .totalSaidas(totalSaidas)
                                .saldoFinal(saldoFinal)
                                .entradas(entradasDTO)
                                .saidas(saidasDTO)
                                .build();
        }

        /**
         * Gera relatório de Contas a Pagar (Pagas e Pendentes).
         * INCLUI:
         * 1. Contas com vencimento no mês (Pendentes ou Pagas)
         * 2. Contas pagas no mês (mesmo que vencimento seja anterior) - para auditoria
         * de caixa
         */
        public RelatorioContasPagarDTO getRelatorioContasPagar(Empresa empresa, YearMonth periodo) {
                LocalDate inicio = periodo.atDay(1);
                LocalDate fim = periodo.atEndOfMonth();

                // 1. Vencimentos no período (Standard view)
                List<ContaPagar> porVencimento = contaPagarRepository.findByVencimentoBetween(empresa, inicio, fim);

                // 2. Pagamentos no período (Cash view - captura pagamentos atrasados feitos
                // agora)
                List<ContaPagar> porPagamento = contaPagarRepository.findPagasBetween(empresa, inicio, fim);

                // 3. Merge lists removing duplicates
                java.util.Set<ContaPagar> merged = new java.util.HashSet<>();
                merged.addAll(porVencimento);
                merged.addAll(porPagamento);

                // Sort by Vencimento
                List<ContaPagar> sortedList = new ArrayList<>(merged);
                sortedList.sort((c1, c2) -> c1.getDataVencimento().compareTo(c2.getDataVencimento()));

                List<RelatorioContasPagarDTO.ItemContaPagarDTO> itens = sortedList.stream()
                                .map(c -> RelatorioContasPagarDTO.ItemContaPagarDTO.builder()
                                                .dataVencimento(c.getDataVencimento())
                                                .dataPagamento(c.getDataPagamento())
                                                .valor(c.getValor())
                                                .descricao(c.getDescricao())
                                                .fornecedor(
                                                                c.getFuncionario() != null
                                                                                ? c.getFuncionario().getUsername()
                                                                                : "Fornecedor/Terceiro")
                                                .tipo(c.getTipo().name())
                                                .status(c.getStatus().name())
                                                .build())
                                .collect(Collectors.toList());

                BigDecimal totalPago = itens.stream()
                                .filter(i -> "PAGO".equals(i.getStatus()))
                                .map(RelatorioContasPagarDTO.ItemContaPagarDTO::getValor)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Pendente (apenas as que realmente estão pendentes na lista atual)
                // Note: If a bill was due last month and is still pending, it WON'T show here
                // unless we scan ALL open bills.
                // However, usually a "Monthly" report shows what happened in that month.
                // "O que ficou em aberto" - implying currently open bills causing debt?
                // For now, let's keep it to the month scope + payments made.
                BigDecimal totalPendente = itens.stream()
                                .filter(i -> "PENDENTE".equals(i.getStatus()))
                                .map(RelatorioContasPagarDTO.ItemContaPagarDTO::getValor)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Vencido = Pendente e Data Vencimento < Hoje
                BigDecimal totalVencido = itens.stream()
                                .filter(i -> "PENDENTE".equals(i.getStatus())
                                                && i.getDataVencimento().isBefore(LocalDate.now()))
                                .map(RelatorioContasPagarDTO.ItemContaPagarDTO::getValor)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                return RelatorioContasPagarDTO.builder()
                                .periodo(periodo.toString())
                                .totalPago(totalPago)
                                .totalPendente(totalPendente)
                                .totalVencido(totalVencido)
                                .itens(itens)
                                .build();
        }

        /**
         * Gera relatório de Distribuição de Lucros.
         */
        public RelatorioDistribuicaoLucrosDTO getRelatorioDistribuicaoLucros(Empresa empresa, YearMonth periodo) {
                LocalDate inicio = periodo.atDay(1);
                LocalDate fim = periodo.atEndOfMonth();
                LocalDate inicioAno = LocalDate.of(periodo.getYear(), 1, 1);

                // 1. Distribuições do Mês
                List<ContaPagar> distribuicoesMes = contaPagarRepository
                                .findByEmpresaAndTipoAndDataVencimentoBetweenOrderByDataVencimentoDesc(
                                                empresa, TipoContaPagar.DISTRIBUICAO_LUCROS, inicio, fim);

                List<RelatorioDistribuicaoLucrosDTO.ItemDistribuicaoDTO> itens = distribuicoesMes.stream()
                                .map(c -> RelatorioDistribuicaoLucrosDTO.ItemDistribuicaoDTO.builder()
                                                .data(c.getDataVencimento())
                                                .valor(c.getValor())
                                                .socio(c.getFuncionario() != null ? c.getFuncionario().getUsername()
                                                                : "Sócio")
                                                .descricao(c.getDescricao())
                                                .build())
                                .collect(Collectors.toList());

                BigDecimal totalMes = itens.stream()
                                .map(RelatorioDistribuicaoLucrosDTO.ItemDistribuicaoDTO::getValor)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // 2. Acumulado Ano
                BigDecimal totalAcumulado = contaPagarRepository.sumByTipoAndVencimentoBetween(
                                empresa, TipoContaPagar.DISTRIBUICAO_LUCROS, inicioAno, fim);

                return RelatorioDistribuicaoLucrosDTO.builder()
                                .periodo(periodo.toString())
                                .totalMes(totalMes)
                                .totalAcumuladoAno(totalAcumulado)
                                .distribuicoes(itens)
                                .build();
        }

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

        /**
         * Busca faturamento por ID com grafo completo (otimizado).
         * Evita N+1 queries ao carregar detalhes da OS, veículos e peças.
         */
        @Transactional(readOnly = true)
        public java.util.Optional<Faturamento> getFaturamentoDetalhado(Long id) {
                log.info("[FETCH_PLAN] faturamento-details joinfetch ON");
                return faturamentoRepository.findByIdComGrafoCompleto(id);
        }
}
