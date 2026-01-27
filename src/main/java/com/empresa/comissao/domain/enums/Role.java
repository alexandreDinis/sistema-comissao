package com.empresa.comissao.domain.enums;

public enum Role {
    USER, // Deprecated, kept for retro-compatibility if needed
    ADMIN, // Deprecated
    ADMIN_EMPRESA,
    FUNCIONARIO,
    ADMIN_LICENCA, // Owner of a White Label License (Reseller Admin)
    REVENDEDOR, // Alias or sub-role if needed
    SUPER_ADMIN
}
