package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.Recebimento;
import com.empresa.comissao.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface RecebimentoRepository extends JpaRepository<Recebimento, Long> {

    // ========================================
    // HISTÓRICO POR CONTA
    // ========================================

    List<Recebimento> findByContaReceberIdOrderByDataPagamentoAsc(Long contaReceberId);

    // ========================================
    // COMISSÃO COLETIVA: soma real de caixa no período
    // Substitui ContaReceberRepository.sumByRecebimentoBetween
    // ========================================

    @Query("SELECT COALESCE(SUM(r.valorPago), 0) FROM Recebimento r " +
            "WHERE r.empresa = :empresa " +
            "AND r.dataPagamento BETWEEN :inicio AND :fim")
    BigDecimal sumByEmpresaAndDataPagamentoBetween(
            @Param("empresa") Empresa empresa,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);

    // ========================================
    // COMISSÃO INDIVIDUAL: soma por funcionário
    // Substitui ContaReceberRepository.sumByRecebimentoBetweenAndFuncionario
    // ========================================

    @Query("SELECT COALESCE(SUM(r.valorPago), 0) FROM Recebimento r " +
            "WHERE r.empresa = :empresa " +
            "AND r.funcionarioResponsavel = :funcionario " +
            "AND r.dataPagamento BETWEEN :inicio AND :fim")
    BigDecimal sumByEmpresaAndFuncionarioAndDataPagamentoBetween(
            @Param("empresa") Empresa empresa,
            @Param("funcionario") User funcionario,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);

    // ========================================
    // FLUXO DE CAIXA: saldo inicial (acumulado antes de uma data)
    // ========================================

    @Query("SELECT COALESCE(SUM(r.valorPago), 0) FROM Recebimento r " +
            "WHERE r.empresa = :empresa " +
            "AND r.dataPagamento < :data")
    BigDecimal sumByEmpresaAndDataPagamentoBefore(
            @Param("empresa") Empresa empresa,
            @Param("data") LocalDate data);

    // ========================================
    // RELATÓRIO DE RECEITA POR CAIXA (BASE DAS)
    // ========================================

    @Query("SELECT r FROM Recebimento r " +
            "LEFT JOIN FETCH r.contaReceber c " +
            "LEFT JOIN FETCH c.ordemServico os " +
            "LEFT JOIN FETCH os.cliente " +
            "WHERE r.empresa = :empresa " +
            "AND r.dataPagamento BETWEEN :inicio AND :fim " +
            "ORDER BY r.dataPagamento ASC")
    List<Recebimento> findByEmpresaAndDataPagamentoBetween(
            @Param("empresa") Empresa empresa,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim);
}
