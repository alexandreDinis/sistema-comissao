package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.ContaReceber;
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
import java.util.Optional;

@Repository
public interface ContaReceberRepository extends JpaRepository<ContaReceber, Long> {

        // Buscar por empresa e status
        List<ContaReceber> findByEmpresaAndStatus(Empresa empresa, StatusConta status);

        // Buscar por empresa ordenado por vencimento
        List<ContaReceber> findByEmpresaOrderByDataVencimentoAsc(Empresa empresa);

        // Buscar pendentes por empresa
        List<ContaReceber> findByEmpresaAndStatusOrderByDataVencimentoAsc(Empresa empresa, StatusConta status);

        // Buscar por funcionário responsável (para comissão individual)
        List<ContaReceber> findByFuncionarioResponsavelAndEmpresaOrderByDataRecebimentoAsc(User funcionario,
                        Empresa empresa);

        // Buscar vencidas (para alertas)
        @Query("SELECT c FROM ContaReceber c WHERE c.empresa = :empresa " +
                        "AND c.status = 'PENDENTE' AND c.dataVencimento < :hoje")
        List<ContaReceber> findVencidasByEmpresa(
                        @Param("empresa") Empresa empresa,
                        @Param("hoje") LocalDate hoje);

        // Soma de contas pendentes por empresa
        @Query("SELECT COALESCE(SUM(c.valor), 0) FROM ContaReceber c " +
                        "WHERE c.empresa = :empresa AND c.status = 'PENDENTE'")
        BigDecimal sumPendentesByEmpresa(@Param("empresa") Empresa empresa);

        // Soma por período (competência) - para DRE
        @Query("SELECT COALESCE(SUM(c.valor), 0) FROM ContaReceber c " +
                        "WHERE c.empresa = :empresa AND c.dataCompetencia BETWEEN :inicio AND :fim")
        BigDecimal sumByCompetenciaBetween(
                        @Param("empresa") Empresa empresa,
                        @Param("inicio") LocalDate inicio,
                        @Param("fim") LocalDate fim);

        // ========================================================
        // CRÍTICO: Soma por período de RECEBIMENTO (caixa) - para COMISSÃO
        // ========================================================

        // Soma recebido por empresa (para comissão COLETIVA)
        @Query("SELECT COALESCE(SUM(c.valor), 0) FROM ContaReceber c " +
                        "WHERE c.empresa = :empresa AND c.dataRecebimento BETWEEN :inicio AND :fim AND c.status = 'PAGO'")
        BigDecimal sumByRecebimentoBetween(
                        @Param("empresa") Empresa empresa,
                        @Param("inicio") LocalDate inicio,
                        @Param("fim") LocalDate fim);

        // Soma recebido por funcionário (para comissão INDIVIDUAL)
        @Query("SELECT COALESCE(SUM(c.valor), 0) FROM ContaReceber c " +
                        "WHERE c.empresa = :empresa AND c.funcionarioResponsavel = :funcionario " +
                        "AND c.dataRecebimento BETWEEN :inicio AND :fim AND c.status = 'PAGO'")
        BigDecimal sumByRecebimentoBetweenAndFuncionario(
                        @Param("empresa") Empresa empresa,
                        @Param("funcionario") User funcionario,
                        @Param("inicio") LocalDate inicio,
                        @Param("fim") LocalDate fim);

        // ========================================================

        // Buscar por período de vencimento
        @Query("SELECT c FROM ContaReceber c WHERE c.empresa = :empresa " +
                        "AND c.dataVencimento BETWEEN :inicio AND :fim ORDER BY c.dataVencimento")
        List<ContaReceber> findByVencimentoBetween(
                        @Param("empresa") Empresa empresa,
                        @Param("inicio") LocalDate inicio,
                        @Param("fim") LocalDate fim);

        // Buscar recebidos por período (para fluxo de caixa)
        @Query("SELECT c FROM ContaReceber c WHERE c.empresa = :empresa " +
                        "AND c.dataRecebimento BETWEEN :inicio AND :fim AND c.status = 'PAGO' ORDER BY c.dataRecebimento")
        List<ContaReceber> findRecebidosBetween(
                        @Param("empresa") Empresa empresa,
                        @Param("inicio") LocalDate inicio,
                        @Param("fim") LocalDate fim);

        // Contar pendentes a vencer nos próximos X dias
        @Query("SELECT COUNT(c) FROM ContaReceber c WHERE c.empresa = :empresa " +
                        "AND c.status = 'PENDENTE' AND c.dataVencimento BETWEEN :hoje AND :limite")
        long countVencendoProximos(
                        @Param("empresa") Empresa empresa,
                        @Param("hoje") LocalDate hoje,
                        @Param("limite") LocalDate limite);

        // Verificar se já existe conta para um faturamento
        Optional<ContaReceber> findByFaturamentoId(Long faturamentoId);

        // Soma recebido ANTES de uma data (para Saldo Inicial de Caixa)
        @Query("SELECT COALESCE(SUM(c.valor), 0) FROM ContaReceber c " +
                        "WHERE c.empresa = :empresa AND c.dataRecebimento < :data AND c.status = 'PAGO'")
        BigDecimal sumByRecebimentoBefore(
                        @Param("empresa") Empresa empresa,
                        @Param("data") LocalDate data);
}
