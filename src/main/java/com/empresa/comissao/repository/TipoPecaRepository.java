package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.TipoPeca;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoPecaRepository extends JpaRepository<TipoPeca, Long> {
    java.util.List<TipoPeca> findByEmpresa(com.empresa.comissao.domain.entity.Empresa empresa);
}
