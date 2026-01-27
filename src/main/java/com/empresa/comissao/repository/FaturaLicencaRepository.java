package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.FaturaLicenca;
import com.empresa.comissao.domain.enums.StatusFatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface FaturaLicencaRepository extends JpaRepository<FaturaLicenca, Long> {

    boolean existsByLicencaIdAndMesReferencia(Long licencaId, String mesReferencia);

    List<FaturaLicenca> findByStatusAndDataVencimentoBefore(StatusFatura status, LocalDate data);
}
