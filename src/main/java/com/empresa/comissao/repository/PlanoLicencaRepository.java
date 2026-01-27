package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.PlanoLicenca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanoLicencaRepository extends JpaRepository<PlanoLicenca, Long> {
    List<PlanoLicenca> findByAtivoTrueOrderByOrdemAsc();
}
