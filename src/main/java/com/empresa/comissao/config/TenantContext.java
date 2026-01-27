package com.empresa.comissao.config;

public class TenantContext {
    private static final ThreadLocal<Long> currentTenantId = new ThreadLocal<>();
    private static final ThreadLocal<Long> currentLicencaId = new ThreadLocal<>();

    public static void setCurrentTenant(Long tenantId) {
        currentTenantId.set(tenantId);
    }

    public static Long getCurrentTenant() {
        return currentTenantId.get();
    }

    public static void setCurrentLicenca(Long licencaId) {
        currentLicencaId.set(licencaId);
    }

    public static Long getCurrentLicenca() {
        return currentLicencaId.get();
    }

    public static void clear() {
        currentTenantId.remove();
        currentLicencaId.remove();
    }
}
