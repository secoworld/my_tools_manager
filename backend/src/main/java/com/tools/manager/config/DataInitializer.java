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
        // 支持通过系统属性或环境变量重置 admin 密码
        // 用法: -Dapp.reset-admin-password=Admin@123 或 APP_RESET_ADMIN_PASSWORD=Admin@123
        String resetPassword = System.getProperty("app.reset-admin-password");
        if (resetPassword == null || resetPassword.isEmpty()) {
            resetPassword = System.getenv("APP_RESET_ADMIN_PASSWORD");
        }

        if (resetPassword != null && !resetPassword.isEmpty()) {
            Optional<UserEntity> existing = userRepository.findByUsername("admin");
            if (existing.isPresent()) {
                UserEntity user = existing.get();
                user.setPasswordHash(passwordEncoder.encode(resetPassword));
                user.setMustChangePassword(false);
                userRepository.save(user);
                log.info("Admin password has been reset via system property");
                log.info("Admin username: admin");
                return;
            }
        }

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
