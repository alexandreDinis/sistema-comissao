package com.empresa.comissao.controller;

import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.domain.enums.Role;
import com.empresa.comissao.repository.UserRepository;
import com.empresa.comissao.security.JwtService;
import com.empresa.comissao.service.RateLimitingService;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
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

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(
            @RequestBody RegisterRequest request) {
        var user = User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER) // Default role
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
        // Rate Limiting Check
        String ip = servletRequest.getRemoteAddr();
        Bucket bucket = rateLimitingService.resolveBucket(ip);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Too many login attempts. Please try again later.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));
        var user = repository.findByEmail(request.getEmail())
                .orElseThrow();
        var jwtToken = jwtService.generateToken(user);
        return ResponseEntity.ok(AuthenticationResponse.builder()
                .token(jwtToken)
                .build());
    }
}

@Data
@Builder
class AuthenticationResponse {
    private String token;
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
