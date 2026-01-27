package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.ComissaoFixaFuncionario;
import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ComissaoFixaFuncionarioRepository extends JpaRepository<ComissaoFixaFuncionario, Long> {

    /**
     * Busca todas as configurações de comissão fixa de uma empresa.
     */
    List<ComissaoFixaFuncionario> findByEmpresaOrderByDataInicioDesc(Empresa empresa);

    /**
     * Busca todas as configurações de comissão fixa de uma empresa pelo ID.
     */
    List<ComissaoFixaFuncionario> findByEmpresaIdOrderByDataInicioDesc(Long empresaId);

    /**
     * Busca a comissão fixa ativa de um funcionário.
     */
    Optional<ComissaoFixaFuncionario> findByUsuarioAndAtivoTrue(User usuario);

    /**
     * Busca a comissão fixa ativa de um funcionário pelo ID.
     */
    Optional<ComissaoFixaFuncionario> findByUsuarioIdAndAtivoTrue(Long usuarioId);

    /**
     * Busca a comissão fixa ativa válida para uma data específica.
     */
    @Query("SELECT c FROM ComissaoFixaFuncionario c WHERE c.usuario = :usuario " +
            "AND c.ativo = true " +
            "AND c.dataInicio <= :data " +
            "AND (c.dataFim IS NULL OR c.dataFim >= :data)")
    Optional<ComissaoFixaFuncionario> findActiveByUsuarioAndDate(
            @Param("usuario") User usuario,
            @Param("data") LocalDate data);

    /**
     * Busca por empresa e funcionário.
     */
    List<ComissaoFixaFuncionario> findByEmpresaIdAndUsuarioId(Long empresaId, Long usuarioId);

    /**
     * Verifica se existe comissão fixa ativa para um funcionário (exceto a atual).
     */
    @Query("SELECT COUNT(c) > 0 FROM ComissaoFixaFuncionario c " +
            "WHERE c.usuario.id = :usuarioId AND c.ativo = true AND c.id != :id")
    boolean existsAnotherActiveByUsuarioId(
            @Param("usuarioId") Long usuarioId,
            @Param("id") Long id);
}
