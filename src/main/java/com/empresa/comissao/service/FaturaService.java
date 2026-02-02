package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.*;
import com.empresa.comissao.domain.enums.StatusConta;
import com.empresa.comissao.domain.enums.TipoContaPagar;
import com.empresa.comissao.exception.BusinessException;
import com.empresa.comissao.repository.ContaPagarRepository;
import com.empresa.comissao.repository.DespesaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Serviço para gestão de Faturas de Cartão de Crédito.
 * Implementa o modelo ERP: despesas agrupadas em uma única fatura mensal.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FaturaService {

    private final ContaPagarRepository contaPagarRepository;
    private final DespesaRepository despesaRepository;

    private static final DateTimeFormatter MES_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * Busca ou cria uma fatura para o cartão no mês especificado.
     */
    @Transactional
    public ContaPagar buscarOuCriarFatura(CartaoCredito cartao, LocalDate dataDespesa) {
        // Determinar mês da fatura baseado no ciclo de fechamento
        // Se despesa é APÓS o fechamento, vai para próxima fatura
        YearMonth mesFatura;
        int diaFechamento = cartao.getDiaFechamento() != null ? cartao.getDiaFechamento() : 25;

        if (dataDespesa.getDayOfMonth() > diaFechamento) {
            mesFatura = YearMonth.from(dataDespesa).plusMonths(1);
        } else {
            mesFatura = YearMonth.from(dataDespesa);
        }

        String mesReferencia = mesFatura.format(MES_FORMATTER);

        log.info("📄 Buscando fatura: {} - {} (Despesa: {}, Fechamento dia: {})",
                cartao.getNome(), mesReferencia, dataDespesa, diaFechamento);

        // Buscar TODAS as faturas deste mês (pode ter mais de uma se houve pagamento
        // antecipado)
        List<ContaPagar> faturasDoMes = contaPagarRepository
                .findByCartaoAndMesReferenciaAndTipo(cartao, mesReferencia, TipoContaPagar.FATURA_CARTAO);

        // Tenta encontrar uma fatura ABERTA (PENDENTE)
        ContaPagar faturaAberta = faturasDoMes.stream()
                .filter(f -> f.getStatus() == StatusConta.PENDENTE)
                .findFirst()
                .orElse(null);

        if (faturaAberta != null) {
            log.info("📄 Fatura aberta encontrada: ID {}", faturaAberta.getId());
            return faturaAberta;
        }

        // Se não tem fatura aberta (ou não existe nenhuma, ou todas estão PAGAS), cria
        // uma nova
        // Isso permite continuar lançando despesas no mês mesmo após pagar a fatura
        // parcial/antecipada

        log.info("📄 Nenhuma fatura aberta encontrada para {}. Criando nova fatura (Complementar se houver pagas).",
                mesReferencia);

        LocalDate dataVencimento = calcularDataVencimento(cartao, mesFatura);

        // Data de competência = Dia do fechamento no mês de referência
        int diaFechamentoComp = cartao.getDiaFechamento() != null ? cartao.getDiaFechamento() : 25;
        LocalDate dataCompetencia = mesFatura.atDay(Math.min(diaFechamentoComp, mesFatura.lengthOfMonth()));

        ContaPagar novaFatura = ContaPagar.builder()
                .empresa(cartao.getEmpresa())
                .descricao("Fatura " + cartao.getNome() + " - " + mesReferencia)
                .valor(BigDecimal.ZERO) // Será atualizado
                .dataCompetencia(dataCompetencia)
                .dataVencimento(dataVencimento)
                .status(StatusConta.PENDENTE)
                .tipo(TipoContaPagar.FATURA_CARTAO)
                .cartao(cartao)
                .mesReferencia(mesReferencia)
                .build();

        ContaPagar salva = contaPagarRepository.save(novaFatura);
        log.info("📄 Nova fatura criada: ID {} | Ref: {} | Vencimento: {}",
                salva.getId(), mesReferencia, dataVencimento);
        return salva;
    }

    /**
     * Atualiza o valor da fatura somando todas as despesas do cartão no mês.
     * Leva em consideração valores já pagos em outras faturas do mesmo mês.
     */
    @Transactional
    public void atualizarValorFatura(ContaPagar fatura) {
        if (fatura.getCartao() == null || fatura.getMesReferencia() == null) {
            log.warn("⚠️ Fatura sem cartão ou mês de referência");
            return;
        }

        // 1. Calcular o PERÍODO DO CICLO (Fechamento anterior + 1 dia ATÉ Fechamento
        // atual)
        YearMonth mesFaturaYM = YearMonth.parse(fatura.getMesReferencia(), MES_FORMATTER);

        // Dia de fechamento configurado (ex: 27)
        int diaFechamento = fatura.getCartao().getDiaFechamento() != null ? fatura.getCartao().getDiaFechamento() : 25;

        // Data Fim = Fechamento do mês atual (Safeguard para Fev/30 dias)
        LocalDate fim = mesFaturaYM.atDay(Math.min(diaFechamento, mesFaturaYM.lengthOfMonth()));

        // Data Início = Fechamento do mês anterior + 1 dia
        YearMonth mesAnteriorYM = mesFaturaYM.minusMonths(1);
        LocalDate fechamentoAnterior = mesAnteriorYM.atDay(Math.min(diaFechamento, mesAnteriorYM.lengthOfMonth()));
        LocalDate inicio = fechamentoAnterior.plusDays(1);

        log.info("📊 Calculando fatura {} ({}) - Ciclo: {} a {}",
                fatura.getId(), fatura.getMesReferencia(), inicio, fim);

        BigDecimal totalDespesas = despesaRepository.sumByCartaoAndPeriodo(
                fatura.getCartao(), inicio, fim);

        if (totalDespesas == null) {
            totalDespesas = BigDecimal.ZERO;
        }

        // 2. Calcular o TOTAL já pago em faturas FECHADAS/PAGAS deste mesmo CICLO/MÊS
        // REFERÊNCIA
        // Nota: O filtro por MesReferencia no repository já deve cobrir isso se estiver
        // correto,
        // mas mantemos a logica original de verificar o que já foi baixado para esta
        // referência.
        BigDecimal totalJaPago = contaPagarRepository.sumValorPagoByCartaoAndMes(
                fatura.getCartao(), fatura.getMesReferencia(), StatusConta.PAGO);

        if (totalJaPago == null) {
            totalJaPago = BigDecimal.ZERO;
        }

        // 3. O valor desta fatura deve ser o saldo restante
        BigDecimal saldoRestante = totalDespesas.subtract(totalJaPago);

        if (saldoRestante.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("⚠️ Saldo restante negativo para fatura {}: R$ {}. Pagou a mais?", fatura.getId(), saldoRestante);
            saldoRestante = BigDecimal.ZERO;
        }

        fatura.setValor(saldoRestante);
        contaPagarRepository.save(fatura);
        log.info("📄 Fatura {} atualizada. Total Despesas: R$ {}, Já Pago: R$ {}, Novo Valor: R$ {}",
                fatura.getId(), totalDespesas, totalJaPago, saldoRestante);
    }

    /**
     * Calcula a data de vencimento da fatura.
     * Regra: dia do vencimento no mês seguinte ao mês de referência da fatura.
     */
    private LocalDate calcularDataVencimento(CartaoCredito cartao, YearMonth mesReferenciaFatura) {
        // Se a fatura é de Janeiro, vence em Fevereiro
        YearMonth mesVencimento = mesReferenciaFatura.plusMonths(1);
        int dia = Math.min(cartao.getDiaVencimento(), mesVencimento.lengthOfMonth());
        return mesVencimento.atDay(dia);
    }

    /**
     * Lista todas as faturas de cartão de uma empresa.
     */
    public List<ContaPagar> listarFaturas(Empresa empresa) {
        return contaPagarRepository.findByEmpresaAndTipoOrderByDataVencimentoDesc(
                empresa, TipoContaPagar.FATURA_CARTAO);
    }

    /**
     * Calcula o limite disponível do cartão.
     * Limite disponível = Limite total - Soma das faturas PENDENTES
     */
    public BigDecimal calcularLimiteDisponivel(CartaoCredito cartao) {
        if (cartao.getLimite() == null) {
            return null; // Sem limite definido
        }

        BigDecimal totalPendente = contaPagarRepository
                .sumValorByCartaoAndStatus(cartao, StatusConta.PENDENTE);

        if (totalPendente == null) {
            totalPendente = BigDecimal.ZERO;
        }

        return cartao.getLimite().subtract(totalPendente);
    }

    /**
     * Verifica se há limite disponível para uma nova despesa.
     * 
     * @throws BusinessException se o limite for insuficiente
     */
    public void validarLimiteDisponivel(CartaoCredito cartao, BigDecimal valorDespesa) {
        if (cartao.getLimite() == null) {
            return; // Sem limite definido = sem restrição
        }

        BigDecimal limiteDisponivel = calcularLimiteDisponivel(cartao);

        if (valorDespesa.compareTo(limiteDisponivel) > 0) {
            throw new BusinessException(String.format(
                    "Limite insuficiente. Disponivel: R$ %.2f, Necessario: R$ %.2f",
                    limiteDisponivel, valorDespesa));
        }
    }
}
