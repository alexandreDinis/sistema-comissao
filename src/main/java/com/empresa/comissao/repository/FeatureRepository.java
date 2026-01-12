package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.Feature;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FeatureRepository extends JpaRepository<Feature, Long> {
    Optional<Feature> findByCodigo(String codigo);
}
