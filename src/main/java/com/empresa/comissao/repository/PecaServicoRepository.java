package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.PecaServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PecaServicoRepository extends JpaRepository<PecaServico, Long> {
    java.util.Optional<PecaServico> findByLocalIdAndVeiculo_OrdemServico_Empresa(String localId,
            com.empresa.comissao.domain.entity.Empresa empresa);
}
