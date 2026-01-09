package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.VeiculoServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VeiculoServicoRepository extends JpaRepository<VeiculoServico, Long> {
    boolean existsByPlaca(String placa);

    java.util.List<VeiculoServico> findByPlacaOrderByOrdemServicoDataDesc(String placa);
}
