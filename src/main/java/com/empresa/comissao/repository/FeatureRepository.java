package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.Feature;
import com.empresa.comissao.domain.enums.Plano;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeatureRepository extends JpaRepository<Feature, Long> {
    Optional<Feature> findByCodigo(String codigo);

    /**
     * Finds all features available for specific plan levels.
     */
    List<Feature> findByPlanoMinimoIn(List<Plano> planos);
}
