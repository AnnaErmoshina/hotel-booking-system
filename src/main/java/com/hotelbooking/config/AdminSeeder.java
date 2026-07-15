package com.hotelbooking.config;

import com.hotelbooking.entity.User;
import com.hotelbooking.entity.enums.Role;
import com.hotelbooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds exactly one ADMIN user on startup if none exists yet with the
 * configured email, so the very first ADMIN account never has to be created
 * by hand-editing the `users` table via a SQL client (see PROJECT_STATUS.md,
 * TODO 6.3).
 * <p>
 * Credentials come from app.admin.* properties (application.yml), overridable
 * via ADMIN_EMAIL / ADMIN_PASSWORD / ADMIN_FIRST_NAME / ADMIN_LAST_NAME env
 * vars. The password is hashed at startup with the same PasswordEncoder bean
 * used everywhere else, so no pre-computed hash needs to be committed to the
 * repo or baked into a Liquibase migration.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.first-name}")
    private String adminFirstName;

    @Value("${app.admin.last-name}")
    private String adminLastName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByEmail(adminEmail)) {
            return;
        }

        User admin = User.builder()
                .firstName(adminFirstName)
                .lastName(adminLastName)
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .role(Role.ADMIN)
                .build();

        userRepository.save(admin);

        log.warn("Seeded initial ADMIN user '{}'. Log in and change the password immediately " +
                "(or set ADMIN_PASSWORD before the very first startup) — see app.admin.* in application.yml.",
                adminEmail);
    }
}
