package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.Prestador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrestadorRepository extends JpaRepository<Prestador, Long> {

    List<Prestador> findByEmpresaAndAtivoTrueOrderByNomeAsc(Empresa empresa);

    List<Prestador> findByEmpresaOrderByNomeAsc(Empresa empresa);
}
