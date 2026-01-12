package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import com.empresa.comissao.domain.enums.Plano;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    Optional<Empresa> findByNome(String nome);

    long countByAtivoTrue();

    long countByPlano(Plano plano);
}
