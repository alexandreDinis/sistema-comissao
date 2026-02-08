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

        // Delta Sync Query: Retorna todos (ativos e deletados) alterados após 'since'
        @org.springframework.data.jpa.repository.Query("SELECT c FROM Cliente c WHERE c.updatedAt > :since AND c.empresa.id = :empresaId")
        java.util.List<Cliente> findSyncData(
                        @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since,
                        @org.springframework.data.repository.query.Param("empresaId") Long empresaId);

        @org.springframework.data.jpa.repository.Query("SELECT c FROM Cliente c WHERE c.empresa.id = :empresaId")
        java.util.List<Cliente> findAllByEmpresaId(
                        @org.springframework.data.repository.query.Param("empresaId") Long empresaId);

        @org.springframework.data.jpa.repository.Query("SELECT MAX(c.updatedAt) FROM Cliente c WHERE c.empresa = :empresa")
        java.time.LocalDateTime findMaxUpdatedAtByEmpresa(
                        @org.springframework.data.repository.query.Param("empresa") com.empresa.comissao.domain.entity.Empresa empresa);

        @org.springframework.data.jpa.repository.Query("SELECT MAX(c.updatedAt) FROM Cliente c WHERE c.empresa.id = :empresaId")
        java.time.LocalDateTime findMaxUpdatedAtByEmpresaId(
                        @org.springframework.data.repository.query.Param("empresaId") Long empresaId);

        // Soft Delete: Buscar por empresa e não deletado
        @org.springframework.data.jpa.repository.Query("SELECT c FROM Cliente c WHERE c.empresa.id = :empresaId AND c.deletedAt IS NULL")
        java.util.List<Cliente> findByEmpresaIdAndDeletedAtIsNull(
                        @org.springframework.data.repository.query.Param("empresaId") Long empresaId);
}
