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

    private final com.empresa.comissao.repository.EmpresaRepository empresaRepository;

    @Bean
    public CommandLineRunner seedAdmin(UserRepository repository, PasswordEncoder passwordEncoder) {
        return args -> {
            String email = "admin@empresa.com";
            try {
                // Find or Create Default Company if using this seeder (but V2 migration should
                // have created it)
                var empresa = empresaRepository.findByNome("Empresa Padrão").orElse(null);
                if (empresa == null) {
                    log.error("❌ 'Empresa Padrão' not found. Seeding aborted. Run V2 migration.");
                    return;
                }

                User user = repository.findByEmail(email).orElse(null);

                if (user == null) {
                    log.info("ℹ️ Admin user not found in repo. Creating...");
                    user = User.builder()
                            .email(email)
                            .role(Role.ADMIN_EMPRESA)
                            .empresa(empresa)
                            .password(passwordEncoder.encode("admin123")) // Set password only on creation
                            .active(true)
                            .build();
                    repository.save(user);
                    log.info("✅ Admin user created. Email: {}", email);
                } else {
                    log.info("ℹ️ Admin user already exists. Skipping password reset.");
                }

            } catch (Exception e) {
                log.error("❌ Failed to seed admin user", e);
            }

            // --- Seed Super Admin ---
            String superEmail = "saas@plataforma.com";
            try {
                // Super Admin cleanses itself of company ties if they exist (legacy fix)
                User superUser = repository.findByEmail(superEmail).orElse(null);

                if (superUser == null) {
                    log.info("ℹ️ Super Admin not found. Creating...");
                    superUser = User.builder()
                            .email(superEmail)
                            .role(Role.SUPER_ADMIN)
                            .empresa(null) // Explicitly null
                            .password(passwordEncoder.encode("47548971")) // Defaul Super Admin Pass
                            .active(true)
                            .build();
                    repository.save(superUser);
                    log.info("✅ Super Admin created. Email: {}", superEmail);
                } else {
                    log.info("ℹ️ Super Admin already exists. Skipping password reset.");
                }

            } catch (Exception e) {
                log.error("❌ Failed to seed Super Admin", e);
            }

        };
    }
}
