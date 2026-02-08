package com.empresa.comissao.config;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TenantContext {
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

    public static void setCurrentTenant(Long tenantId) {
        // Log is too verbose for every request if level is debug, but fine for now
        // log.debug("[TenantContext] Setting tenant: {}", tenantId);
        TENANT_ID.set(tenantId);
    }

    public static Long getCurrentTenant() {
        return TENANT_ID.get();
    }

    public static void clear() {
        TENANT_ID.remove();
    }
}
