package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.ComissaoCalculada;
import com.empresa.comissao.domain.entity.Empresa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import com.empresa.comissao.domain.entity.User;

@Repository
public interface ComissaoCalculadaRepository extends JpaRepository<ComissaoCalculada, Long> {

    Optional<ComissaoCalculada> findFirstByAnoMesReferencia(YearMonth anoMesReferencia);

    Optional<ComissaoCalculada> findFirstByAnoMesReferenciaAndUsuario(YearMonth anoMesReferencia, User usuario);

    Optional<ComissaoCalculada> findFirstByAnoMesReferenciaAndEmpresaAndUsuarioIsNull(YearMonth anoMesReferencia,
            Empresa empresa);

    // For cleanup: find all for empresa (including user-specific ones)
    List<ComissaoCalculada> findByAnoMesReferenciaAndEmpresa(YearMonth anoMesReferencia, Empresa empresa);
}
