package com.empresa.comissao.security;

import com.empresa.comissao.domain.entity.Feature;
import com.empresa.comissao.domain.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    @Value("${security.jwt.secret-key}")
    private String secretKey;

    @Value("${security.jwt.expiration:86400000}") // 24h default
    private long jwtExpiration;

    /**
     * Extrai userId (subject) do token.
     */
    public Long extractUserId(String token) {
        try {
            return Long.parseLong(extractClaim(token, Claims::getSubject));
        } catch (NumberFormatException e) {
            log.warn("Token com subject inválido (não é Long): {}", extractClaim(token, Claims::getSubject));
            return null;
        }
    }

    /**
     * Extrai claim genérico.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Gera token com contexto completo (userId, empresaId, roles, features,
     * versões).
     * ✅ NOVO: Token "gordo" com snapshot do estado atual.
     */
    public String generateToken(UserDetails userDetails) {
        if (!(userDetails instanceof User)) {
            throw new IllegalArgumentException("UserDetails deve ser instância de User");
        }

        User user = (User) userDetails;
        Map<String, Object> claims = new HashMap<>();

        // ✅ Contexto de tenant
        if (user.getEmpresa() != null) {
            claims.put("tid", user.getEmpresa().getId()); // Tenant ID
            claims.put("v_t", user.getEmpresa().getTenantVersion()); // Tenant Version
        }

        // ✅ Roles
        claims.put("roles", List.of("ROLE_" + user.getRole().name()));

        // ✅ Features (converter para lista de strings)
        List<String> featureCodes = user.getFeatures().stream()
                .map(Feature::getCodigo)
                .collect(Collectors.toList());
        claims.put("feats", featureCodes);

        // ✅ Versões de auth
        claims.put("v_u", user.getAuthVersion()); // User Auth Version

        // ✅ Email para display
        claims.put("email", user.getEmail());

        log.info("[JWT] Gerando token para userId={}, empresaId={}, roles={}, features={}",
                user.getId(),
                user.getEmpresa() != null ? user.getEmpresa().getId() : null,
                claims.get("roles"),
                claims.get("feats"));

        return generateToken(claims, user.getId().toString());
    }

    /**
     * Gera token com claims customizados.
     */
    public String generateToken(Map<String, Object> extraClaims, String subject) {
        return Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(subject) // userId como string
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Valida token (assinatura + expiração).
     * ✅ NOVO: Não valida contra UserDetails (isso vem depois no filtro).
     */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.warn("[JWT] Token inválido: {}", e.getMessage());
            return false;
        }
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
