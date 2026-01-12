package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.ComissaoCalculada;
import com.empresa.comissao.domain.entity.Despesa;
import com.empresa.comissao.domain.entity.Faturamento;
import com.empresa.comissao.domain.entity.PagamentoAdiantado;
import com.empresa.comissao.domain.enums.CategoriaDespesa;
import com.empresa.comissao.domain.model.FaixaComissao;
import com.empresa.comissao.domain.model.TabelaComissao;
import com.empresa.comissao.dto.RelatorioFinanceiroDTO;
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
                                .findByAnoMesReferenciaAndUsuario(anoMesReferencia, usuario);

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

        @Transactional
        public void invalidarCache(com.empresa.comissao.domain.entity.User usuario, YearMonth anoMes) {
                log.info("🗑️ Invalidando cache de comissão para Usuário: {} - Mês: {}",
                                usuario != null ? usuario.getEmail() : "GLOBAL", anoMes);
                comissaoCalculadaRepository.findByAnoMesReferenciaAndUsuario(anoMes, usuario)
                                .ifPresent(comissao -> {
                                        comissaoCalculadaRepository.delete(comissao);
                                        log.info("✅ Cache invalidado com sucesso.");
                                });
        }

        @Transactional
        public Faturamento adicionarFaturamento(LocalDate data, BigDecimal valor) {
                log.info("📝 Registrando faturamento: {} - R$ {}", data, valor);

                Faturamento faturamento = Faturamento.builder()
                                .dataFaturamento(data)
                                .valor(valor)
                                .build();

                Faturamento salvo = faturamentoRepository.save(faturamento);
                log.info("✅ Faturamento registrado com ID: {}", salvo.getId());

                // Invalidate Global Cache (since we don't know user here in this legacy method)
                invalidarCache(null, YearMonth.from(data));

                return salvo;
        }

        @Transactional
        public PagamentoAdiantado adicionarAdiantamento(LocalDate data, BigDecimal valor) {
                log.info("📝 Registrando adiantamento: {} - R$ {}", data, valor);

                PagamentoAdiantado adiantamento = PagamentoAdiantado.builder()
                                .dataPagamento(data)
                                .valor(valor)
                                .build();

                PagamentoAdiantado salvo = pagamentoAdiantadoRepository.save(adiantamento);
                log.info("✅ Adiantamento registrado com ID: {}", salvo.getId());

                // Invalidate Global Cache
                invalidarCache(null, YearMonth.from(data));

                return salvo;
        }

        @Transactional
        public Despesa adicionarDespesa(LocalDate data, BigDecimal valor, CategoriaDespesa categoria,
                        String descricao) {
                log.info("📝 Registrando despesa: {} - R$ {} - {}", data, valor, categoria);

                Despesa despesa = Despesa.builder()
                                .dataDespesa(data)
                                .valor(valor)
                                .categoria(categoria)
                                .descricao(descricao)
                                .build();

                Despesa salva = despesaRepository.save(despesa);
                log.info("✅ Despesa registrada com ID: {}", salva.getId());

                return salva;
        }

        public RelatorioFinanceiroDTO gerarRelatorioFinanceiro(int ano, int mes,
                        com.empresa.comissao.domain.entity.User usuario) {
                YearMonth anoMes = YearMonth.of(ano, mes);
                LocalDate inicioDoMes = anoMes.atDay(1);
                LocalDate fimDoMes = anoMes.atEndOfMonth();

                log.info("📊 Gerando relatório consolidado para {}/{} - Usuário: {}", ano, mes,
                                usuario != null ? usuario.getEmail() : "GLOBAL");

                // 1. Obter Comissão do Mês (contém faturamento mensal total)
                ComissaoCalculada comissao = calcularEObterComissaoMensal(ano, mes, usuario);
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
}