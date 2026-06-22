package com.tools.manager.config;

import com.tools.manager.auth.UserEntity;
import com.tools.manager.auth.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        Optional<UserEntity> existing = userRepository.findByUsername("admin");
        if (existing.isPresent()) {
            log.info("Admin user already exists, skipping initialization");
            return;
        }

        UserEntity admin = new UserEntity();
        admin.setUsername("admin");
        admin.setPasswordHash(passwordEncoder.encode("admin"));
        admin.setMustChangePassword(true);
        userRepository.save(admin);
        log.info("Default admin user created with username 'admin' and default password 'admin'");
        log.info("IMPORTANT: Please change the default password on first login");
    }
}
