package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

import com.empresa.comissao.domain.enums.Plano;
import com.empresa.comissao.domain.enums.StatusEmpresa;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    Optional<Empresa> findByNome(String nome);

    long countByAtivoTrue();

    long countByPlano(Plano plano);

    long countByLicencaIdAndStatus(Long licencaId, StatusEmpresa status);

    long countByLicencaId(Long licencaId);

    java.util.List<Empresa> findByLicencaId(Long licencaId);

    java.util.List<Empresa> findByLicenca(com.empresa.comissao.domain.entity.Licenca licenca);

    java.util.List<Empresa> findByLicencaIdAndStatus(Long licencaId, StatusEmpresa status);

    // Stats for Owner Dashboard
    @Query("SELECT SUM(e.valorMensalPago) FROM Empresa e WHERE e.licenca.id = :licencaId")
    BigDecimal sumValorMensalPagoByLicencaId(@Param("licencaId") Long licencaId);

    // Orphan tenants (no reseller, direct owner management)
    java.util.List<Empresa> findByLicencaIsNull();

    long countByLicencaIsNull();

    // Risk Management: Tenants linked to suspended/cancelled resellers
    @Query("SELECT e FROM Empresa e WHERE e.licenca.status IN ('SUSPENSA', 'CANCELADA')")
    java.util.List<Empresa> findEmpresasComRevendedorBloqueado();
}
