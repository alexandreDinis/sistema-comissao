package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.CartaoCredito;
import com.empresa.comissao.domain.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartaoCreditoRepository extends JpaRepository<CartaoCredito, Long> {

    List<CartaoCredito> findByEmpresaAndAtivoTrueOrderByNomeAsc(Empresa empresa);

    List<CartaoCredito> findByEmpresaOrderByNomeAsc(Empresa empresa);
}
