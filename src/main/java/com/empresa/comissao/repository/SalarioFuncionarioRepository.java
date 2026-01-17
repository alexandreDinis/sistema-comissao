package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.SalarioFuncionario;
import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.domain.enums.TipoRemuneracao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalarioFuncionarioRepository extends JpaRepository<SalarioFuncionario, Long> {

    /**
     * Busca todas as configurações de salário de uma empresa.
     */
    List<SalarioFuncionario> findByEmpresaOrderByDataInicioDesc(Empresa empresa);

    /**
     * Busca todas as configurações de salário de uma empresa pelo ID.
     */
    List<SalarioFuncionario> findByEmpresaIdOrderByDataInicioDesc(Long empresaId);

    /**
     * Busca a configuração de salário ativa de um funcionário.
     */
    Optional<SalarioFuncionario> findByUsuarioAndAtivoTrue(User usuario);

    /**
     * Busca a configuração de salário ativa de um funcionário pelo ID.
     */
    Optional<SalarioFuncionario> findByUsuarioIdAndAtivoTrue(Long usuarioId);

    /**
     * Busca a configuração de salário ativa válida para uma data específica.
     */
    @Query("SELECT s FROM SalarioFuncionario s WHERE s.usuario = :usuario " +
            "AND s.ativo = true " +
            "AND s.dataInicio <= :data " +
            "AND (s.dataFim IS NULL OR s.dataFim >= :data)")
    Optional<SalarioFuncionario> findActiveByUsuarioAndDate(
            @Param("usuario") User usuario,
            @Param("data") LocalDate data);

    /**
     * Busca por empresa e funcionário.
     */
    List<SalarioFuncionario> findByEmpresaIdAndUsuarioId(Long empresaId, Long usuarioId);

    /**
     * Busca funcionários com um tipo específico de remuneração.
     */
    List<SalarioFuncionario> findByEmpresaIdAndTipoRemuneracaoAndAtivoTrue(
            Long empresaId, TipoRemuneracao tipoRemuneracao);

    /**
     * Conta funcionários por tipo de remuneração.
     */
    @Query("SELECT s.tipoRemuneracao, COUNT(s) FROM SalarioFuncionario s " +
            "WHERE s.empresa.id = :empresaId AND s.ativo = true " +
            "GROUP BY s.tipoRemuneracao")
    List<Object[]> countByTipoRemuneracaoAndEmpresaId(@Param("empresaId") Long empresaId);

    /**
     * Verifica se existe configuração ativa para um funcionário (exceto a atual).
     */
    @Query("SELECT COUNT(s) > 0 FROM SalarioFuncionario s " +
            "WHERE s.usuario.id = :usuarioId AND s.ativo = true AND s.id != :id")
    boolean existsAnotherActiveByUsuarioId(
            @Param("usuarioId") Long usuarioId,
            @Param("id") Long id);
}
