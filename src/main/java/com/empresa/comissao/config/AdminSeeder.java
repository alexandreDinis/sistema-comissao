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
                    log.info("ℹ️ Admin user not found in repo (might exist in DB but not mapped?). Creating...");
                    user = User.builder()
                            .email(email)
                            .role(Role.ADMIN_EMPRESA)
                            .empresa(empresa)
                            .build();
                }

                // Always reset credentials to ensure they are correct
                user.setRole(Role.ADMIN_EMPRESA); // Ensure proper role if existed with old role
                user.setEmpresa(empresa); // Ensure linkage
                user.setPassword(passwordEncoder.encode("admin123"));
                user.setActive(true);
                repository.save(user); // JPA Update or Insert
                log.info("✅ Admin user ensured. Email: {}, Role: {}, active: true", email, user.getRole());

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
                            .build();
                }

                superUser.setRole(Role.SUPER_ADMIN);
                superUser.setEmpresa(null); // Ensure null
                superUser.setPassword(passwordEncoder.encode("admin123")); // Ensure password
                superUser.setActive(true);
                repository.save(superUser);
                log.info("✅ Super Admin ensured (Platform Level). Email: {}, Role: {}", superEmail,
                        superUser.getRole());

            } catch (Exception e) {
                log.error("❌ Failed to seed Super Admin", e);
            }
        };
    }
}
