package com.empresa.comissao.repository;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByEmpresa(Empresa empresa);

    List<User> findByEmpresaAndRole(Empresa empresa, com.empresa.comissao.domain.enums.Role role);

    List<User> findByLicencaAndRole(com.empresa.comissao.domain.entity.Licenca licenca,
            com.empresa.comissao.domain.enums.Role role);

    long countByEmpresaLicencaId(Long licencaId);

    @org.springframework.data.jpa.repository.Query("""
                SELECT new com.empresa.comissao.security.AuthVersionService$UserAuthSnapshot(
                    u.id, u.authVersion, u.active
                )
                FROM User u
                WHERE u.id = :userId
            """)
    java.util.Optional<com.empresa.comissao.security.AuthVersionService.UserAuthSnapshot> findAuthVersionById(
            @org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    @org.springframework.data.jpa.repository.Query("UPDATE User u SET u.authVersion = u.authVersion + 1 WHERE u.id = :userId")
    void incrementAuthVersion(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT u.id FROM User u WHERE u.empresa.id = :empresaId")
    java.util.List<Long> findUserIdsByEmpresaId(
            @org.springframework.data.repository.query.Param("empresaId") Long empresaId);
}
