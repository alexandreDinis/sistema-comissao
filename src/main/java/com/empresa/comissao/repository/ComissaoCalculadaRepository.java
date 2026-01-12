package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.ComissaoCalculada;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.YearMonth;
import java.util.Optional;

import com.empresa.comissao.domain.entity.User;

@Repository
public interface ComissaoCalculadaRepository extends JpaRepository<ComissaoCalculada, Long> {

    Optional<ComissaoCalculada> findByAnoMesReferencia(YearMonth anoMesReferencia);

    Optional<ComissaoCalculada> findByAnoMesReferenciaAndUsuario(YearMonth anoMesReferencia, User usuario);
}
