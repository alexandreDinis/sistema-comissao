package com.empresa.comissao.config;

import com.empresa.comissao.domain.enums.StatusEmpresa;
import com.empresa.comissao.domain.enums.StatusLicenca;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class TenantAccessInterceptor implements HandlerInterceptor {

    private final com.empresa.comissao.security.AuthVersionService authVersionService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // Ignorar endpoints públicos (login, webhooks, paginas de erro) e Swagger
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/auth") ||
                path.startsWith("/api/v1/webhooks") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs")) {
            return true;
        }

        Long tenantId = TenantContext.getCurrentTenant();

        if (tenantId != null) {
            // ✅ USAR CACHE: 0 DB Hits se estiver quente
            var snapshot = authVersionService.getTenantAccessVersion(tenantId);

            if (snapshot != null) {
                // 1. Verificar se Tenant está bloqueado
                if (snapshot.getStatus() == StatusEmpresa.BLOQUEADA) {
                    bloquearAcesso(response, "Acesso bloqueado por inadimplência da empresa.");
                    return false;
                }

                // 2. Verificar se Licenca (Revendedor) está suspensa
                if (snapshot.getLicencaStatus() == StatusLicenca.SUSPENSA) {
                    bloquearAcesso(response, "Acesso suspenso pelo administrador do sistema.");
                    return false;
                }
            }
        }

        return true;
    }

    private void bloquearAcesso(HttpServletResponse response, String msg) throws java.io.IOException {
        response.setStatus(402); // Payment Required
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"" + msg + "\"}");
    }
}
