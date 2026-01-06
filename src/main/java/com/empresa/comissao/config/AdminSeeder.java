package com.empresa.comissao.config;

import com.empresa.comissao.domain.entity.User;
import com.empresa.comissao.domain.enums.Role;
import com.empresa.comissao.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder {

    @Bean
    public CommandLineRunner seedAdmin(UserRepository repository, PasswordEncoder passwordEncoder) {
        return args -> {
            String email = "admin@empresa.com";
            try {
                User user = repository.findByEmail(email).orElse(null);

                if (user == null) {
                    log.info("ℹ️ Admin user not found. Creating...");
                    user = User.builder()
                            .email(email)
                            .role(Role.ADMIN)
                            .build();
                }

                // Always reset credentials to ensure they are correct
                user.setPassword(passwordEncoder.encode("admin123"));
                user.setActive(true);
                repository.save(user); // JPA Update or Insert
                log.info("✅ Admin user ensured. Email: {}, Password: admin123, Active: true", email);

            } catch (Exception e) {
                log.error("❌ Failed to seed admin user", e);
            }
        };
    }
}
