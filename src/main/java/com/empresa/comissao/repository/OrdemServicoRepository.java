package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.OrdemServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OrdemServicoRepository extends JpaRepository<OrdemServico, Long> {
    List<OrdemServico> findByClienteId(Long clienteId);
}
