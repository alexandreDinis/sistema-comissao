package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.FaturaTenant;
import com.empresa.comissao.domain.enums.StatusFatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FaturaTenantRepository extends JpaRepository<FaturaTenant, Long> {

    boolean existsByEmpresaIdAndMesReferencia(Long empresaId, String mesReferencia);

    List<FaturaTenant> findByStatusAndDataVencimentoBefore(StatusFatura status, LocalDate data);

    Optional<FaturaTenant> findByPaymentId(String paymentId);

    long countByLicencaIdAndStatus(Long licencaId, StatusFatura status); // for Dashboard check
}
