package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.ComissaoCalculada;
import com.empresa.comissao.domain.entity.Despesa;
import com.empresa.comissao.domain.entity.Faturamento;
import com.empresa.comissao.domain.entity.PagamentoAdiantado;
import com.empresa.comissao.domain.enums.CategoriaDespesa;
import com.empresa.comissao.dto.RelatorioFinanceiroDTO;
import com.empresa.comissao.dto.ComparacaoFaturamentoDTO;
import com.empresa.comissao.dto.MesFaturamentoDTO;
import com.empresa.comissao.dto.RelatorioAnualDTO;
import com.empresa.comissao.repository.ComissaoCalculadaRepository;
import com.empresa.comissao.repository.DespesaRepository;
import com.empresa.comissao.repository.FaturamentoRepository;
import com.empresa.comissao.repository.PagamentoAdiantadoRepository;
import com.empresa.comissao.repository.RegraComissaoRepository;
import com.empresa.comissao.domain.entity.RegraComissao;
import com.empresa.comissao.domain.entity.FaixaComissaoConfig;
import com.empresa.comissao.domain.enums.TipoRegraComissao;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComissaoService {

        private final FaturamentoRepository faturamentoRepository;
        private final PagamentoAdiantadoRepository pagamentoAdiantadoRepository;
        private final ComissaoCalculadaRepository comissaoCalculadaRepository;
        private final DespesaRepository despesaRepository;
        private final com.empresa.comissao.repository.OrdemServicoRepository ordemServicoRepository;
        private final RegraComissaoRepository regraComissaoRepository;
        private final com.empresa.comissao.repository.ContaReceberRepository contaReceberRepository;
        private final com.empresa.comissao.repository.RecebimentoRepository recebimentoRepository;
        private final com.empresa.comissao.repository.UserRepository userRepository;
        private final com.empresa.comissao.repository.ContaPagarRepository contaPagarRepository;

        @org.springframework.beans.factory.annotation.Autowired
        @org.springframework.context.annotation.Lazy
        private FinanceiroService financeiroService;

        @Transactional
        public ComissaoCalculada calcularEObterComissaoMensal(int ano, int mes,
                        com.empresa.comissao.domain.entity.User usuario) {
                return calcularEObterComissaoMensal(ano, mes, usuario, false);
        }

        @Transactional
        public ComissaoCalculada calcularEObterComissaoMensal(int ano, int mes,
                        com.empresa.comissao.domain.entity.User usuario, boolean force) {
                YearMonth anoMesReferencia = YearMonth.of(ano, mes);

                log.info("🔍 Buscando comissão para: {}/{} - Usuário: {}", ano, mes,
                                usuario != null ? usuario.getUsername() : "GLOBAL");

                // ⛔ Opt-Out Check: Users who don't participate in commission should be skipped
                if (usuario != null && !usuario.isParticipaComissao()) {
                        log.info("🚫 Usuário {} não participa de comissão (participaComissao=false).",
                                        usuario.getUsername());
                        throw new com.empresa.comissao.exception.BusinessException(
                                        "Usuário não configurado para receber comissões.");
                }

                // ⛔ SAFETY: SaaS/Platform Admins (without Empresa) CANNOT generate commission
                // data
                if (usuario != null && usuario.getEmpresa() == null) {
                        throw new com.empresa.comissao.exception.BusinessException(
                                        "Administradores de sistema (SaaS/Plataforma) não possuem faturamento ou comissão calculada.");
                }

                // 1. Verificar se a comissão já foi calculada e persistida para este
                // mês/usuário
                Optional<ComissaoCalculada> comissaoExistente = comissaoCalculadaRepository
                                .findFirstByAnoMesReferenciaAndUsuario(anoMesReferencia, usuario);

                if (comissaoExistente.isPresent()) {
                        if (force) {
                                log.info("🔄 Atualizando valores da comissão existente: {}",
                                                comissaoExistente.get().getId());
                                // We will update the existing instance instead of deleting it
                        } else {
                                log.info("✅ Comissão encontrada em cache: {} - Valor: {} - Quitado: {}",
                                                anoMesReferencia,
                                                comissaoExistente.get().getSaldoAReceber(),
                                                comissaoExistente.get().getQuitado());
                                return comissaoExistente.get();
                        }
                }

                log.info("📊 Comissão não encontrada. Calculando...\n");

                // 2a. Buscar saldo do mês anterior (CARRYOVER)
                YearMonth mesAnterior = anoMesReferencia.minusMonths(1);
                BigDecimal saldoAnterior = BigDecimal.ZERO;
                Optional<ComissaoCalculada> comissaoMesAnterior = comissaoCalculadaRepository
                                .findFirstByAnoMesReferenciaAndUsuario(mesAnterior, usuario);
                if (comissaoMesAnterior.isPresent()) {
                        BigDecimal saldoMesAnterior = comissaoMesAnterior.get().getSaldoAReceber();
                        // Se o saldo do mês anterior é negativo, transferimos como dívida
                        if (saldoMesAnterior.compareTo(BigDecimal.ZERO) < 0) {
                                saldoAnterior = saldoMesAnterior; // Valor negativo (dívida)
                                log.info("⚠️ Saldo anterior negativo (carryover): {}", saldoAnterior);
                        }
                }

                // 2. Somar o RECEBIDO total do mês (ContaReceber.PAGO - base para comissão)
                // MUDANÇA CRÍTICA: Comissão agora é baseada em CAIXA, não COMPETÊNCIA
                LocalDate inicioDoMes = anoMesReferencia.atDay(1);
                LocalDate fimDoMes = anoMesReferencia.atEndOfMonth();

                BigDecimal faturamentoMensalTotal;
                if (usuario != null && usuario.getEmpresa() != null) {
                        // Verificar o MODO de comissão da empresa
                        com.empresa.comissao.domain.enums.ModoComissao modo = usuario.getEmpresa().getModoComissao();

                        if (modo == com.empresa.comissao.domain.enums.ModoComissao.COLETIVA) {
                                // Modo COLETIVA: Soma real de caixa da EMPRESA inteira (Recebimentos)
                                faturamentoMensalTotal = recebimentoRepository
                                                .sumByEmpresaAndDataPagamentoBetween(usuario.getEmpresa(), inicioDoMes,
                                                                fimDoMes);
                                log.info("💰 Faturamento Base (COLETIVA - Recebimentos): {}", faturamentoMensalTotal);
                        } else {
                                // Modo INDIVIDUAL: Soma recebimentos do funcionário
                                faturamentoMensalTotal = recebimentoRepository
                                                .sumByEmpresaAndFuncionarioAndDataPagamentoBetween(
                                                                usuario.getEmpresa(), usuario, inicioDoMes, fimDoMes);
                                log.info("💰 Faturamento Base (INDIVIDUAL - Recebimentos): {}", faturamentoMensalTotal);
                        }
                } else {
                        // Fallback: usar faturamento tradicional se não houver empresa
                        faturamentoMensalTotal = faturamentoRepository
                                        .sumValorByDataFaturamentoBetween(inicioDoMes, fimDoMes)
                                        .orElse(BigDecimal.ZERO);
                        log.info("💰 Faturamento (fallback): {}", faturamentoMensalTotal);
                }

                log.info("💵 Base para comissão (recebido): {}", faturamentoMensalTotal);

                // 3. Somar os adiantamentos totais do mês
                BigDecimal valorTotalAdiantamentos;
                if (usuario != null) {
                        valorTotalAdiantamentos = pagamentoAdiantadoRepository
                                        .sumValorByDataPagamentoBetweenAndUsuario(inicioDoMes, fimDoMes, usuario)
                                        .orElse(BigDecimal.ZERO);
                } else {
                        valorTotalAdiantamentos = pagamentoAdiantadoRepository
                                        .sumValorByDataPagamentoBetween(inicioDoMes, fimDoMes)
                                        .orElse(BigDecimal.ZERO);
                }

                log.info("💸 Adiantamentos total: {}", valorTotalAdiantamentos);

                // 4. Determinar a faixa e calcular comissão
                BigDecimal percentualAplicado = BigDecimal.ZERO;
                String faixaDescricao = "Sem comissão definida";

                boolean regraEncontrada = false;

                // Tentar Regra Dinâmica (Prioridade)
                if (usuario != null && usuario.getEmpresa() != null) {
                        Optional<RegraComissao> regraOpt = regraComissaoRepository
                                        .findActiveWithFaixasByEmpresa(usuario.getEmpresa());

                        if (regraOpt.isPresent()) {
                                RegraComissao regra = regraOpt.get();
                                regraEncontrada = true;
                                log.info("📏 Regra dinâmica aplicada: {}", regra.getNome());

                                if (regra.getTipoRegra() == TipoRegraComissao.FIXA_EMPRESA) {
                                        percentualAplicado = regra.getPercentualFixo() != null
                                                        ? regra.getPercentualFixo().divide(new BigDecimal("100"), 4,
                                                                        RoundingMode.HALF_UP)
                                                        : BigDecimal.ZERO;
                                        faixaDescricao = "Fixa: " + regra.getNome();
                                } else {
                                        // Percorrer faixas
                                        if (regra.getFaixas() != null) {
                                                for (FaixaComissaoConfig fc : regra.getFaixas()) {
                                                        boolean maiorIgualMin = faturamentoMensalTotal
                                                                        .compareTo(fc.getMinFaturamento()) >= 0;
                                                        boolean menorIgualMax = fc.getMaxFaturamento() == null
                                                                        || faturamentoMensalTotal.compareTo(
                                                                                        fc.getMaxFaturamento()) <= 0;

                                                        if (maiorIgualMin && menorIgualMax) {
                                                                percentualAplicado = fc.getPorcentagem() != null
                                                                                ? fc.getPorcentagem().divide(
                                                                                                new BigDecimal("100"),
                                                                                                4, RoundingMode.HALF_UP)
                                                                                : BigDecimal.ZERO;
                                                                // Fallback: generate description if null
                                                                String minStr = String.format("%,.2f",
                                                                                fc.getMinFaturamento());
                                                                String maxStr = fc.getMaxFaturamento() != null
                                                                                ? String.format("%,.2f",
                                                                                                fc.getMaxFaturamento())
                                                                                : "∞";
                                                                String rangeStr = "R$ " + minStr + " até R$ " + maxStr;

                                                                log.info("🔍 [DEBUG FAIXA] Calculando Faixa: Min={}, Max={}, Nome={}",
                                                                                minStr, maxStr, fc.getDescricao());

                                                                if (fc.getDescricao() != null
                                                                                && !fc.getDescricao().isBlank()) {
                                                                        faixaDescricao = rangeStr + " ("
                                                                                        + fc.getDescricao() + ")";
                                                                } else {
                                                                        faixaDescricao = rangeStr;
                                                                }
                                                                log.info("✅ [DEBUG FAIXA] Descrição Final Gerada: {}",
                                                                                faixaDescricao);
                                                                break;
                                                        }
                                                }
                                        }
                                }
                        }
                }

                // Fallback Legacy (Tabela Estática)
                if (!regraEncontrada) {
                        log.warn("⚠️ Nenhuma regra de comissão ativa encontrada para usuário {}",
                                        usuario != null ? usuario.getUsername() : "N/A");
                        percentualAplicado = BigDecimal.ZERO;
                        faixaDescricao = "Nenhuma regra de comissão configurada";
                }

                log.info("📈 Percentual aplicado: {} ({})", percentualAplicado, faixaDescricao);

                // 5. Calcular o valor bruto da comissão
                // 5. Calcular o valor bruto da comissão
                BigDecimal valorBrutoComissao = faturamentoMensalTotal.multiply(percentualAplicado)
                                .setScale(2, RoundingMode.HALF_UP);

                log.info("💵 Valor bruto da comissão: {}", valorBrutoComissao);

                // 6. Somar pagamentos já realizados (valor_quitado vindo de ContaPagar)
                BigDecimal valorQuitado = BigDecimal.ZERO;
                if (comissaoExistente.isPresent()) {
                        valorQuitado = contaPagarRepository.sumPaidByComissao(comissaoExistente.get());
                }

                log.info("💳 Pagamentos já realizados (valor_quitado): {}", valorQuitado);

                // 7. Calcular o saldo a receber (incluindo carryover e abatendo o que já foi
                // pago)
                // Fórmula: (valorBruto - adiantamentos + saldoAnterior) - valorQuitado
                BigDecimal saldoAReceber = valorBrutoComissao.subtract(valorTotalAdiantamentos)
                                .add(saldoAnterior)
                                .subtract(valorQuitado)
                                .setScale(2, RoundingMode.HALF_UP);

                log.info("✅ Saldo a receber (com carryover e pagamentos): {}", saldoAReceber);

                // 8. Criar ou Atualizar o objeto ComissaoCalculada
                ComissaoCalculada comissao = comissaoExistente.orElseGet(() -> ComissaoCalculada.builder()
                                .anoMesReferencia(anoMesReferencia)
                                .usuario(usuario)
                                .empresa(usuario != null ? usuario.getEmpresa() : null)
                                .build());

                comissao.setFaturamentoMensalTotal(faturamentoMensalTotal);
                comissao.setFaixaComissaoDescricao(faixaDescricao);
                comissao.setPorcentagemComissaoAplicada(percentualAplicado.multiply(new BigDecimal("100")));
                comissao.setValorBrutoComissao(valorBrutoComissao);
                comissao.setValorTotalAdiantamentos(valorTotalAdiantamentos);
                comissao.setValorQuitado(valorQuitado);
                comissao.setSaldoAReceber(saldoAReceber);
                comissao.setSaldoAnterior(saldoAnterior);

                // Status Auto-Update: Se saldo zerado, marca como quitado. Se saldo positivo,
                // reabre.
                comissao.setQuitado(saldoAReceber.compareTo(BigDecimal.ZERO) <= 0
                                && valorQuitado.compareTo(BigDecimal.ZERO) > 0);
                if (comissao.getQuitado()) {
                        comissao.setDataQuitacao(java.time.LocalDateTime.now());
                }

                ComissaoCalculada salva = comissaoCalculadaRepository.save(comissao);
                log.info("💾 Comissão salva/atualizada com ID: {}", salva.getId());

                return salva;
        }

        // Overload for Global Report (backward compatibility)
        @Transactional
        public ComissaoCalculada calcularEObterComissaoMensal(int ano, int mes) {
                return calcularEObterComissaoMensal(ano, mes, null);
        }

        /**
         * Calculate company-wide commission for ADMIN_EMPRESA role.
         * This aggregates all faturamento/adiantamento for the entire empresa.
         */
        @Transactional
        public ComissaoCalculada calcularComissaoEmpresaMensal(int ano, int mes,
                        com.empresa.comissao.domain.entity.Empresa empresa) {
                return calcularComissaoEmpresaMensal(ano, mes, empresa, false);
        }

        @Transactional
        public ComissaoCalculada calcularComissaoEmpresaMensal(int ano, int mes,
                        com.empresa.comissao.domain.entity.Empresa empresa, boolean force) {
                YearMonth anoMesReferencia = YearMonth.of(ano, mes);

                log.info("🏢 Buscando comissão EMPRESA para: {}/{} - Empresa: {}", ano, mes,
                                empresa != null ? empresa.getNome() : "GLOBAL");

                if (empresa == null) {
                        throw new com.empresa.comissao.exception.BusinessException(
                                        "Empresa é obrigatória para cálculo de comissão consolidada.");
                }

                // 1. Check cache by empresa (not user)
                Optional<ComissaoCalculada> comissaoExistente = comissaoCalculadaRepository
                                .findFirstByAnoMesReferenciaAndEmpresaAndUsuarioIsNull(anoMesReferencia, empresa);

                if (comissaoExistente.isPresent()) {
                        if (force) {
                                log.info("🔄 Atualizando valores da comissão EMPRESA existente: {}",
                                                comissaoExistente.get().getId());
                        } else {
                                log.info("✅ Comissão empresa encontrada em cache: {}", anoMesReferencia);
                                return comissaoExistente.get();
                        }
                }

                log.info("📊 Comissão empresa não encontrada. Calculando...");

                // 2a. Buscar saldo do mês anterior (CARRYOVER) para empresa
                YearMonth mesAnterior = anoMesReferencia.minusMonths(1);
                BigDecimal saldoAnterior = BigDecimal.ZERO;
                Optional<ComissaoCalculada> comissaoMesAnterior = comissaoCalculadaRepository
                                .findFirstByAnoMesReferenciaAndEmpresaAndUsuarioIsNull(mesAnterior, empresa);
                if (comissaoMesAnterior.isPresent()) {
                        BigDecimal saldoMesAnterior = comissaoMesAnterior.get().getSaldoAReceber();
                        // Se o saldo do mês anterior é negativo, transferimos como dívida
                        if (saldoMesAnterior.compareTo(BigDecimal.ZERO) < 0) {
                                saldoAnterior = saldoMesAnterior; // Valor negativo (dívida)
                                log.info("⚠️ Saldo anterior negativo (carryover empresa): {}", saldoAnterior);
                        }
                }

                // 2. Somar o RECEBIDO total do mês para a empresa (Recebimentos reais)
                // CORREÇÃO CRÍTICA: Usa tabela recebimentos, não contas_receber
                LocalDate inicioDoMes = anoMesReferencia.atDay(1);
                LocalDate fimDoMes = anoMesReferencia.atEndOfMonth();

                BigDecimal faturamentoMensalTotal = recebimentoRepository
                                .sumByEmpresaAndDataPagamentoBetween(empresa, inicioDoMes, fimDoMes);

                log.info("💰 Recebido total da empresa (Recebimentos reais): {}", faturamentoMensalTotal);

                // 3. Sum all adiantamentos for the empresa
                BigDecimal valorTotalAdiantamentos = pagamentoAdiantadoRepository
                                .sumValorByDataPagamentoBetweenAndEmpresa(inicioDoMes, fimDoMes, empresa)
                                .orElse(BigDecimal.ZERO);

                log.info("💸 Adiantamentos total da empresa: {}", valorTotalAdiantamentos);

                // 4. Calculate commission
                BigDecimal percentualAplicado = BigDecimal.ZERO;
                String faixaDescricao = "Sem comissão definida";
                boolean regraEncontrada = false;

                // Try Dynamic Rule
                Optional<RegraComissao> regraOpt = regraComissaoRepository.findActiveWithFaixasByEmpresa(empresa);
                if (regraOpt.isPresent()) {
                        RegraComissao regra = regraOpt.get();
                        regraEncontrada = true;
                        if (regra.getTipoRegra() == TipoRegraComissao.FIXA_EMPRESA) {
                                percentualAplicado = regra.getPercentualFixo() != null
                                                ? regra.getPercentualFixo().divide(new BigDecimal("100"), 4,
                                                                RoundingMode.HALF_UP)
                                                : BigDecimal.ZERO;
                                faixaDescricao = "Fixa: " + regra.getNome();
                        } else {
                                if (regra.getFaixas() != null) {
                                        for (FaixaComissaoConfig fc : regra.getFaixas()) {
                                                boolean maiorIgualMin = faturamentoMensalTotal
                                                                .compareTo(fc.getMinFaturamento()) >= 0;
                                                boolean menorIgualMax = fc.getMaxFaturamento() == null
                                                                || faturamentoMensalTotal
                                                                                .compareTo(fc.getMaxFaturamento()) <= 0;
                                                if (maiorIgualMin && menorIgualMax) {
                                                        percentualAplicado = fc.getPorcentagem() != null
                                                                        ? fc.getPorcentagem().divide(
                                                                                        new BigDecimal("100"), 4,
                                                                                        RoundingMode.HALF_UP)
                                                                        : BigDecimal.ZERO;
                                                        // Fallback: generate description if null
                                                        String minStr = String.format("%,.2f", fc.getMinFaturamento());
                                                        String maxStr = fc.getMaxFaturamento() != null
                                                                        ? String.format("%,.2f", fc.getMaxFaturamento())
                                                                        : "∞";
                                                        String rangeStr = "R$ " + minStr + " até R$ " + maxStr;

                                                        log.info("🔍 [DEBUG FAIXA EMPRESA] Calculando: Min={}, Max={}, Nome={}",
                                                                        minStr, maxStr, fc.getDescricao());

                                                        if (fc.getDescricao() != null && !fc.getDescricao().isBlank()) {
                                                                faixaDescricao = rangeStr + " (" + fc.getDescricao()
                                                                                + ")";
                                                        } else {
                                                                faixaDescricao = rangeStr;
                                                        }
                                                        log.info("✅ [DEBUG FAIXA EMPRESA] Descrição Final: {}",
                                                                        faixaDescricao);
                                                        break;
                                                }
                                        }
                                }
                        }
                }

                // Fallback Legacy
                if (!regraEncontrada) {
                        log.warn("⚠️ Nenhuma regra de comissão ativa encontrada para empresa {}",
                                        empresa.getNome());
                        percentualAplicado = BigDecimal.ZERO;
                        faixaDescricao = "Nenhuma regra de comissão configurada";
                }

                log.info("📈 Faixa encontrada: {} - {}%", faixaDescricao, percentualAplicado);

                // 5. Calcular o valor bruto da comissão
                BigDecimal valorBrutoComissao = faturamentoMensalTotal.multiply(percentualAplicado)
                                .setScale(2, RoundingMode.HALF_UP);

                log.info("💵 Valor bruto da empresa: {}", valorBrutoComissao);

                // 6. Somar pagamentos já realizados (da empresa consolidada)
                BigDecimal valorQuitado = BigDecimal.ZERO;
                if (comissaoExistente.isPresent()) {
                        valorQuitado = contaPagarRepository.sumPaidByComissao(comissaoExistente.get());
                }

                log.info("💳 Pagamentos empresa já realizados: {}", valorQuitado);

                // 7. Calcular o saldo a receber consolidado
                BigDecimal saldoAReceber = valorBrutoComissao.subtract(valorTotalAdiantamentos)
                                .add(saldoAnterior)
                                .subtract(valorQuitado)
                                .setScale(2, RoundingMode.HALF_UP);

                log.info("✅ Saldo a receber empresa (com carryover e pagamentos): {}", saldoAReceber);

                // 8. Atualizar ou Criar
                ComissaoCalculada comissao = comissaoExistente.orElseGet(() -> ComissaoCalculada.builder()
                                .anoMesReferencia(anoMesReferencia)
                                .usuario(null)
                                .empresa(empresa)
                                .build());

                comissao.setFaturamentoMensalTotal(faturamentoMensalTotal);
                comissao.setFaixaComissaoDescricao(faixaDescricao);
                comissao.setPorcentagemComissaoAplicada(percentualAplicado.multiply(new BigDecimal("100")));
                comissao.setValorBrutoComissao(valorBrutoComissao);
                comissao.setValorTotalAdiantamentos(valorTotalAdiantamentos);
                comissao.setValorQuitado(valorQuitado);
                comissao.setSaldoAReceber(saldoAReceber);
                comissao.setSaldoAnterior(saldoAnterior);

                comissao.setQuitado(saldoAReceber.compareTo(BigDecimal.ZERO) <= 0
                                && valorQuitado.compareTo(BigDecimal.ZERO) > 0);
                if (comissao.getQuitado()) {
                        comissao.setDataQuitacao(java.time.LocalDateTime.now());
                }

                ComissaoCalculada salva = comissaoCalculadaRepository.save(comissao);
                log.info("💾 Comissão empresa salva/atualizada com ID: {}", salva.getId());

                return salva;
        }

        /**
         * Marca uma comissão como quitada (paga).
         * Registra a data de quitação para auditoria.
         */
        @Transactional
        public void quitarComissao(Long comissaoId) {
                log.info("💸 Quitando comissão ID: {}", comissaoId);

                ComissaoCalculada comissao = comissaoCalculadaRepository.findById(comissaoId)
                                .orElseThrow(() -> new com.empresa.comissao.exception.BusinessException(
                                                "Comissão não encontrada com ID: " + comissaoId));

                // 1. Recalcular para garantir que o saldo está atualizado antes de quitar
                comissao = calcularEObterComissaoMensal(
                                comissao.getAnoMesReferencia().getYear(),
                                comissao.getAnoMesReferencia().getMonthValue(),
                                comissao.getUsuario(),
                                true);

                BigDecimal saldo = comissao.getSaldoAReceber();

                if (saldo == null || saldo.compareTo(BigDecimal.ZERO) <= 0) {
                        throw new com.empresa.comissao.exception.BusinessException(
                                        "Esta comissão já está totalmente quitada ou não possui saldo.");
                }

                log.info("💰 Saldo atual a quitar: {}", saldo);

                // 2. IDEMPOTÊNCIA: Verificar se já existe uma conta PENDENTE para esta comissão
                try {
                        java.util.List<com.empresa.comissao.domain.entity.ContaPagar> contasPendentes = financeiroService
                                        .listarContasPendentesPorComissao(comissao);

                        if (!contasPendentes.isEmpty()) {
                                com.empresa.comissao.domain.entity.ContaPagar contaPendente = contasPendentes.get(0);
                                log.info("🔄 Conta PENDENTE encontrada (ID: {}). Atualizando valor para {} e quitando...",
                                                contaPendente.getId(), saldo);

                                // Atualizar valor da conta pendente para o saldo atual (evita duplicidade de
                                // cliques)
                                contaPendente.setValor(saldo);
                                financeiroService.salvarContaPagar(contaPendente);

                                financeiroService.pagarConta(contaPendente.getId(), LocalDate.now(),
                                                com.empresa.comissao.domain.enums.MeioPagamento.OUTROS);
                        } else {
                                log.info("🆕 Nenhuma conta pendente. Criando nova quitação para o saldo: {}", saldo);
                                financeiroService.criarContaPagarComissaoQuitada(comissao,
                                                comissao.getUsuario() != null ? comissao.getUsuario().getEmpresa()
                                                                : comissao.getEmpresa(),
                                                saldo);
                        }
                } catch (Exception e) {
                        log.error("❌ Erro ao processar quitação financeira: {}", e.getMessage());
                        throw new com.empresa.comissao.exception.BusinessException(
                                        "Erro ao processar quitação financeira: " + e.getMessage());
                }

                // 3. Atualizar cache final
                if (comissao.getUsuario() != null) {
                        invalidarCache(comissao.getUsuario(), comissao.getAnoMesReferencia());
                } else if (comissao.getEmpresa() != null) {
                        invalidarCacheEmpresa(comissao.getEmpresa(), comissao.getAnoMesReferencia());
                }

                log.info("✅ Comissão {} processada para quitação com sucesso.", comissaoId);
        }

        /**
         * Gera uma conta a pagar (PENDENTE) para o pagamento da comissão.
         */
        @Transactional
        public com.empresa.comissao.domain.entity.ContaPagar gerarPagamentoComissao(Long comissaoId,
                        LocalDate dataVencimento) {
                log.info("💸 Gerando conta a pagar para comissão ID: {}", comissaoId);

                ComissaoCalculada comissao = comissaoCalculadaRepository.findById(comissaoId)
                                .orElseThrow(() -> new com.empresa.comissao.exception.BusinessException(
                                                "Comissão não encontrada com ID: " + comissaoId));

                if (comissao.getSaldoAReceber().compareTo(BigDecimal.ZERO) <= 0) {
                        throw new com.empresa.comissao.exception.BusinessException(
                                        "Comissão não possui saldo positivo a pagar.");
                }

                // IDEMPOTÊNCIA: Reutilizar conta pendente se já existir
                java.util.List<com.empresa.comissao.domain.entity.ContaPagar> pendentes = financeiroService
                                .listarContasPendentesPorComissao(comissao);

                if (!pendentes.isEmpty()) {
                        com.empresa.comissao.domain.entity.ContaPagar existente = pendentes.get(0);
                        log.info("🔄 Reutilizando conta pendente ID: {} (Valor antigo: {} -> Novo: {})",
                                        existente.getId(), existente.getValor(), comissao.getSaldoAReceber());
                        existente.setValor(comissao.getSaldoAReceber());
                        existente.setDataVencimento(dataVencimento);
                        return financeiroService.salvarContaPagar(existente);
                }

                return financeiroService.criarContaPagarComissao(
                                comissao,
                                comissao.getUsuario() != null ? comissao.getUsuario().getEmpresa()
                                                : comissao.getEmpresa(),
                                dataVencimento);
        }

        @Transactional
        public void invalidarCache(com.empresa.comissao.domain.entity.User usuario, YearMonth anoMes) {
                log.info("🔄 Atualizando cache de comissão para Usuário: {} - Mês: {}",
                                usuario != null ? usuario.getEmail() : "GLOBAL", anoMes);

                // Instead of deleting, we trigger a recalculation which will update the record
                try {
                        calcularEObterComissaoMensal(anoMes.getYear(), anoMes.getMonthValue(), usuario, true);
                } catch (Exception e) {
                        log.warn("⚠️ Não foi possível atualizar cache de comissão: {}", e.getMessage());
                }
        }

        @Transactional
        public void invalidarCacheEmpresa(com.empresa.comissao.domain.entity.Empresa empresa, YearMonth anoMes) {
                if (empresa == null)
                        return;
                log.info("🔄 Atualizando cache de comissão para Empresa: {} - Mês: {}", empresa.getNome(), anoMes);

                try {
                        calcularComissaoEmpresaMensal(anoMes.getYear(), anoMes.getMonthValue(), empresa, true);
                } catch (Exception e) {
                        log.warn("⚠️ Não foi possível atualizar cache de comissão da empresa: {}", e.getMessage());
                }
        }

        @Transactional
        public Faturamento adicionarFaturamento(LocalDate data, BigDecimal valor,
                        com.empresa.comissao.domain.entity.User usuario) {
                log.info("📝 Registrando faturamento: {} - R$ {} - Usuário: {}", data, valor,
                                usuario != null ? usuario.getEmail() : "GLOBAL");

                Faturamento faturamento = Faturamento.builder()
                                .dataFaturamento(data)
                                .valor(valor)
                                .usuario(usuario)
                                .empresa(usuario != null ? usuario.getEmpresa() : null)
                                .build();

                Faturamento salvo = faturamentoRepository.save(faturamento);
                log.info("✅ Faturamento registrado com ID: {}", salvo.getId());

                // Invalidate Cache for this user's month
                invalidarCache(usuario, YearMonth.from(data));

                return salvo;
        }

        @Transactional
        public PagamentoAdiantado adicionarAdiantamento(LocalDate data, BigDecimal valor, String descricao,
                        com.empresa.comissao.domain.entity.User usuario) {
                log.info("📝 Registrando adiantamento: {} - R$ {} - Desc: {} - Usuário: {}", data, valor, descricao,
                                usuario != null ? usuario.getEmail() : "GLOBAL");

                PagamentoAdiantado adiantamento = PagamentoAdiantado.builder()
                                .dataPagamento(data)
                                .valor(valor)
                                .descricao(descricao)
                                .usuario(usuario)
                                .empresa(usuario != null ? usuario.getEmpresa() : null)
                                .build();

                PagamentoAdiantado salvo = pagamentoAdiantadoRepository.save(adiantamento);
                log.info("✅ Adiantamento registrado com ID: {}", salvo.getId());

                // Invalidate Cache for this user's month
                invalidarCache(usuario, YearMonth.from(data));

                // AUTO-FINANCEIRO: Gerar despesa (saída de caixa)
                if (usuario != null && usuario.getEmpresa() != null) {
                        try {
                                financeiroService.criarDespesaAdiantamento(salvo, usuario.getEmpresa());
                        } catch (Exception e) {
                                log.warn("⚠️ Falha ao criar registro financeiro para adiantamento: {}", e.getMessage());
                        }
                }

                return salvo;
        }

        @Transactional
        public Despesa adicionarDespesa(LocalDate data, BigDecimal valor, CategoriaDespesa categoria,
                        String descricao, com.empresa.comissao.domain.entity.User usuario) {
                log.info("📝 Registrando despesa: {} - R$ {} - {} - Usuário: {}", data, valor, categoria,
                                usuario != null ? usuario.getEmail() : "GLOBAL");

                Despesa despesa = Despesa.builder()
                                .dataDespesa(data)
                                .valor(valor)
                                .categoria(categoria)
                                .descricao(descricao)
                                .empresa(usuario != null ? usuario.getEmpresa() : null)
                                .build();

                Despesa salva = despesaRepository.save(despesa);
                log.info("✅ Despesa registrada com ID: {}", salva.getId());

                return salva;
        }

        @Transactional
        public Despesa atualizarDespesa(Despesa despesa) {
                return despesaRepository.save(despesa);
        }

        public RelatorioFinanceiroDTO gerarRelatorioFinanceiro(int ano, int mes,
                        com.empresa.comissao.domain.entity.User usuario,
                        com.empresa.comissao.domain.entity.Empresa empresaFresh) {
                YearMonth anoMes = YearMonth.of(ano, mes);
                LocalDate inicioDoMes = anoMes.atDay(1);
                LocalDate fimDoMes = anoMes.atEndOfMonth();

                log.info("📊 Gerando relatório consolidado para {}/{} - Usuário: {}", ano, mes,
                                usuario != null ? usuario.getEmail() : "GLOBAL");

                // ====================================================================
                // RECEITA PARA DRE: Regime de Competência (data da OS)
                // Diferente da comissão que usa Regime de Caixa (data do recebimento)
                // ====================================================================
                BigDecimal faturamentoTotal;

                if (empresaFresh != null) {
                        // Para a Auditoria Financeira / DRE, queremos SEMPRE a visão global da empresa
                        faturamentoTotal = contaReceberRepository
                                        .sumByCompetenciaBetweenForReports(empresaFresh, inicioDoMes, fimDoMes);
                        log.info("💰 Receita DRE (Competência global): {}", faturamentoTotal);
                } else {
                        // Fallback: usar faturamento tradicional se não houver empresa
                        faturamentoTotal = faturamentoRepository
                                        .sumValorByDataFaturamentoBetween(inicioDoMes, fimDoMes)
                                        .orElse(BigDecimal.ZERO);
                        log.info("💰 Receita DRE (Fallback legacy): {}", faturamentoTotal);
                }

                // 1. Obter Comissão do Mês (CONTINUA usando CAIXA como antes)
                ComissaoCalculada comissao = ComissaoCalculada.builder()
                                .faturamentoMensalTotal(BigDecimal.ZERO)
                                .valorBrutoComissao(BigDecimal.ZERO)
                                .saldoAReceber(BigDecimal.ZERO)
                                .valorTotalAdiantamentos(BigDecimal.ZERO)
                                .build();

                if (empresaFresh != null) {
                        if (empresaFresh.getModoComissao() == com.empresa.comissao.domain.enums.ModoComissao.COLETIVA) {
                                log.info("📊 Comissão calculada em modo COLETIVA para DRE da empresa: {}",
                                                empresaFresh.getNome());
                                comissao = calcularComissaoEmpresaMensal(ano, mes, empresaFresh);
                        } else {
                                log.info("📊 Agregando comissões INDIVIDUAIS de todos os funcionários para DRE da empresa: {}",
                                                empresaFresh.getNome());
                                List<ComissaoCalculada> comissoes = listarComissoesEmpresa(ano, mes, empresaFresh);

                                BigDecimal totalBruto = BigDecimal.ZERO;
                                BigDecimal totalAdiantamentos = BigDecimal.ZERO;
                                BigDecimal totalSaldo = BigDecimal.ZERO;

                                for (ComissaoCalculada c : comissoes) {
                                        totalBruto = totalBruto.add(
                                                        c.getValorBrutoComissao() != null ? c.getValorBrutoComissao()
                                                                        : BigDecimal.ZERO);
                                        totalAdiantamentos = totalAdiantamentos
                                                        .add(c.getValorTotalAdiantamentos() != null
                                                                        ? c.getValorTotalAdiantamentos()
                                                                        : BigDecimal.ZERO);
                                        totalSaldo = totalSaldo.add(c.getSaldoAReceber() != null ? c.getSaldoAReceber()
                                                        : BigDecimal.ZERO);
                                }

                                comissao = ComissaoCalculada.builder()
                                                .valorBrutoComissao(totalBruto)
                                                .valorTotalAdiantamentos(totalAdiantamentos)
                                                .saldoAReceber(totalSaldo)
                                                .build();
                        }
                }

                // 2. Calcular Imposto (usar alíquota configurada na empresa, default 6%)
                BigDecimal aliquota = (empresaFresh != null && empresaFresh.getAliquotaImposto() != null)
                                ? empresaFresh.getAliquotaImposto()
                                : new BigDecimal("0.06"); // Fallback: 6% Simples Nacional 1ª faixa
                BigDecimal imposto = faturamentoTotal.multiply(aliquota)
                                .setScale(2, RoundingMode.HALF_UP);

                BigDecimal aliquotaPercent = aliquota.multiply(new BigDecimal("100"));
                log.info("🏷️ Imposto calculado ({}%): {}", aliquotaPercent, imposto);

                // 3. Obter Despesas por Categoria e Total
                Map<CategoriaDespesa, BigDecimal> despesasPorCategoria = new EnumMap<>(CategoriaDespesa.class);

                // Inicializa o mapa com zero
                for (CategoriaDespesa cat : CategoriaDespesa.values()) {
                        despesasPorCategoria.put(cat, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                }

                // DRE: Regime de Competência
                // Despesas são contabilizadas pelo valor integral no mês em que ocorreram,
                // independente do pagamento da fatura do cartão.
                // (A lógica de caixa/rateio proporcional fica apenas no Fluxo de Caixa)

                // Buscar despesas do mês
                List<Despesa> despesasRaw;
                if (empresaFresh != null) {
                        despesasRaw = despesaRepository.findByEmpresaAndDataDespesaBetween(empresaFresh, inicioDoMes,
                                        fimDoMes);
                } else {
                        despesasRaw = despesaRepository.findByDataDespesaBetween(inicioDoMes, fimDoMes);
                }

                // Agregar despesas por categoria (valor integral, sem fator de pagamento)
                for (Despesa d : despesasRaw) {
                        // Excluir categorias já calculadas separadamente no DRE
                        boolean isExcluded = d.getCategoria() == CategoriaDespesa.IMPOSTOS_SOBRE_VENDA ||
                                        d.getCategoria() == CategoriaDespesa.PROLABORE;

                        if (!isExcluded) {
                                BigDecimal current = despesasPorCategoria.getOrDefault(d.getCategoria(),
                                                BigDecimal.ZERO);
                                despesasPorCategoria.put(d.getCategoria(),
                                                current.add(d.getValor()).setScale(2, RoundingMode.HALF_UP));
                        }
                }
                // Sum only valid operational expenses (excluding taxes and prolabore)
                BigDecimal despesasTotal = despesasPorCategoria.values().stream()
                                .reduce(BigDecimal.ZERO, BigDecimal::add)
                                .setScale(2, RoundingMode.HALF_UP);

                log.info("💸 Despesas totais: {}", despesasTotal);

                // 4. Comissão (Alocada e Saldo)
                BigDecimal comissaoAlocada = comissao.getValorBrutoComissao()
                                .setScale(2, RoundingMode.HALF_UP);
                BigDecimal adiantamentosTotal = comissao.getValorTotalAdiantamentos()
                                .setScale(2, RoundingMode.HALF_UP);
                BigDecimal saldoRemanescenteComissao = comissao.getSaldoAReceber()
                                .setScale(2, RoundingMode.HALF_UP);

                // 5. Total Geral (Despesas + Impostos + Comissão Alocada)
                // Usamos a comissão alocada no custo total para o lucro líquido não ser
                // alterado
                // por adiantamentos
                BigDecimal totalGeral = despesasTotal.add(imposto).add(comissaoAlocada)
                                .setScale(2, RoundingMode.HALF_UP);

                // 6. Lucro Líquido (Faturamento - Total Geral/Custos Totais)
                BigDecimal lucroLiquido = faturamentoTotal.subtract(totalGeral)
                                .setScale(2, RoundingMode.HALF_UP);

                log.info("✅ Relatório gerado com sucesso para {}/{}", ano, mes);

                return RelatorioFinanceiroDTO.builder()
                                .despesasPorCategoria(despesasPorCategoria)
                                .faturamentoTotal(faturamentoTotal)
                                .despesasTotal(despesasTotal)
                                .imposto(imposto)
                                .adiantamentosTotal(adiantamentosTotal)
                                .comissaoAlocada(comissaoAlocada)
                                .saldoRemanescenteComissao(saldoRemanescenteComissao)
                                .totalGeral(totalGeral)
                                .lucroLiquido(lucroLiquido)
                                .build();
        }

        public List<Faturamento> listarFaturamentos(com.empresa.comissao.domain.entity.Empresa empresa) {
                if (empresa != null) {
                        // USA QUERY OTIMIZADA COM JOIN FETCH
                        return faturamentoRepository.findAllWithRelations(empresa);
                }
                return java.util.Collections.emptyList();
        }

        public List<PagamentoAdiantado> listarAdiantamentos(com.empresa.comissao.domain.entity.Empresa empresa) {
                if (empresa != null) {
                        return pagamentoAdiantadoRepository.findByEmpresa(empresa);
                }
                return java.util.Collections.emptyList();
        }

        public List<Despesa> listarDespesas(com.empresa.comissao.domain.entity.Empresa empresa) {
                if (empresa != null) {
                        return despesaRepository.findByEmpresa(empresa);
                }
                return java.util.Collections.emptyList();
        }

        /**
         * Obtém comparação Year-over-Year de faturamento para um mês específico.
         * Compara o faturamento do mês atual com o mesmo mês do ano anterior.
         */
        public ComparacaoFaturamentoDTO obterComparacaoYoY(int ano, int mes,
                        com.empresa.comissao.domain.entity.User usuario,
                        com.empresa.comissao.domain.entity.Empresa empresa) {

                // Prioritize the passed 'empresa' (likely a proxy from TenantContext)
                com.empresa.comissao.domain.entity.Empresa empresaToUse = empresa;

                if (empresaToUse == null && usuario != null) {
                        empresaToUse = usuario.getEmpresa();
                }

                if (empresaToUse == null) {
                        // Try to recover from context if still null (last resort)
                        Long tenantId = com.empresa.comissao.config.TenantContext.getCurrentTenant();
                        if (tenantId != null) {
                                empresaToUse = new com.empresa.comissao.domain.entity.Empresa();
                                empresaToUse.setId(tenantId);
                        }
                }

                log.info("📊 Calculando comparação YoY para {}/{} - Empresa ID: {}", ano, mes,
                                empresaToUse != null ? empresaToUse.getId() : "NULL");

                if (empresaToUse == null) {
                        throw new com.empresa.comissao.exception.BusinessException(
                                        "Empresa é obrigatória para cálculo de comparação YoY.");
                }

                // Get current year/month revenue
                LocalDate inicio = LocalDate.of(ano, mes, 1);
                LocalDate fim = inicio.withDayOfMonth(inicio.lengthOfMonth());

                BigDecimal faturamentoAtual = faturamentoRepository
                                .sumValorByDataBetweenAndEmpresa(inicio, fim, empresaToUse);

                // Get previous year revenue for the same month
                int anoAnterior = ano - 1;
                LocalDate inicioAnterior = LocalDate.of(anoAnterior, mes, 1);
                LocalDate fimAnterior = inicioAnterior.withDayOfMonth(inicioAnterior.lengthOfMonth());

                BigDecimal faturamentoAnoAnterior = faturamentoRepository
                                .sumValorByDataBetweenAndEmpresa(inicioAnterior, fimAnterior, empresaToUse);

                boolean temDadosAnoAnterior = faturamentoAnoAnterior.compareTo(BigDecimal.ZERO) > 0;

                BigDecimal diferencaAbsoluta = null;
                BigDecimal diferencaPercentual = null;

                if (temDadosAnoAnterior) {
                        diferencaAbsoluta = faturamentoAtual.subtract(faturamentoAnoAnterior)
                                        .setScale(2, RoundingMode.HALF_UP);

                        // Calculate percentage: ((atual - anterior) / anterior) * 100
                        diferencaPercentual = diferencaAbsoluta
                                        .divide(faturamentoAnoAnterior, 4, RoundingMode.HALF_UP)
                                        .multiply(new BigDecimal("100"))
                                        .setScale(2, RoundingMode.HALF_UP);

                        log.info("✅ YoY: Atual={}, Anterior={}, Diferença={} ({}%)",
                                        faturamentoAtual, faturamentoAnoAnterior, diferencaAbsoluta,
                                        diferencaPercentual);
                } else {
                        log.info("ℹ️ Sem dados do ano anterior para comparação.");
                }

                return ComparacaoFaturamentoDTO.builder()
                                .anoAtual(ano)
                                .mesAtual(mes)
                                .faturamentoAtual(faturamentoAtual)
                                .faturamentoAnoAnterior(temDadosAnoAnterior ? faturamentoAnoAnterior : null)
                                .diferencaAbsoluta(diferencaAbsoluta)
                                .diferencaPercentual(diferencaPercentual)
                                .temDadosAnoAnterior(temDadosAnoAnterior)
                                .build();
        }

        /**
         * Gera relatório anual consolidado com comparação YoY para cada mês.
         */
        public RelatorioAnualDTO gerarRelatorioAnual(int ano,
                        com.empresa.comissao.domain.entity.User usuario,
                        com.empresa.comissao.domain.entity.Empresa empresa) {

                log.info("📅 Gerando relatório anual para {} - Empresa: {}", ano,
                                empresa != null ? empresa.getNome() : "N/A");

                // Safety check
                if (empresa == null && usuario != null && usuario.getEmpresa() == null) {
                        throw new com.empresa.comissao.exception.BusinessException(
                                        "Administradores de sistema não possuem dados de faturamento.");
                }

                com.empresa.comissao.domain.entity.Empresa empresaToUse = empresa != null ? empresa
                                : (usuario != null ? usuario.getEmpresa() : null);

                if (empresaToUse == null) {
                        throw new com.empresa.comissao.exception.BusinessException(
                                        "Empresa é obrigatória para geração de relatório anual.");
                }

                // Get monthly revenue for current year
                LocalDate inicioAno = LocalDate.of(ano, 1, 1);
                LocalDate fimAno = LocalDate.of(ano, 12, 31);

                List<Object[]> faturamentosMensais = faturamentoRepository
                                .findFaturamentoMensalByDataBetweenAndEmpresa(inicioAno, fimAno, empresaToUse);

                // Build map for quick lookup
                Map<Integer, BigDecimal> faturamentoPorMes = new java.util.HashMap<>();
                for (Object[] row : faturamentosMensais) {
                        Integer mes = (Integer) row[0];
                        BigDecimal total = (BigDecimal) row[1];
                        faturamentoPorMes.put(mes, total);
                }

                // Get previous year data
                int anoAnterior = ano - 1;
                LocalDate inicioAnoAnterior = LocalDate.of(anoAnterior, 1, 1);
                LocalDate fimAnoAnterior = LocalDate.of(anoAnterior, 12, 31);

                List<Object[]> faturamentosMensaisAnoAnterior = faturamentoRepository
                                .findFaturamentoMensalByDataBetweenAndEmpresa(inicioAnoAnterior, fimAnoAnterior,
                                                empresaToUse);

                Map<Integer, BigDecimal> faturamentoPorMesAnoAnterior = new java.util.HashMap<>();
                for (Object[] row : faturamentosMensaisAnoAnterior) {
                        Integer mes = (Integer) row[0];
                        BigDecimal total = (BigDecimal) row[1];
                        faturamentoPorMesAnoAnterior.put(mes, total);
                }

                // Build list of months with revenue data
                List<MesFaturamentoDTO> mesesComFaturamento = new java.util.ArrayList<>();
                String[] nomesMeses = { "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
                                "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro" };

                BigDecimal totalAno = BigDecimal.ZERO;
                BigDecimal totalAnoAnterior = BigDecimal.ZERO;

                for (int mes = 1; mes <= 12; mes++) {
                        BigDecimal faturamentoMes = faturamentoPorMes.getOrDefault(mes, BigDecimal.ZERO);
                        BigDecimal faturamentoMesAnoAnterior = faturamentoPorMesAnoAnterior.getOrDefault(mes,
                                        BigDecimal.ZERO);

                        totalAno = totalAno.add(faturamentoMes);
                        totalAnoAnterior = totalAnoAnterior.add(faturamentoMesAnoAnterior);

                        BigDecimal variacao = null;
                        BigDecimal variacaoPercentual = null;

                        if (faturamentoMesAnoAnterior.compareTo(BigDecimal.ZERO) > 0) {
                                variacao = faturamentoMes.subtract(faturamentoMesAnoAnterior)
                                                .setScale(2, RoundingMode.HALF_UP);
                                variacaoPercentual = variacao
                                                .divide(faturamentoMesAnoAnterior, 4, RoundingMode.HALF_UP)
                                                .multiply(new BigDecimal("100"))
                                                .setScale(2, RoundingMode.HALF_UP);
                        }

                        mesesComFaturamento.add(MesFaturamentoDTO.builder()
                                        .mes(mes)
                                        .nomeMes(nomesMeses[mes - 1])
                                        .faturamento(faturamentoMes.setScale(2, RoundingMode.HALF_UP))
                                        .faturamentoAnoAnterior(faturamentoMesAnoAnterior.compareTo(BigDecimal.ZERO) > 0
                                                        ? faturamentoMesAnoAnterior.setScale(2, RoundingMode.HALF_UP)
                                                        : null)
                                        .variacao(variacao)
                                        .variacaoPercentual(variacaoPercentual)
                                        .build());
                }

                // Calculate annual difference
                BigDecimal diferencaAnual = totalAno.subtract(totalAnoAnterior).setScale(2, RoundingMode.HALF_UP);
                BigDecimal crescimentoPercentualAnual = BigDecimal.ZERO;

                if (totalAnoAnterior.compareTo(BigDecimal.ZERO) > 0) {
                        crescimentoPercentualAnual = diferencaAnual
                                        .divide(totalAnoAnterior, 4, RoundingMode.HALF_UP)
                                        .multiply(new BigDecimal("100"))
                                        .setScale(2, RoundingMode.HALF_UP);
                }

                log.info("✅ Relatório anual gerado: Total={}, Total Ano Anterior={}, Crescimento={}%",
                                totalAno, totalAnoAnterior, crescimentoPercentualAnual);

                return RelatorioAnualDTO.builder()
                                .ano(ano)
                                .mesesComFaturamento(mesesComFaturamento)
                                .faturamentoTotalAno(totalAno.setScale(2, RoundingMode.HALF_UP))
                                .faturamentoTotalAnoAnterior(totalAnoAnterior.setScale(2, RoundingMode.HALF_UP))
                                .diferencaAnual(diferencaAnual)
                                .crescimentoPercentualAnual(crescimentoPercentualAnual)
                                .build();
        }

        public List<com.empresa.comissao.dto.response.RankingClienteDTO> gerarRankingClientes(int ano, Integer mes,
                        com.empresa.comissao.domain.entity.User usuario,
                        com.empresa.comissao.domain.entity.Empresa empresa) {

                // Determine which empresa to use (Tenant)
                com.empresa.comissao.domain.entity.Empresa empresaToUse = empresa != null ? empresa
                                : (usuario != null ? usuario.getEmpresa() : null);

                if (empresaToUse == null) {
                        throw new com.empresa.comissao.exception.BusinessException(
                                        "Empresa é obrigatória para gerar ranking de clientes.");
                }

                log.info("🏆 Gerando ranking de clientes para empresa: {} - Ano: {} - Mês: {}",
                                empresaToUse.getNome(), ano, mes != null ? mes : "TODOS");

                LocalDate start;
                LocalDate end;

                if (mes != null) {
                        start = LocalDate.of(ano, mes, 1);
                        end = start.withDayOfMonth(start.lengthOfMonth());
                } else {
                        start = LocalDate.of(ano, 1, 1);
                        end = LocalDate.of(ano, 12, 31);
                }

                return ordemServicoRepository.findRankingClientes(empresaToUse.getId(), start, end);
        }

        /**
         * Lista as comissões de todos os funcionários da empresa para um determinado
         * mês.
         * Usado pelo Admin/Financeiro para gestão de pagamentos.
         */
        @Transactional
        public List<ComissaoCalculada> listarComissoesEmpresa(int ano, int mes,
                        com.empresa.comissao.domain.entity.Empresa empresa) {
                return listarComissoesEmpresa(ano, mes, empresa, false);
        }

        @Transactional
        public List<ComissaoCalculada> listarComissoesEmpresa(int ano, int mes,
                        com.empresa.comissao.domain.entity.Empresa empresa, boolean force) {
                if (empresa == null) {
                        throw new com.empresa.comissao.exception.BusinessException("Empresa é obrigatória.");
                }

                log.info("📊 Listando comissões de todos os funcionários para {}/{} - Empresa: {}",
                                ano, mes, empresa.getNome());

                List<com.empresa.comissao.domain.entity.User> funcionarios = userRepository.findByEmpresa(empresa);
                List<ComissaoCalculada> comissoes = new java.util.ArrayList<>();

                for (com.empresa.comissao.domain.entity.User funcionario : funcionarios) {
                        try {
                                if (!funcionario.isParticipaComissao()) {
                                        continue; // Skip without error log
                                }
                                ComissaoCalculada comissao = calcularEObterComissaoMensal(ano, mes, funcionario, force);
                                comissoes.add(comissao);
                        } catch (Exception e) {
                                log.warn("⚠️ Erro ao calcular comissão para {}: {}", funcionario.getEmail(),
                                                e.getMessage());
                        }
                }

                log.info("✅ Retornando {} comissões calculadas", comissoes.size());
                return comissoes;
        }
}