package com.empresa.comissao.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Principal leve (não é Entity JPA).
 * Carregado do token, não do banco.
 */
@Data
@AllArgsConstructor
public class AuthPrincipal {
    private Long userId;
    private String email;
    private Long tenantId;

    public Long getTenantId() {
        return tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }
}
