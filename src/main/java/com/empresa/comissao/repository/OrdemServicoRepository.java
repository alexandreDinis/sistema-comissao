package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.OrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {
        List<OrdemServico> findByClienteId(Long clienteId);

        List<OrdemServico> findByEmpresa(com.empresa.comissao.domain.entity.Empresa empresa);

        long countByStatusInAndEmpresa(
                        java.util.Collection<com.empresa.comissao.domain.enums.StatusOrdemServico> statuses,
                        com.empresa.comissao.domain.entity.Empresa empresa);

        long countByStatusAndDataBetweenAndEmpresa(com.empresa.comissao.domain.enums.StatusOrdemServico status,
                        java.time.LocalDate start, java.time.LocalDate end,
                        com.empresa.comissao.domain.entity.Empresa empresa);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(v) FROM OrdemServico o JOIN o.veiculos v WHERE o.status = :status AND o.data BETWEEN :start AND :end AND o.empresa = :empresa")
        long countVeiculosByStatusAndDataAndEmpresa(
                        @org.springframework.data.repository.query.Param("status") com.empresa.comissao.domain.enums.StatusOrdemServico status,
                        @org.springframework.data.repository.query.Param("start") java.time.LocalDate start,
                        @org.springframework.data.repository.query.Param("end") java.time.LocalDate end,
                        @org.springframework.data.repository.query.Param("empresa") com.empresa.comissao.domain.entity.Empresa empresa);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM OrdemServico o JOIN o.veiculos v JOIN v.pecas p WHERE o.status = :status AND o.data BETWEEN :start AND :end AND o.empresa = :empresa")
        long countPecasByStatusAndDataAndEmpresa(
                        @org.springframework.data.repository.query.Param("status") com.empresa.comissao.domain.enums.StatusOrdemServico status,
                        @org.springframework.data.repository.query.Param("start") java.time.LocalDate start,
                        @org.springframework.data.repository.query.Param("end") java.time.LocalDate end,
                        @org.springframework.data.repository.query.Param("empresa") com.empresa.comissao.domain.entity.Empresa empresa);

        @org.springframework.data.jpa.repository.Query("SELECT new com.empresa.comissao.dto.response.RankingClienteDTO(c.id, COALESCE(c.nomeFantasia, c.razaoSocial), COUNT(os), SUM(os.valorTotal)) "
                        +
                        "FROM OrdemServico os JOIN os.cliente c " +
                        "WHERE os.empresa.id = :empresaId " +
                        "AND os.status = 'FINALIZADA' " +
                        "AND os.data BETWEEN :start AND :end " +
                        "GROUP BY c.id, c.nomeFantasia, c.razaoSocial " +
                        "ORDER BY SUM(os.valorTotal) DESC")
        List<com.empresa.comissao.dto.response.RankingClienteDTO> findRankingClientes(
                        @org.springframework.data.repository.query.Param("empresaId") Long empresaId,
                        @org.springframework.data.repository.query.Param("start") java.time.LocalDate start,
                        @org.springframework.data.repository.query.Param("end") java.time.LocalDate end);

        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT os FROM OrdemServico os LEFT JOIN FETCH os.cliente LEFT JOIN FETCH os.veiculos v LEFT JOIN FETCH v.pecas p WHERE os.updatedAt > :since AND os.empresa.id = :empresaId")
        List<OrdemServico> findSyncData(
                        @org.springframework.data.repository.query.Param("empresaId") Long empresaId,
                        @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since);

        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT os FROM OrdemServico os LEFT JOIN FETCH os.cliente LEFT JOIN FETCH os.veiculos v LEFT JOIN FETCH v.pecas p WHERE os.empresa.id = :empresaId")
        List<OrdemServico> findAllFullSync(
                        @org.springframework.data.repository.query.Param("empresaId") Long empresaId);

        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT os FROM OrdemServico os LEFT JOIN FETCH os.cliente LEFT JOIN FETCH os.veiculos v LEFT JOIN FETCH v.pecas p WHERE os.updatedAt > :since AND os.empresa.id = :empresaId AND os.usuario.id = :usuarioId")
        List<OrdemServico> findSyncDataByUsuario(
                        @org.springframework.data.repository.query.Param("empresaId") Long empresaId,
                        @org.springframework.data.repository.query.Param("since") java.time.LocalDateTime since,
                        @org.springframework.data.repository.query.Param("usuarioId") Long usuarioId);

        @org.springframework.data.jpa.repository.Query("SELECT DISTINCT os FROM OrdemServico os LEFT JOIN FETCH os.cliente LEFT JOIN FETCH os.veiculos v LEFT JOIN FETCH v.pecas p WHERE os.empresa.id = :empresaId AND os.usuario.id = :usuarioId")
        List<OrdemServico> findAllFullSyncByUsuario(
                        @org.springframework.data.repository.query.Param("empresaId") Long empresaId,
                        @org.springframework.data.repository.query.Param("usuarioId") Long usuarioId);

        @org.springframework.data.jpa.repository.Query("SELECT MAX(os.updatedAt) FROM OrdemServico os WHERE os.empresa = :empresa")
        java.time.LocalDateTime findMaxUpdatedAtByEmpresa(
                        @org.springframework.data.repository.query.Param("empresa") com.empresa.comissao.domain.entity.Empresa empresa);

        @org.springframework.data.jpa.repository.Query("SELECT MAX(os.updatedAt) FROM OrdemServico os WHERE os.empresa.id = :empresaId")
        java.time.LocalDateTime findMaxUpdatedAtByEmpresaId(
                        @org.springframework.data.repository.query.Param("empresaId") Long empresaId);

        java.util.Optional<OrdemServico> findByLocalIdAndEmpresa(String localId,
                        com.empresa.comissao.domain.entity.Empresa empresa);
}
