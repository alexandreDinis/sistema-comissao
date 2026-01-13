package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.Faturamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.empresa.comissao.domain.entity.User;

@Repository
public interface FaturamentoRepository extends JpaRepository<Faturamento, Long> {

        @Query("SELECT SUM(f.valor) FROM Faturamento f WHERE f.dataFaturamento BETWEEN :startDate AND :endDate")
        Optional<BigDecimal> sumValorByDataFaturamentoBetween(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT SUM(f.valor) FROM Faturamento f WHERE f.usuario = :usuario AND f.dataFaturamento BETWEEN :startDate AND :endDate")
        Optional<BigDecimal> sumValorByDataFaturamentoBetweenAndUsuario(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate, @Param("usuario") User usuario);

        @Query("SELECT SUM(f.valor) FROM Faturamento f WHERE f.empresa = :empresa AND f.dataFaturamento BETWEEN :startDate AND :endDate")
        Optional<BigDecimal> sumValorByDataFaturamentoBetweenAndEmpresa(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate, @Param("empresa") Empresa empresa);

        List<Faturamento> findByEmpresa(Empresa empresa);

        // YoY Comparison Queries
        @Query("SELECT COALESCE(SUM(f.valor), 0) FROM Faturamento f WHERE f.empresa = :empresa " +
                        "AND YEAR(f.dataFaturamento) = :ano AND MONTH(f.dataFaturamento) = :mes")
        BigDecimal sumValorByAnoAndMesAndEmpresa(@Param("ano") int ano, @Param("mes") int mes,
                        @Param("empresa") Empresa empresa);

        @Query("SELECT COALESCE(SUM(f.valor), 0) FROM Faturamento f WHERE f.usuario = :usuario " +
                        "AND YEAR(f.dataFaturamento) = :ano AND MONTH(f.dataFaturamento) = :mes")
        BigDecimal sumValorByAnoAndMesAndUsuario(@Param("ano") int ano, @Param("mes") int mes,
                        @Param("usuario") User usuario);

        @Query("SELECT COALESCE(SUM(f.valor), 0) FROM Faturamento f WHERE f.empresa = :empresa " +
                        "AND YEAR(f.dataFaturamento) = :ano")
        BigDecimal sumValorByAnoAndEmpresa(@Param("ano") int ano, @Param("empresa") Empresa empresa);

        @Query("SELECT MONTH(f.dataFaturamento) as mes, SUM(f.valor) as total FROM Faturamento f " +
                        "WHERE f.empresa = :empresa AND YEAR(f.dataFaturamento) = :ano " +
                        "GROUP BY MONTH(f.dataFaturamento) ORDER BY MONTH(f.dataFaturamento)")
        List<Object[]> findFaturamentoMensalByAnoAndEmpresa(@Param("ano") int ano, @Param("empresa") Empresa empresa);
}
