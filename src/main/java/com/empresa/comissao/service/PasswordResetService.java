package com.empresa.comissao.service;

import com.empresa.comissao.domain.entity.PasswordResetToken;
import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.repository.PasswordResetTokenRepository;
import com.empresa.comissao.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    private static final int TOKEN_VALIDITY_MINUTES = 30;

    @Transactional
    public void initiatePasswordReset(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            // Don't reveal if user exists - log and return silently
            log.warn("⚠️ Tentativa de reset para email não cadastrado: {}", email);
            return;
        }

        User user = userOpt.get();

        // Delete any existing tokens for this user
        tokenRepository.deleteByUser(user);

        // Generate new token
        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiresAt(LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES))
                .used(false)
                .build();

        tokenRepository.save(resetToken);

        // Build reset link
        String resetLink = baseUrl + "/reset-password?token=" + token;

        // Send email
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        log.info("✅ Token de reset criado para: {}", email);
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            log.warn("⚠️ Token de reset não encontrado: {}", token);
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (!resetToken.isValid()) {
            log.warn("⚠️ Token inválido ou expirado: {}", token);
            return false;
        }

        // Update password
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("✅ Senha resetada com sucesso para: {}", user.getEmail());
        return true;
    }

    public boolean isTokenValid(String token) {
        return tokenRepository.findByToken(token)
                .map(PasswordResetToken::isValid)
                .orElse(false);
    }
}
