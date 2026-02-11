package com.empresa.comissao.security;

import com.empresa.comissao.config.TenantContext;
import com.empresa.comissao.domain.enums.StatusEmpresa;
import com.empresa.comissao.domain.enums.StatusLicenca;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AuthVersionService authVersionService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        // 1) Extrair token do header
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final Long userId;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Check query param for file downloads if needed
            final String queryToken = request.getParameter("token");
            if (queryToken != null && !queryToken.isEmpty()) {
                jwt = queryToken;
            } else {
                log.debug("[JWT] Sem token no request: {}", request.getRequestURI());
                filterChain.doFilter(request, response);
                return;
            }
        } else {
            jwt = authHeader.substring(7);
        }

        // 2) Validar assinatura + expiração
        if (!jwtService.isTokenValid(jwt)) {
            log.warn("[JWT] Token inválido ou expirado");
            // Proceed without auth (let Spring Security handle 401 if needed) or return 401
            // immediately?
            // Usually if token is invalid but present, we might want to return 401.
            // But strict standard is: if auth fails, context is empty. Endpoints protected
            // by config will fail.
            // However, blueprint says "return 401".
            // Let's keep it safe: continue filter chain but with empty context.
            // OR if the blueprint explicitly wrote response...
            // Blueprint: response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); return;
            // This is "gatekeeper" style.
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Token inválido ou expirado\"}");
            return;
        }

        // 3) Extrair userId (subject)
        userId = jwtService.extractUserId(jwt);
        if (userId == null) {
            log.warn("[JWT] Não conseguiu extrair userId do token");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Token malformado\"}");
            return;
        }

        // 4) Extrair claims do token
        Claims claims = jwtService.extractAllClaims(jwt);
        Long tenantId = claims.get("tid", Long.class);
        Integer userVersionToken = claims.get("v_u", Integer.class);

        // Fix: Handle v_t as Long safely (it might be Integer in token)
        Number tenantVersionClaim = claims.get("v_t", Number.class);
        Long tenantVersionToken = tenantVersionClaim != null ? tenantVersionClaim.longValue() : null;

        @SuppressWarnings("unchecked")
        List<String> rolesToken = (List<String>) claims.get("roles");
        @SuppressWarnings("unchecked")
        List<String> featsToken = (List<String>) claims.get("feats");
        String email = claims.get("email", String.class);

        log.debug("[JWT] Token extraído: userId={}, tenantId={}, roles={}, features={}",
                userId, tenantId, rolesToken, featsToken);

        // 5) ✅ VALIDAR VERSÃO DE AUTH (cache hit esperado)
        AuthVersionService.UserAuthSnapshot userSnapshot = authVersionService.getUserAuthVersion(userId);
        if (userSnapshot == null || !userSnapshot.isActive()) {
            log.warn("[JWT] Usuário não encontrado ou inativo: userId={}", userId);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Usuário não encontrado ou inativo\"}");
            return;
        }

        if (userVersionToken != null && !userVersionToken.equals(userSnapshot.getAuthVersion())) {
            // Handle nullable version in token (legacy support if we wanted, but we are
            // enforcing new scheme)
            // If token has no version, it's old -> reject? Or accept as 0?
            // Blueprint assumes consistency.
            // If user tokens are generated with new logic, they have version.
            // If version mismatch:
            log.warn("[JWT] Auth version mismatch: token={}, server={}. Forçando refresh.",
                    userVersionToken, userSnapshot.getAuthVersion());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Permissões alteradas. Faça login novamente.\"}");
            return;
        }

        // 6) ✅ VALIDAR VERSÃO DE TENANT (cache hit esperado)
        if (tenantId != null) {
            AuthVersionService.TenantAccessSnapshot tenantSnapshot = authVersionService
                    .getTenantAccessVersion(tenantId);
            if (tenantSnapshot == null) {
                log.warn("[JWT] Tenant não encontrado: tenantId={}", tenantId);
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Tenant não encontrado\"}");
                return;
            }

            // Verificar bloqueio/suspensão
            if (tenantSnapshot.getStatus() == StatusEmpresa.BLOQUEADA) {
                log.warn("[JWT] Tenant bloqueado: tenantId={}", tenantId);
                response.setStatus(402); // Payment Required
                response.getWriter().write("{\"error\": \"Acesso bloqueado por inadimplência\"}");
                return;
            }

            if (tenantSnapshot.getLicencaStatus() == StatusLicenca.SUSPENSA) {
                log.warn("[JWT] Licença suspensa: tenantId={}", tenantId);
                response.setStatus(402);
                response.getWriter().write("{\"error\": \"Acesso suspenso pelo administrador\"}");
                return;
            }

            // Verificar version mismatch
            if (tenantVersionToken != null && !tenantVersionToken.equals(tenantSnapshot.getTenantVersion())) {
                log.warn("[JWT] Tenant version mismatch: token={}, server={}. Forçando refresh.",
                        tenantVersionToken, tenantSnapshot.getTenantVersion());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Plano/status alterado. Faça login novamente.\"}");
                return;
            }

            // Setar TenantContext
            TenantContext.setCurrentTenant(tenantId);
        }

        // 7) ✅ MONTAR AUTHENTICATION (sem carregar User do banco)
        List<GrantedAuthority> authorities = rolesToken.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        // Adicionar features como authorities
        if (featsToken != null) {
            featsToken.forEach(feat -> authorities.add(new SimpleGrantedAuthority("FEATURE_" + feat)));
        }

        // Criar principal leve (DTO, não Entity)
        AuthPrincipal principal = new AuthPrincipal(userId, email, tenantId);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                authorities);

        SecurityContextHolder.getContext().setAuthentication(authentication);

        log.debug("[JWT] Autenticação bem-sucedida: userId={}, tenantId={}, authorities={}",
                userId, tenantId, authorities.size());

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        return path.startsWith("/api/v1/auth/")
                || path.startsWith("/api/v1/webhooks/")
                || path.equals("/health")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }
}
