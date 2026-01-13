package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.ComissaoCalculada;
import com.empresa.comissao.domain.entity.Despesa;
import com.empresa.comissao.domain.entity.Faturamento;
import com.empresa.comissao.domain.entity.PagamentoAdiantado;
import com.empresa.comissao.domain.enums.CategoriaDespesa;
import com.empresa.comissao.domain.model.FaixaComissao;
import com.empresa.comissao.domain.model.TabelaComissao;
import com.empresa.comissao.dto.RelatorioFinanceiroDTO;
import com.empresa.comissao.dto.ComparacaoFaturamentoDTO;
import com.empresa.comissao.dto.MesFaturamentoDTO;
import com.empresa.comissao.dto.RelatorioAnualDTO;
import com.empresa.comissao.repository.ComissaoCalculadaRepository;
import com.empresa.comissao.repository.DespesaRepository;
import com.empresa.comissao.repository.FaturamentoRepository;
import com.empresa.comissao.repository.PagamentoAdiantadoRepository;
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

        @Transactional
        public ComissaoCalculada calcularEObterComissaoMensal(int ano, int mes,
                        com.empresa.comissao.domain.entity.User usuario) {
                YearMonth anoMesReferencia = YearMonth.of(ano, mes);

                log.info("🔍 Buscando comissão para: {}/{} - Usuário: {}", ano, mes,
                                usuario != null ? usuario.getUsername() : "GLOBAL");

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
                        log.info("✅ Comissão encontrada em cache: {}", anoMesReferencia);
                        return comissaoExistente.get();
                }

                log.info("📊 Comissão não encontrada. Calculando...");

                // 2. Somar o faturamento total do mês (Filtrando por usuário se informado)
                LocalDate inicioDoMes = anoMesReferencia.atDay(1);
                LocalDate fimDoMes = anoMesReferencia.atEndOfMonth();

                BigDecimal faturamentoMensalTotal;
                if (usuario != null) {
                        faturamentoMensalTotal = faturamentoRepository
                                        .sumValorByDataFaturamentoBetweenAndUsuario(inicioDoMes, fimDoMes, usuario)
                                        .orElse(BigDecimal.ZERO);
                } else {
                        faturamentoMensalTotal = faturamentoRepository
                                        .sumValorByDataFaturamentoBetween(inicioDoMes, fimDoMes)
                                        .orElse(BigDecimal.ZERO);
                }

                log.info("💰 Faturamento total: {}", faturamentoMensalTotal);

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

                // 4. Determinar a faixa de comissão
                FaixaComissao faixa = TabelaComissao.getFaixaByFaturamento(faturamentoMensalTotal);

                log.info("📈 Faixa encontrada: {} - {}%", faixa.getDescricao(), faixa.getPorcentagem());

                // 5. Calcular o valor bruto da comissão
                BigDecimal valorBrutoComissao = faturamentoMensalTotal.multiply(faixa.getPorcentagem())
                                .setScale(2, RoundingMode.HALF_UP);

                log.info("💵 Valor bruto da comissão: {}", valorBrutoComissao);

                // 6. Calcular o saldo a receber
                BigDecimal saldoAReceber = valorBrutoComissao.subtract(valorTotalAdiantamentos)
                                .setScale(2, RoundingMode.HALF_UP);

                log.info("✅ Saldo a receber: {}", saldoAReceber);

                // 7. Criar e persistir o objeto ComissaoCalculada
                ComissaoCalculada novaComissao = ComissaoCalculada.builder()
                                .anoMesReferencia(anoMesReferencia)
                                .faturamentoMensalTotal(faturamentoMensalTotal)
                                .faixaComissaoDescricao(faixa.getDescricao())
                                .porcentagemComissaoAplicada(faixa.getPorcentagem().multiply(new BigDecimal("100")))
                                .valorBrutoComissao(valorBrutoComissao)
                                .valorTotalAdiantamentos(valorTotalAdiantamentos)
                                .saldoAReceber(saldoAReceber)
                                .saldoAReceber(saldoAReceber)
                                .usuario(usuario)
                                .empresa(usuario != null ? usuario.getEmpresa() : null) // Bind business data to tenant
                                .build();

                ComissaoCalculada salva = comissaoCalculadaRepository.save(novaComissao);
                log.info("💾 Comissão salva com ID: {}", salva.getId());

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
                        log.info("✅ Comissão empresa encontrada em cache: {}", anoMesReferencia);
                        return comissaoExistente.get();
                }

                log.info("📊 Comissão empresa não encontrada. Calculando...");

                // 2. Sum all faturamento for the empresa
                LocalDate inicioDoMes = anoMesReferencia.atDay(1);
                LocalDate fimDoMes = anoMesReferencia.atEndOfMonth();

                BigDecimal faturamentoMensalTotal = faturamentoRepository
                                .sumValorByDataFaturamentoBetweenAndEmpresa(inicioDoMes, fimDoMes, empresa)
                                .orElse(BigDecimal.ZERO);

                log.info("💰 Faturamento total da empresa: {}", faturamentoMensalTotal);

                // 3. Sum all adiantamentos for the empresa
                BigDecimal valorTotalAdiantamentos = pagamentoAdiantadoRepository
                                .sumValorByDataPagamentoBetweenAndEmpresa(inicioDoMes, fimDoMes, empresa)
                                .orElse(BigDecimal.ZERO);

                log.info("💸 Adiantamentos total da empresa: {}", valorTotalAdiantamentos);

                // 4. Calculate commission
                com.empresa.comissao.domain.model.FaixaComissao faixa = com.empresa.comissao.domain.model.TabelaComissao
                                .getFaixaByFaturamento(faturamentoMensalTotal);

                log.info("📈 Faixa encontrada: {} - {}%", faixa.getDescricao(), faixa.getPorcentagem());

                BigDecimal valorBrutoComissao = faturamentoMensalTotal.multiply(faixa.getPorcentagem())
                                .setScale(2, java.math.RoundingMode.HALF_UP);

                BigDecimal saldoAReceber = valorBrutoComissao.subtract(valorTotalAdiantamentos)
                                .setScale(2, java.math.RoundingMode.HALF_UP);

                log.info("✅ Saldo a receber da empresa: {}", saldoAReceber);

                // 5. Save company-wide commission (no usuario, only empresa)
                ComissaoCalculada novaComissao = ComissaoCalculada.builder()
                                .anoMesReferencia(anoMesReferencia)
                                .faturamentoMensalTotal(faturamentoMensalTotal)
                                .faixaComissaoDescricao(faixa.getDescricao())
                                .porcentagemComissaoAplicada(faixa.getPorcentagem().multiply(new BigDecimal("100")))
                                .valorBrutoComissao(valorBrutoComissao)
                                .valorTotalAdiantamentos(valorTotalAdiantamentos)
                                .saldoAReceber(saldoAReceber)
                                .usuario(null) // No specific user for company-wide report
                                .empresa(empresa)
                                .build();

                ComissaoCalculada salva = comissaoCalculadaRepository.save(novaComissao);
                log.info("💾 Comissão empresa salva com ID: {}", salva.getId());

                return salva;
        }

        @Transactional
        public void invalidarCache(com.empresa.comissao.domain.entity.User usuario, YearMonth anoMes) {
                log.info("🗑️ Invalidando cache de comissão para Usuário: {} - Mês: {}",
                                usuario != null ? usuario.getEmail() : "GLOBAL", anoMes);

                // Invalidate user-specific cache
                comissaoCalculadaRepository.findFirstByAnoMesReferenciaAndUsuario(anoMes, usuario)
                                .ifPresent(comissao -> {
                                        comissaoCalculadaRepository.delete(comissao);
                                        log.info("✅ Cache de usuário invalidado com sucesso.");
                                });

                // Also invalidate empresa cache if user has empresa
                if (usuario != null && usuario.getEmpresa() != null) {
                        comissaoCalculadaRepository.findByAnoMesReferenciaAndEmpresa(anoMes, usuario.getEmpresa())
                                        .forEach(comissao -> {
                                                comissaoCalculadaRepository.delete(comissao);
                                                log.info("✅ Cache de empresa invalidado com sucesso.");
                                        });
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
        public PagamentoAdiantado adicionarAdiantamento(LocalDate data, BigDecimal valor,
                        com.empresa.comissao.domain.entity.User usuario) {
                log.info("📝 Registrando adiantamento: {} - R$ {} - Usuário: {}", data, valor,
                                usuario != null ? usuario.getEmail() : "GLOBAL");

                PagamentoAdiantado adiantamento = PagamentoAdiantado.builder()
                                .dataPagamento(data)
                                .valor(valor)
                                .usuario(usuario)
                                .empresa(usuario != null ? usuario.getEmpresa() : null)
                                .build();

                PagamentoAdiantado salvo = pagamentoAdiantadoRepository.save(adiantamento);
                log.info("✅ Adiantamento registrado com ID: {}", salvo.getId());

                // Invalidate Cache for this user's month
                invalidarCache(usuario, YearMonth.from(data));

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

        public RelatorioFinanceiroDTO gerarRelatorioFinanceiro(int ano, int mes,
                        com.empresa.comissao.domain.entity.User usuario,
                        com.empresa.comissao.domain.entity.Empresa empresaFresh) {
                YearMonth anoMes = YearMonth.of(ano, mes);
                LocalDate inicioDoMes = anoMes.atDay(1);
                LocalDate fimDoMes = anoMes.atEndOfMonth();

                log.info("📊 Gerando relatório consolidado para {}/{} - Usuário: {}", ano, mes,
                                usuario != null ? usuario.getEmail() : "GLOBAL");

                // 1. Obter Comissão do Mês (respeitando modoComissao)
                ComissaoCalculada comissao;
                if (empresaFresh != null && empresaFresh
                                .getModoComissao() == com.empresa.comissao.domain.enums.ModoComissao.COLETIVA) {
                        log.info("📊 Relatório usando modo COLETIVA para empresa: {}", empresaFresh.getNome());
                        comissao = calcularComissaoEmpresaMensal(ano, mes, empresaFresh);
                } else {
                        comissao = calcularEObterComissaoMensal(ano, mes, usuario);
                }
                BigDecimal faturamentoTotal = comissao.getFaturamentoMensalTotal();

                // 2. Calcular Imposto (6% sobre faturamento total)
                BigDecimal imposto = faturamentoTotal.multiply(new BigDecimal("0.06"))
                                .setScale(2, RoundingMode.HALF_UP);

                log.info("🏷️ Imposto calculado (6%): {}", imposto);

                // 3. Obter Despesas por Categoria e Total
                Map<CategoriaDespesa, BigDecimal> despesasPorCategoria = new EnumMap<>(CategoriaDespesa.class);

                // Inicializa o mapa com zero para todas as categorias para garantir que o front
                // receba uma estrutura completa
                for (CategoriaDespesa cat : CategoriaDespesa.values()) {
                        despesasPorCategoria.put(cat, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                }

                despesaRepository.sumValorByCategoriaAndDataDespesaBetween(inicioDoMes, fimDoMes)
                                .forEach(row -> {
                                        if (row != null && row.length >= 2 && row[0] != null && row[1] != null) {
                                                CategoriaDespesa cat = (CategoriaDespesa) row[0];
                                                BigDecimal val = (BigDecimal) row[1];
                                                despesasPorCategoria.put(cat, val.setScale(2, RoundingMode.HALF_UP));
                                        }
                                });

                BigDecimal despesasTotal = despesaRepository.sumValorByDataDespesaBetween(inicioDoMes, fimDoMes)
                                .orElse(BigDecimal.ZERO)
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

        public List<Faturamento> listarFaturamentos() {
                return faturamentoRepository.findAll();
        }

        public List<PagamentoAdiantado> listarAdiantamentos() {
                return pagamentoAdiantadoRepository.findAll();
        }

        public List<Despesa> listarDespesas() {
                return despesaRepository.findAll();
        }

        /**
         * Obtém comparação Year-over-Year de faturamento para um mês específico.
         * Compara o faturamento do mês atual com o mesmo mês do ano anterior.
         */
        public ComparacaoFaturamentoDTO obterComparacaoYoY(int ano, int mes,
                        com.empresa.comissao.domain.entity.User usuario,
                        com.empresa.comissao.domain.entity.Empresa empresa) {

                log.info("📊 Calculando comparação YoY para {}/{} - Empresa: {}", ano, mes,
                                empresa != null ? empresa.getNome() : "N/A");

                // Safety check for SaaS admins
                if (empresa == null && usuario != null && usuario.getEmpresa() == null) {
                        throw new com.empresa.comissao.exception.BusinessException(
                                        "Administradores de sistema não possuem dados de faturamento.");
                }

                // Determine which empresa to use
                com.empresa.comissao.domain.entity.Empresa empresaToUse = empresa != null ? empresa
                                : (usuario != null ? usuario.getEmpresa() : null);

                if (empresaToUse == null) {
                        throw new com.empresa.comissao.exception.BusinessException(
                                        "Empresa é obrigatória para cálculo de comparação YoY.");
                }

                // Get current year/month revenue
                BigDecimal faturamentoAtual = faturamentoRepository
                                .sumValorByAnoAndMesAndEmpresa(ano, mes, empresaToUse);

                // Get previous year revenue for the same month
                int anoAnterior = ano - 1;
                BigDecimal faturamentoAnoAnterior = faturamentoRepository
                                .sumValorByAnoAndMesAndEmpresa(anoAnterior, mes, empresaToUse);

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
                List<Object[]> faturamentosMensais = faturamentoRepository
                                .findFaturamentoMensalByAnoAndEmpresa(ano, empresaToUse);

                // Build map for quick lookup
                Map<Integer, BigDecimal> faturamentoPorMes = new java.util.HashMap<>();
                for (Object[] row : faturamentosMensais) {
                        Integer mes = (Integer) row[0];
                        BigDecimal total = (BigDecimal) row[1];
                        faturamentoPorMes.put(mes, total);
                }

                // Get previous year data
                int anoAnterior = ano - 1;
                List<Object[]> faturamentosMensaisAnoAnterior = faturamentoRepository
                                .findFaturamentoMensalByAnoAndEmpresa(anoAnterior, empresaToUse);

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
}