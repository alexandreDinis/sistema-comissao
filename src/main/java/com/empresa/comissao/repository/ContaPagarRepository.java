package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.ContaPagar;
import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.domain.enums.StatusConta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ContaPagarRepository extends JpaRepository<ContaPagar, Long> {

        // Buscar por empresa e status
        List<ContaPagar> findByEmpresaAndStatus(Empresa empresa, StatusConta status);

        // Buscar por empresa ordenado por vencimento
        List<ContaPagar> findByEmpresaOrderByDataVencimentoAsc(Empresa empresa);

        // Buscar pendentes por empresa
        List<ContaPagar> findByEmpresaAndStatusOrderByDataVencimentoAsc(Empresa empresa, StatusConta status);

        // Buscar por funcionário
        List<ContaPagar> findByFuncionarioAndEmpresaOrderByDataVencimentoAsc(User funcionario, Empresa empresa);

        // Buscar vencidas (para alertas)
        @Query("SELECT c FROM ContaPagar c WHERE c.empresa = :empresa " +
                        "AND c.status = 'PENDENTE' AND c.dataVencimento < :hoje")
        List<ContaPagar> findVencidasByEmpresa(
                        @Param("empresa") Empresa empresa,
                        @Param("hoje") LocalDate hoje);

        // Soma de contas pendentes por empresa
        @Query("SELECT COALESCE(SUM(c.valor), 0) FROM ContaPagar c " +
                        "WHERE c.empresa = :empresa AND c.status = 'PENDENTE'")
        BigDecimal sumPendentesByEmpresa(@Param("empresa") Empresa empresa);

        // Soma por período (competência)
        @Query("SELECT COALESCE(SUM(c.valor), 0) FROM ContaPagar c " +
                        "WHERE c.empresa = :empresa AND c.dataCompetencia BETWEEN :inicio AND :fim")
        BigDecimal sumByCompetenciaBetween(
                        @Param("empresa") Empresa empresa,
                        @Param("inicio") LocalDate inicio,
                        @Param("fim") LocalDate fim);

        // Soma por período (pagamento - caixa)
        @Query("SELECT COALESCE(SUM(c.valor), 0) FROM ContaPagar c " +
                        "WHERE c.empresa = :empresa AND c.dataPagamento BETWEEN :inicio AND :fim AND c.status = 'PAGO'")
        BigDecimal sumByPagamentoBetween(
                        @Param("empresa") Empresa empresa,
                        @Param("inicio") LocalDate inicio,
                        @Param("fim") LocalDate fim);

        // Buscar por período de vencimento
        @Query("SELECT c FROM ContaPagar c WHERE c.empresa = :empresa " +
                        "AND c.dataVencimento BETWEEN :inicio AND :fim ORDER BY c.dataVencimento")
        List<ContaPagar> findByVencimentoBetween(
                        @Param("empresa") Empresa empresa,
                        @Param("inicio") LocalDate inicio,
                        @Param("fim") LocalDate fim);

        // Contar pendentes a vencer nos próximos X dias
        @Query("SELECT COUNT(c) FROM ContaPagar c WHERE c.empresa = :empresa " +
                        "AND c.status = 'PENDENTE' AND c.dataVencimento BETWEEN :hoje AND :limite")
        long countVencendoProximos(
                        @Param("empresa") Empresa empresa,
                        @Param("hoje") LocalDate hoje,
                        @Param("limite") LocalDate limite);

        // ========================================
        // FATURAS DE CARTÃO
        // ========================================

        // Buscar faturas por cartão e mês (pode haver mais de uma se houve pagamento
        // parcial/antecipado)
        @Query("SELECT c FROM ContaPagar c WHERE c.cartao = :cartao AND c.mesReferencia = :mesRef AND c.tipo = :tipo")
        List<ContaPagar> findByCartaoAndMesReferenciaAndTipo(
                        @Param("cartao") com.empresa.comissao.domain.entity.CartaoCredito cartao,
                        @Param("mesRef") String mesReferencia,
                        @Param("tipo") com.empresa.comissao.domain.enums.TipoContaPagar tipo);

        // Somar valor de faturas PAGAS por cartão e mês
        @Query("SELECT COALESCE(SUM(c.valor), 0) FROM ContaPagar c " +
                        "WHERE c.cartao = :cartao AND c.mesReferencia = :mesRef AND c.tipo = 'FATURA_CARTAO' AND c.status = :status")
        BigDecimal sumValorPagoByCartaoAndMes(
                        @Param("cartao") com.empresa.comissao.domain.entity.CartaoCredito cartao,
                        @Param("mesRef") String mesReferencia,
                        @Param("status") StatusConta status);

        // Listar faturas de cartão por empresa
        List<ContaPagar> findByEmpresaAndTipoOrderByDataVencimentoDesc(
                        Empresa empresa,
                        com.empresa.comissao.domain.enums.TipoContaPagar tipo);

        // Listar faturas por empresa, tipo e status
        List<ContaPagar> findByEmpresaAndTipoAndStatus(
                        Empresa empresa,
                        com.empresa.comissao.domain.enums.TipoContaPagar tipo,
                        StatusConta status);

        // Buscar conta a pagar associada a uma comissão
        java.util.Optional<ContaPagar> findFirstByComissao(
                        com.empresa.comissao.domain.entity.ComissaoCalculada comissao);

        // Somar valor de faturas pendentes por cartão (para cálculo de limite
        // disponível)
        @Query("SELECT COALESCE(SUM(c.valor), 0) FROM ContaPagar c " +
                        "WHERE c.cartao = :cartao AND c.status = :status AND c.tipo = 'FATURA_CARTAO'")
        BigDecimal sumValorByCartaoAndStatus(
                        @Param("cartao") com.empresa.comissao.domain.entity.CartaoCredito cartao,
                        @Param("status") StatusConta status);

        // Soma por período (pagamento - caixa)
        @Query("SELECT COALESCE(SUM(c.valor), 0) FROM ContaPagar c " +
                        "WHERE c.empresa = :empresa AND c.dataPagamento < :data AND c.status = 'PAGO'")
        BigDecimal sumByPagamentoBefore(
                        @Param("empresa") Empresa empresa,
                        @Param("data") LocalDate data);

        // Soma por tipo e vencimento (para Distribuição de Lucros acumulada)
        @Query("SELECT COALESCE(SUM(c.valor), 0) FROM ContaPagar c " +
                        "WHERE c.empresa = :empresa AND c.tipo = :tipo AND c.dataVencimento BETWEEN :inicio AND :fim")
        BigDecimal sumByTipoAndVencimentoBetween(
                        @Param("empresa") Empresa empresa,
                        @Param("tipo") com.empresa.comissao.domain.enums.TipoContaPagar tipo,
                        @Param("inicio") LocalDate inicio,
                        @Param("fim") LocalDate fim);

        // Buscar pagas por período (para relatório Contas a Pagar - Pagas)
        @Query("SELECT c FROM ContaPagar c WHERE c.empresa = :empresa " +
                        "AND c.dataPagamento BETWEEN :inicio AND :fim AND c.status = 'PAGO' ORDER BY c.dataPagamento")
        List<ContaPagar> findPagasBetween(
                        @Param("empresa") Empresa empresa,
                        @Param("inicio") LocalDate inicio,
                        @Param("fim") LocalDate fim);

        // Buscar por tipo e status e período de vencimento (para Distribuição de
        // Lucros)
        List<ContaPagar> findByEmpresaAndTipoAndDataVencimentoBetweenOrderByDataVencimentoDesc(
                        Empresa empresa,
                        com.empresa.comissao.domain.enums.TipoContaPagar tipo,
                        LocalDate inicio,
                        LocalDate fim);

        // OTIMIZAÇÃO DASHBOARD: Top 10 contas a pagar vencendo em breve
        // Projeta diretamente no DTO para evitar carregar entidades pesadas
        @Query("SELECT new com.empresa.comissao.dto.list.ContaResumoDTO(" +
                        "c.id, c.descricao, c.valor, c.dataVencimento, c.status, " +
                        "COALESCE(f.email, 'Diversos')) " +
                        "FROM ContaPagar c " +
                        "LEFT JOIN c.funcionario f " +
                        "WHERE c.empresa = :empresa " +
                        "AND c.status = com.empresa.comissao.domain.enums.StatusConta.PENDENTE " +
                        "ORDER BY c.dataVencimento ASC")
        java.util.List<com.empresa.comissao.dto.list.ContaResumoDTO> findTop10VencendoProximos(
                        @Param("empresa") com.empresa.comissao.domain.entity.Empresa empresa,
                        org.springframework.data.domain.Pageable pageable);
}
