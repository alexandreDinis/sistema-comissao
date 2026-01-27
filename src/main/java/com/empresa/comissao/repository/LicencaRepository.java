package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.Licenca;
import com.empresa.comissao.domain.enums.StatusLicenca;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LicencaRepository extends JpaRepository<Licenca, Long> {

    boolean existsByCnpj(String cnpj);

    Optional<Licenca> findByCnpj(String cnpj);

    Page<Licenca> findByStatus(StatusLicenca status, Pageable pageable);

    List<Licenca> findByStatus(StatusLicenca status);

    long countByStatus(StatusLicenca status);
}
