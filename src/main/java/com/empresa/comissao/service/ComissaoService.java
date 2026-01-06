package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.ComissaoCalculada;
import com.empresa.comissao.domain.entity.Faturamento;
import com.empresa.comissao.domain.entity.PagamentoAdiantado;
import com.empresa.comissao.domain.model.FaixaComissao;
import com.empresa.comissao.domain.model.TabelaComissao;
import com.empresa.comissao.repository.ComissaoCalculadaRepository;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ComissaoService {

    private final FaturamentoRepository faturamentoRepository;
    private final PagamentoAdiantadoRepository pagamentoAdiantadoRepository;
    private final ComissaoCalculadaRepository comissaoCalculadaRepository;

    @Transactional
    public ComissaoCalculada calcularEObterComissaoMensal(int ano, int mes) {
        YearMonth anoMesReferencia = YearMonth.of(ano, mes);

        log.info("🔍 Buscando comissão para: {}/{}", ano, mes);

        // 1. Verificar se a comissão já foi calculada e persistida para este mês
        Optional<ComissaoCalculada> comissaoExistente = comissaoCalculadaRepository
                .findByAnoMesReferencia(anoMesReferencia);
        if (comissaoExistente.isPresent()) {
            log.info("✅ Comissão encontrada em cache: {}", anoMesReferencia);
            return comissaoExistente.get();
        }

        log.info("📊 Comissão não encontrada. Calculando...");

        // 2. Somar o faturamento total do mês
        LocalDate inicioDoMes = anoMesReferencia.atDay(1);
        LocalDate fimDoMes = anoMesReferencia.atEndOfMonth();

        log.info("📅 Período: {} a {}", inicioDoMes, fimDoMes);

        BigDecimal faturamentoMensalTotal = faturamentoRepository
                .sumValorByDataFaturamentoBetween(inicioDoMes, fimDoMes)
                .orElse(BigDecimal.ZERO);

        log.info("💰 Faturamento total: {}", faturamentoMensalTotal);

        // 3. Somar os adiantamentos totais do mês
        BigDecimal valorTotalAdiantamentos = pagamentoAdiantadoRepository
                .sumValorByDataPagamentoBetween(inicioDoMes, fimDoMes)
                .orElse(BigDecimal.ZERO);

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
                .build();

        ComissaoCalculada salva = comissaoCalculadaRepository.save(novaComissao);
        log.info("💾 Comissão salva com ID: {}", salva.getId());

        return salva;
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

        // ✅ IMPORTANTE: Deletar a comissão do mês atual para forçar recalcular
        YearMonth mesAtual = YearMonth.from(data);
        Optional<ComissaoCalculada> comissaoExistente = comissaoCalculadaRepository
                .findByAnoMesReferencia(mesAtual);

        if (comissaoExistente.isPresent()) {
            log.info("🗑️ Deletando comissão antiga para recalcular: {}", mesAtual);
            comissaoCalculadaRepository.delete(comissaoExistente.get());
            log.info("✅ Comissão deletada. Próxima requisição recalculará.");
        }

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

        // ✅ IMPORTANTE: Deletar a comissão do mês atual para forçar recalcular
        YearMonth mesAtual = YearMonth.from(data);
        Optional<ComissaoCalculada> comissaoExistente = comissaoCalculadaRepository
                .findByAnoMesReferencia(mesAtual);

        if (comissaoExistente.isPresent()) {
            log.info("🗑️ Deletando comissão antiga para recalcular: {}", mesAtual);
            comissaoCalculadaRepository.delete(comissaoExistente.get());
            log.info("✅ Comissão deletada. Próxima requisição recalculará.");
        }

        return salvo;
    }
}