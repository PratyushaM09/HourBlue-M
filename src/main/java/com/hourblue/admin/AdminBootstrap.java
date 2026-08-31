package com.hourblue.admin;

import java.util.Locale;

import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component
@Profile("!test")
class AdminBootstrap implements ApplicationRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    AdminBootstrap(
            AdminRepository adminRepository,
            PasswordEncoder passwordEncoder,
            @Value("${ADMIN_BOOTSTRAP_EMAIL:}") String email,
            @Value("${ADMIN_BOOTSTRAP_PASSWORD:}") String password) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (isBlank(email) || isBlank(password) || adminRepository.count() > 0) {
            return;
        }

        adminRepository.save(new Admin(email.trim().toLowerCase(Locale.ROOT), passwordEncoder.encode(password)));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
