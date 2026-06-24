package com.tools.manager.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Scanner;

/**
 * 密码重置工具
 *
 * 使用方式：
 *   普通模式：java -jar tools-manager.jar --reset-password
 *   Docker 模式：docker exec -it <容器名> java -jar app.jar --reset-password
 *
 * 启动后会进入交互式命令行，输入用户名和新密码即可重置。
 */
@Component
public class PasswordResetRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetRunner.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public PasswordResetRunner(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        boolean resetMode = false;
        for (String arg : args) {
            if ("--reset-password".equals(arg)) {
                resetMode = true;
                break;
            }
        }

        if (!resetMode) {
            return;
        }

        System.out.println();
        System.out.println("========================================");
        System.out.println("       密码重置工具 (Password Reset)");
        System.out.println("========================================");
        System.out.println();

        Scanner scanner = new Scanner(System.in);

        // 输入用户名
        System.out.print("请输入要重置的用户名 (默认: admin): ");
        String username = scanner.nextLine().trim();
        if (username.isEmpty()) {
            username = "admin";
        }

        Optional<UserEntity> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            System.out.println("[错误] 用户 '" + username + "' 不存在！");
            System.out.println("退出密码重置工具。");
            return;
        }

        UserEntity user = userOpt.get();
        System.out.println("找到用户: " + username + " (ID: " + user.getId() + ")");

        // 输入新密码
        String newPassword;
        while (true) {
            System.out.print("请输入新密码 (至少8位，含大小写字母和数字): ");
            newPassword = scanner.nextLine().trim();
            if (newPassword.isEmpty()) {
                System.out.println("[错误] 密码不能为空");
                continue;
            }

            // 校验密码强度
            String validationError = validatePassword(newPassword, username);
            if (validationError != null) {
                System.out.println("[错误] " + validationError);
                continue;
            }
            break;
        }

        // 确认密码
        System.out.print("请再次输入新密码: ");
        String confirmPassword = scanner.nextLine().trim();
        if (!newPassword.equals(confirmPassword)) {
            System.out.println("[错误] 两次输入的密码不一致，退出重置。");
            return;
        }

        // 执行重置
        String hashed = passwordEncoder.encode(newPassword);
        user.setPasswordHash(hashed);
        user.setMustChangePassword(false);
        userRepository.save(user);

        System.out.println();
        System.out.println("[成功] 用户 '" + username + "' 的密码已重置！");
        System.out.println("请使用新密码登录系统。");
        System.out.println("========================================");
        System.out.println();

        // 退出应用
        System.exit(0);
    }

    /**
     * 密码强度校验（与 PasswordValidator 规则一致）
     */
    private String validatePassword(String password, String username) {
        if (password.length() < 8 || password.length() > 32) {
            return "密码长度必须在 8-32 个字符之间";
        }
        if (!password.matches(".*[A-Z].*")) {
            return "密码必须包含至少一个大写字母";
        }
        if (!password.matches(".*[a-z].*")) {
            return "密码必须包含至少一个小写字母";
        }
        if (!password.matches(".*\\d.*")) {
            return "密码必须包含至少一个数字";
        }
        if (password.equals(username)) {
            return "密码不能与用户名相同";
        }
        return null;
    }
}
