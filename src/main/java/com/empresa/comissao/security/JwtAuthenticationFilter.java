package com.empresa.comissao.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        try {
            jwt = authHeader.substring(7).trim();
            userEmail = jwtService.extractUsername(jwt);
            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);
                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities());
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // Set Tenant Context
                    if (userDetails instanceof com.empresa.comissao.domain.entity.User) {
                        com.empresa.comissao.domain.entity.Empresa empresa = ((com.empresa.comissao.domain.entity.User) userDetails)
                                .getEmpresa();
                        if (empresa != null) {
                            com.empresa.comissao.config.TenantContext.setTenant(empresa.getId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Log error if needed, but don't crash the request.
            // Proceed without authentication (will be 401/403 downstream).
        } finally {
            // We do NOT clear validation here because the request is still processing.
            // Ideally we should clear AFTER the controller returns, but OnePerRequestFilter
            // wraps the whole chain. Let's explicitly clear in a try-finally block AROUND
            // the doFilter.
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            com.empresa.comissao.config.TenantContext.clear();
        }
    }
}
