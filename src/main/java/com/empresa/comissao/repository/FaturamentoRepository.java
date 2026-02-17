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

        // OTIMIZACAO PARA DASHBOARD FINANCEIRO (Evita N+1 ao carregar OS e veículos)
        // Usa Set nas entidades para permitir múltiplos JOIN FETCH
        @Query("SELECT DISTINCT f FROM Faturamento f " +
                        "LEFT JOIN FETCH f.ordemServico os " +
                        "LEFT JOIN FETCH os.cliente " +
                        "LEFT JOIN FETCH os.veiculos v " +
                        "LEFT JOIN FETCH v.pecas p " +
                        "LEFT JOIN FETCH p.tipoPeca " +
                        "WHERE f.empresa = :empresa " +
                        "ORDER BY f.dataFaturamento DESC")
        List<Faturamento> findAllWithRelations(@Param("empresa") Empresa empresa);

        List<Faturamento> findByEmpresa(Empresa empresa);

        // YoY Comparison Queries
        // YoY Comparison Queries (Optimized with BETWEEN)
        @Query("SELECT COALESCE(SUM(f.valor), 0) FROM Faturamento f WHERE f.empresa = :empresa " +
                        "AND f.dataFaturamento BETWEEN :inicio AND :fim")
        BigDecimal sumValorByDataBetweenAndEmpresa(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim,
                        @Param("empresa") Empresa empresa);

        @Query("SELECT COALESCE(SUM(f.valor), 0) FROM Faturamento f WHERE f.usuario = :usuario " +
                        "AND f.dataFaturamento BETWEEN :inicio AND :fim")
        BigDecimal sumValorByDataBetweenAndUsuario(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim,
                        @Param("usuario") User usuario);

        // Used for total year sum (pass Jan 1 to Dec 31)
        @Query("SELECT COALESCE(SUM(f.valor), 0) FROM Faturamento f WHERE f.empresa = :empresa " +
                        "AND f.dataFaturamento BETWEEN :inicio AND :fim")
        BigDecimal sumValorTotalByDataBetweenAndEmpresa(@Param("inicio") LocalDate inicio, @Param("fim") LocalDate fim,
                        @Param("empresa") Empresa empresa);

        @Query("SELECT MONTH(f.dataFaturamento) as mes, SUM(f.valor) as total FROM Faturamento f " +
                        "WHERE f.empresa = :empresa AND f.dataFaturamento BETWEEN :inicio AND :fim " +
                        "GROUP BY MONTH(f.dataFaturamento) ORDER BY MONTH(f.dataFaturamento)")
        List<Object[]> findFaturamentoMensalByDataBetweenAndEmpresa(@Param("inicio") LocalDate inicio,
                        @Param("fim") LocalDate fim, @Param("empresa") Empresa empresa);

        Optional<Faturamento> findByOrdemServico(com.empresa.comissao.domain.entity.OrdemServico ordemServico);

        @Query("SELECT DISTINCT f FROM Faturamento f " +
                        "LEFT JOIN FETCH f.ordemServico os " +
                        "LEFT JOIN FETCH os.cliente " +
                        "LEFT JOIN FETCH os.veiculos v " +
                        "LEFT JOIN FETCH v.pecas p " +
                        "LEFT JOIN FETCH p.tipoPeca " +
                        "WHERE f.id = :id")
        Optional<Faturamento> findByIdComGrafoCompleto(@Param("id") Long id);
}
