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

        // Buscar fatura existente
        ContaPagar faturaExistente = contaPagarRepository
                .findByCartaoAndMesReferenciaAndTipo(cartao, mesReferencia, TipoContaPagar.FATURA_CARTAO)
                .orElse(null);

        if (faturaExistente != null) {
            // Validar se fatura já foi paga
            if (faturaExistente.getStatus() == StatusConta.PAGO) {
                throw new BusinessException(
                        "Fatura de " + mesReferencia + " já foi paga. Não é possível adicionar despesas.");
            }
            log.info("📄 Fatura existente encontrada: ID {}", faturaExistente.getId());
            return faturaExistente;
        }

        // Criar nova fatura
        LocalDate dataVencimento = calcularDataVencimento(cartao, dataDespesa);

        ContaPagar novaFatura = ContaPagar.builder()
                .empresa(cartao.getEmpresa())
                .descricao("Fatura " + cartao.getNome() + " - " + mesReferencia)
                .valor(BigDecimal.ZERO) // Será atualizado
                .dataCompetencia(dataDespesa.withDayOfMonth(1))
                .dataVencimento(dataVencimento)
                .status(StatusConta.PENDENTE)
                .tipo(TipoContaPagar.FATURA_CARTAO)
                .cartao(cartao)
                .mesReferencia(mesReferencia)
                .build();

        ContaPagar salva = contaPagarRepository.save(novaFatura);
        log.info("📄 Nova fatura criada: ID {} | Vencimento: {}", salva.getId(), dataVencimento);
        return salva;
    }

    /**
     * Atualiza o valor da fatura somando todas as despesas do cartão no mês.
     */
    @Transactional
    public void atualizarValorFatura(ContaPagar fatura) {
        if (fatura.getCartao() == null || fatura.getMesReferencia() == null) {
            log.warn("⚠️ Fatura sem cartão ou mês de referência");
            return;
        }

        // Buscar todas as despesas do cartão no mês
        YearMonth ym = YearMonth.parse(fatura.getMesReferencia(), MES_FORMATTER);
        LocalDate inicio = ym.atDay(1);
        LocalDate fim = ym.atEndOfMonth();

        BigDecimal total = despesaRepository.sumByCartaoAndPeriodo(
                fatura.getCartao(), inicio, fim);

        if (total == null) {
            total = BigDecimal.ZERO;
        }

        fatura.setValor(total);
        contaPagarRepository.save(fatura);
        log.info("📄 Fatura atualizada: ID {} | Novo valor: R$ {}", fatura.getId(), total);
    }

    /**
     * Calcula a data de vencimento da fatura.
     * Regra: dia do vencimento no mês seguinte.
     */
    private LocalDate calcularDataVencimento(CartaoCredito cartao, LocalDate dataDespesa) {
        YearMonth mesSeguinte = YearMonth.from(dataDespesa).plusMonths(1);
        int dia = Math.min(cartao.getDiaVencimento(), mesSeguinte.lengthOfMonth());
        return mesSeguinte.atDay(dia);
    }

    /**
     * Lista todas as faturas de cartão de uma empresa.
     */
    public List<ContaPagar> listarFaturas(Empresa empresa) {
        return contaPagarRepository.findByEmpresaAndTipoOrderByDataVencimentoDesc(
                empresa, TipoContaPagar.FATURA_CARTAO);
    }
}
