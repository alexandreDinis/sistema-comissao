package com.empresa.comissao.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    // Email do remetente (deve ser verificado no Brevo)
    @Value("${app.mail.from:${spring.mail.username:noreply@sistema.com}}")
    private String fromAddress;

    @Value("${app.name:Sistema de Gestão}")
    private String appName;

    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        // Check if SMTP is configured
        if (fromAddress == null || fromAddress.isBlank() || fromAddress.equals("noreply@sistema.com")) {
            // DEV MODE: Just log the link instead of sending email
            log.warn("📧 [DEV MODE] SMTP não configurado. Link de reset para {}: {}", toEmail, resetLink);
            log.warn("📧 [DEV MODE] Configure MAIL_USERNAME e MAIL_PASSWORD para enviar emails reais.");
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(appName + " - Recuperação de Senha");
            message.setText(buildPasswordResetEmailBody(resetLink));

            mailSender.send(message);
            log.info("✅ Email de recuperação enviado para: {}", toEmail);
        } catch (Exception e) {
            log.error("❌ Erro ao enviar email para {}: {}", toEmail, e.getMessage());
            // In dev, don't throw - just log the link
            log.warn("📧 [FALLBACK] Link de reset: {}", resetLink);
        }
    }

    private String buildPasswordResetEmailBody(String resetLink) {
        return String.format("""
                Olá,

                Você solicitou a recuperação de senha para sua conta no %s.

                Clique no link abaixo para criar uma nova senha:
                %s

                Este link é válido por 30 minutos.

                Se você não solicitou esta recuperação, ignore este email.

                Atenciosamente,
                Equipe %s
                """, appName, resetLink, appName);
    }
}
