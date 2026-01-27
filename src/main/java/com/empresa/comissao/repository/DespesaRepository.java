package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.Despesa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DespesaRepository extends JpaRepository<Despesa, Long> {

        List<Despesa> findByEmpresa(com.empresa.comissao.domain.entity.Empresa empresa);

        List<Despesa> findByDataDespesaBetween(LocalDate start, LocalDate end);

        List<Despesa> findByEmpresaAndDataDespesaBetween(com.empresa.comissao.domain.entity.Empresa empresa,
                        LocalDate start, LocalDate end);

        @Query("SELECT SUM(d.valor) FROM Despesa d WHERE d.dataDespesa BETWEEN :start AND :end")
        Optional<BigDecimal> sumValorByDataDespesaBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

        @Query("SELECT d.categoria, SUM(d.valor) FROM Despesa d WHERE d.dataDespesa BETWEEN :start AND :end GROUP BY d.categoria")
        List<Object[]> sumValorByCategoriaAndDataDespesaBetween(@Param("start") LocalDate start,
                        @Param("end") LocalDate end);

        @Query("SELECT SUM(d.valor) FROM Despesa d WHERE d.empresa = :empresa AND d.dataDespesa BETWEEN :start AND :end")
        Optional<BigDecimal> sumValorByDataDespesaBetweenAndEmpresa(@Param("start") LocalDate start,
                        @Param("end") LocalDate end,
                        @Param("empresa") com.empresa.comissao.domain.entity.Empresa empresa);

        @Query("SELECT d.categoria, SUM(d.valor) FROM Despesa d WHERE d.empresa = :empresa AND d.dataDespesa BETWEEN :start AND :end GROUP BY d.categoria")
        List<Object[]> sumValorByCategoriaAndDataDespesaBetweenAndEmpresa(@Param("start") LocalDate start,
                        @Param("end") LocalDate end,
                        @Param("empresa") com.empresa.comissao.domain.entity.Empresa empresa);

        // ========================================
        // CARTÃO DE CRÉDITO
        // ========================================

        // Soma despesas de um cartão em um período
        @Query("SELECT COALESCE(SUM(d.valor), 0) FROM Despesa d WHERE d.cartao = :cartao AND d.dataDespesa BETWEEN :inicio AND :fim")
        BigDecimal sumByCartaoAndPeriodo(
                        @Param("cartao") com.empresa.comissao.domain.entity.CartaoCredito cartao,
                        @Param("inicio") LocalDate inicio,
                        @Param("fim") LocalDate fim);

        // Listar despesas de um cartão em um período
        List<Despesa> findByCartaoAndDataDespesaBetween(
                        com.empresa.comissao.domain.entity.CartaoCredito cartao,
                        LocalDate inicio,
                        LocalDate fim);
}
