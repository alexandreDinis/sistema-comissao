package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.Despesa;
import com.empresa.comissao.domain.enums.CategoriaDespesa;
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

    List<Despesa> findByDataDespesaBetween(LocalDate start, LocalDate end);

    @Query("SELECT SUM(d.valor) FROM Despesa d WHERE d.dataDespesa BETWEEN :start AND :end")
    Optional<BigDecimal> sumValorByDataDespesaBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT d.categoria, SUM(d.valor) FROM Despesa d WHERE d.dataDespesa BETWEEN :start AND :end GROUP BY d.categoria")
    List<Object[]> sumValorByCategoriaAndDataDespesaBetween(@Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
