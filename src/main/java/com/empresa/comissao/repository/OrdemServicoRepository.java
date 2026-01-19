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

        @org.springframework.data.jpa.repository.Query("SELECT new com.empresa.comissao.dto.response.RankingClienteDTO(c.id, c.nomeFantasia, COUNT(os), SUM(os.valorTotal)) "
                        +
                        "FROM OrdemServico os JOIN os.cliente c " +
                        "WHERE os.empresa.id = :empresaId " +
                        "AND os.status = 'FINALIZADA' " +
                        "AND YEAR(os.data) = :ano " +
                        "AND (:mes IS NULL OR MONTH(os.data) = :mes) " +
                        "GROUP BY c.id, c.nomeFantasia " +
                        "ORDER BY SUM(os.valorTotal) DESC")
        List<com.empresa.comissao.dto.response.RankingClienteDTO> findRankingClientes(
                        @org.springframework.data.repository.query.Param("empresaId") Long empresaId,
                        @org.springframework.data.repository.query.Param("ano") int ano,
                        @org.springframework.data.repository.query.Param("mes") Integer mes);

}
