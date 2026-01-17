package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.domain.enums.Role;
import com.empresa.comissao.repository.UserRepository;
import com.empresa.comissao.security.JwtService;
import com.empresa.comissao.service.RateLimitingService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

        private final UserRepository repository;
        private final PasswordEncoder passwordEncoder;
        private final JwtService jwtService;
        private final AuthenticationManager authenticationManager;
        private final RateLimitingService rateLimitingService;
        private final com.empresa.comissao.repository.EmpresaRepository empresaRepository;

        @PostMapping("/register")
        public ResponseEntity<AuthenticationResponse> register(
                        @RequestBody RegisterRequest request) {

                var defaultCompany = empresaRepository.findByNome("Empresa Padrão")
                                .orElseThrow(() -> new RuntimeException(
                                                "Default Company not found. Run Migration V11."));

                var user = User.builder()
                                .email(request.getEmail())
                                .password(passwordEncoder.encode(request.getPassword()))
                                .role(Role.USER) // Default role
                                .empresa(defaultCompany)
                                .active(false) // User must be approved by admin
                                .build();
                repository.save(user);
                // We do NOT generate token for inactive user
                return ResponseEntity.ok(AuthenticationResponse.builder()
                                .token("User registered. Please wait for Admin approval.")
                                .build());
        }

        @PostMapping("/login")
        public ResponseEntity<?> authenticate(
                        @RequestBody AuthenticationRequest request,
                        HttpServletRequest servletRequest) {
                // Rate Limiting Check - per IP
                String ip = servletRequest.getRemoteAddr();
                Bucket bucket = rateLimitingService.resolveBucket(ip);

                // Check if rate limit exceeded BEFORE attempting auth
                if (bucket.getAvailableTokens() <= 0) {
                        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                                        .body("Too many login attempts. Please try again later.");
                }

                try {
                        authenticationManager.authenticate(
                                        new UsernamePasswordAuthenticationToken(
                                                        request.getEmail(),
                                                        request.getPassword()));
                } catch (org.springframework.security.core.AuthenticationException e) {
                        // Only consume token on FAILED authentication
                        bucket.tryConsume(1);
                        throw e; // Re-throw to be handled by GlobalExceptionHandler
                }

                // Successful authentication - no token consumed
                var user = repository.findByEmail(request.getEmail())
                                .orElseThrow();
                var jwtToken = jwtService.generateToken(user);

                // Build empresa info if user has empresa
                AuthenticationResponse.EmpresaInfo empresaInfo = null;
                if (user.getEmpresa() != null) {
                        empresaInfo = AuthenticationResponse.EmpresaInfo.builder()
                                        .id(user.getEmpresa().getId())
                                        .nome(user.getEmpresa().getNome())
                                        .plano(user.getEmpresa().getPlano() != null
                                                        ? user.getEmpresa().getPlano().name()
                                                        : null)
                                        .build();
                }

                // Build features list
                java.util.List<String> features = user.getFeatures() != null
                                ? user.getFeatures().stream()
                                                .map(f -> f.getCodigo())
                                                .collect(java.util.stream.Collectors.toList())
                                : java.util.Collections.emptyList();

                return ResponseEntity.ok(AuthenticationResponse.builder()
                                .token(jwtToken)
                                .empresa(empresaInfo)
                                .features(features)
                                .build());
        }
}

@Data
@Builder
class AuthenticationResponse {
        private String token;
        private EmpresaInfo empresa;
        private java.util.List<String> features;

        @Data
        @Builder
        public static class EmpresaInfo {
                private Long id;
                private String nome;
                private String plano;
        }
}

@Data
class RegisterRequest {
        private String email;
        private String password;
}

@Data
class AuthenticationRequest {
        private String email;
        private String password;
}
