package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.PagamentoAdiantado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import com.empresa.comissao.domain.entity.User;

@Repository
public interface PagamentoAdiantadoRepository extends JpaRepository<PagamentoAdiantado, Long> {

        @Query("SELECT SUM(p.valor) FROM PagamentoAdiantado p WHERE p.dataPagamento BETWEEN :startDate AND :endDate")
        Optional<BigDecimal> sumValorByDataPagamentoBetween(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        @Query("SELECT SUM(p.valor) FROM PagamentoAdiantado p WHERE p.usuario = :usuario AND p.dataPagamento BETWEEN :startDate AND :endDate")
        Optional<BigDecimal> sumValorByDataPagamentoBetweenAndUsuario(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate, @Param("usuario") User usuario);

        @Query("SELECT SUM(p.valor) FROM PagamentoAdiantado p WHERE p.empresa = :empresa AND p.dataPagamento BETWEEN :startDate AND :endDate")
        Optional<BigDecimal> sumValorByDataPagamentoBetweenAndEmpresa(@Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate, @Param("empresa") Empresa empresa);
}
