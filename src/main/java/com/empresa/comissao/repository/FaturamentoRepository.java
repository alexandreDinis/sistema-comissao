package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.Faturamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface FaturamentoRepository extends JpaRepository<Faturamento, Long> {

    @Query("SELECT SUM(f.valor) FROM Faturamento f WHERE f.dataFaturamento BETWEEN :startDate AND :endDate")
    Optional<BigDecimal> sumValorByDataFaturamentoBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
