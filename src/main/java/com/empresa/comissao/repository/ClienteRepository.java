package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Cliente> {
    Optional<Cliente> findByCnpj(String cnpj);

    // Multi-tenant: busca CNPJ dentro da empresa específica
    Optional<Cliente> findByCnpjAndEmpresa(String cnpj, com.empresa.comissao.domain.entity.Empresa empresa);
}
