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

        // Buscar fatura por cartão e mês
        @Query("SELECT c FROM ContaPagar c WHERE c.cartao = :cartao AND c.mesReferencia = :mesRef AND c.tipo = :tipo")
        java.util.Optional<ContaPagar> findByCartaoAndMesReferenciaAndTipo(
                        @Param("cartao") com.empresa.comissao.domain.entity.CartaoCredito cartao,
                        @Param("mesRef") String mesReferencia,
                        @Param("tipo") com.empresa.comissao.domain.enums.TipoContaPagar tipo);

        // Listar faturas de cartão por empresa
        List<ContaPagar> findByEmpresaAndTipoOrderByDataVencimentoDesc(
                        Empresa empresa,
                        com.empresa.comissao.domain.enums.TipoContaPagar tipo);
}
