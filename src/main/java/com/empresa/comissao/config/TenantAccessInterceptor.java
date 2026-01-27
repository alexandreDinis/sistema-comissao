package com.empresa.comissao.config;

import com.empresa.comissao.domain.entity.Empresa;
import com.empresa.comissao.domain.entity.Licenca;
import com.empresa.comissao.domain.enums.StatusEmpresa;
import com.empresa.comissao.domain.enums.StatusLicenca;
import com.empresa.comissao.repository.EmpresaRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class TenantAccessInterceptor implements HandlerInterceptor {

    private final EmpresaRepository empresaRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        // Ignorar endpoints públicos (login, webhooks, paginas de erro)
        String path = request.getRequestURI();
        if (path.startsWith("/api/v1/webhooks") || path.startsWith("/api/v1/auth")) {
            return true;
        }

        Long tenantId = TenantContext.getCurrentTenant();

        if (tenantId != null) {
            Optional<Empresa> empresaOpt = empresaRepository.findById(tenantId);

            if (empresaOpt.isPresent()) {
                Empresa empresa = empresaOpt.get();

                // 1. Verificar se Tenant está bloqueado
                if (empresa.getStatus() == StatusEmpresa.BLOQUEADA) {
                    bloquearAcesso(response, "Acesso bloqueado por inadimplência da empresa.");
                    return false;
                }

                // 2. Verificar se Licenca (Revendedor) está suspensa
                // Note: Empresa entity must have getLicenca() loaded or we fetch ID
                if (empresa.getLicenca() != null) {
                    Licenca licenca = empresa.getLicenca(); // Hibernate Proxy check might be needed if lazy
                    // Melhor buscar status da licença simples

                    // Se estiver lazy carregado, talvez falhe se session fechada?
                    // Mas repository.findById deve trazer.

                    if (licenca.getStatus() == StatusLicenca.SUSPENSA) {
                        bloquearAcesso(response, "Acesso suspenso pelo administrador do sistema.");
                        return false;
                    }
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
