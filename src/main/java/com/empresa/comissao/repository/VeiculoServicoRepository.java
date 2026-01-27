package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.VeiculoServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface VeiculoServicoRepository extends JpaRepository<VeiculoServico, Long> {
    boolean existsByPlaca(String placa);

    java.util.List<VeiculoServico> findByPlacaOrderByOrdemServicoDataDesc(String placa);

    java.util.List<VeiculoServico> findByPlacaIgnoreCase(String placa);

    // Tenant-isolated queries
    @Query("SELECT v FROM VeiculoServico v WHERE UPPER(v.placa) = UPPER(:placa) AND v.ordemServico.empresa = :empresa")
    java.util.List<VeiculoServico> findByPlacaIgnoreCaseAndEmpresa(@Param("placa") String placa,
            @Param("empresa") Empresa empresa);

    @Query("SELECT v FROM VeiculoServico v WHERE v.placa = :placa AND v.ordemServico.empresa = :empresa ORDER BY v.ordemServico.data DESC")
    java.util.List<VeiculoServico> findByPlacaAndEmpresaOrderByOrdemServicoDataDesc(@Param("placa") String placa,
            @Param("empresa") Empresa empresa);
}
