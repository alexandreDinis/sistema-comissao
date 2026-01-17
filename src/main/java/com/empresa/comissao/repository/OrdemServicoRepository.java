package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.OrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {
        List<OrdemServico> findByClienteId(Long clienteId);

        List<OrdemServico> findByEmpresa(com.empresa.comissao.domain.entity.Empresa empresa);

        long countByStatusIn(java.util.Collection<com.empresa.comissao.domain.enums.StatusOrdemServico> statuses);

        long countByStatusAndDataBetween(com.empresa.comissao.domain.enums.StatusOrdemServico status,
                        java.time.LocalDate start, java.time.LocalDate end);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(v) FROM OrdemServico o JOIN o.veiculos v WHERE o.status = :status AND o.data BETWEEN :start AND :end")
        long countVeiculosByStatusAndData(
                        @org.springframework.data.repository.query.Param("status") com.empresa.comissao.domain.enums.StatusOrdemServico status,
                        @org.springframework.data.repository.query.Param("start") java.time.LocalDate start,
                        @org.springframework.data.repository.query.Param("end") java.time.LocalDate end);

        @org.springframework.data.jpa.repository.Query("SELECT COUNT(p) FROM OrdemServico o JOIN o.veiculos v JOIN v.pecas p WHERE o.status = :status AND o.data BETWEEN :start AND :end")
        long countPecasByStatusAndData(
                        @org.springframework.data.repository.query.Param("status") com.empresa.comissao.domain.enums.StatusOrdemServico status,
                        @org.springframework.data.repository.query.Param("start") java.time.LocalDate start,
                        @org.springframework.data.repository.query.Param("end") java.time.LocalDate end);

}
