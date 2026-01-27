package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.FaixaComissaoConfig;
import com.empresa.comissao.domain.entity.RegraComissao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FaixaComissaoConfigRepository extends JpaRepository<FaixaComissaoConfig, Long> {

    /**
     * Busca todas as faixas de uma regra, ordenadas.
     */
    List<FaixaComissaoConfig> findByRegraOrderByOrdemAsc(RegraComissao regra);

    /**
     * Busca todas as faixas de uma regra pelo ID, ordenadas.
     */
    List<FaixaComissaoConfig> findByRegraIdOrderByOrdemAsc(Long regraId);

    /**
     * Deleta todas as faixas de uma regra.
     */
    void deleteByRegraId(Long regraId);

    /**
     * Conta o número de faixas de uma regra.
     */
    @Query("SELECT COUNT(f) FROM FaixaComissaoConfig f WHERE f.regra.id = :regraId")
    long countByRegraId(@Param("regraId") Long regraId);
}
