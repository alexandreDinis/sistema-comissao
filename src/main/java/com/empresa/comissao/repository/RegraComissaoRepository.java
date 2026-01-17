package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.RegraComissao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegraComissaoRepository extends JpaRepository<RegraComissao, Long> {

    /**
     * Busca todas as regras de uma empresa.
     */
    List<RegraComissao> findByEmpresaOrderByDataInicioDesc(Empresa empresa);

    /**
     * Busca todas as regras de uma empresa pelo ID.
     */
    List<RegraComissao> findByEmpresaIdOrderByDataInicioDesc(Long empresaId);

    /**
     * Busca a regra ativa de uma empresa.
     */
    Optional<RegraComissao> findByEmpresaAndAtivoTrue(Empresa empresa);

    /**
     * Busca a regra ativa de uma empresa pelo ID.
     */
    Optional<RegraComissao> findByEmpresaIdAndAtivoTrue(Long empresaId);

    /**
     * Busca a regra ativa válida para uma data específica.
     */
    @Query("SELECT r FROM RegraComissao r WHERE r.empresa = :empresa " +
            "AND r.ativo = true " +
            "AND r.dataInicio <= :data " +
            "AND (r.dataFim IS NULL OR r.dataFim >= :data)")
    Optional<RegraComissao> findActiveByEmpresaAndDate(
            @Param("empresa") Empresa empresa,
            @Param("data") LocalDate data);

    /**
     * Verifica se existe outra regra ativa para a mesma empresa (exceto a atual).
     */
    @Query("SELECT COUNT(r) > 0 FROM RegraComissao r " +
            "WHERE r.empresa.id = :empresaId AND r.ativo = true AND r.id != :regraId")
    boolean existsAnotherActiveByEmpresaId(
            @Param("empresaId") Long empresaId,
            @Param("regraId") Long regraId);

    /**
     * Busca regras com suas faixas carregadas (eager).
     */
    @Query("SELECT DISTINCT r FROM RegraComissao r LEFT JOIN FETCH r.faixas WHERE r.empresa = :empresa AND r.ativo = true")
    Optional<RegraComissao> findActiveWithFaixasByEmpresa(@Param("empresa") Empresa empresa);
}
