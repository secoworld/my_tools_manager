package com.tools.manager.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserRepository userRepository;
    private final SessionManager sessionManager;
    private final PasswordValidator passwordValidator;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository,
                          SessionManager sessionManager,
                          PasswordValidator passwordValidator,
                          BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.sessionManager = sessionManager;
        this.passwordValidator = passwordValidator;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            return errorResponse("用户名和密码不能为空", HttpStatus.BAD_REQUEST);
        }

        if (sessionManager.isLocked(username)) {
            log.warn("Login attempt for locked account: {}", username);
            return errorResponse("账户已被锁定，请稍后再试", HttpStatus.UNAUTHORIZED);
        }

        Optional<UserEntity> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            sessionManager.recordFailedAttempt(username);
            return errorResponse("用户名或密码错误", HttpStatus.UNAUTHORIZED);
        }

        UserEntity user = userOpt.get();
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            sessionManager.recordFailedAttempt(username);
            return errorResponse("用户名或密码错误", HttpStatus.UNAUTHORIZED);
        }

        sessionManager.resetAttempts(username);
        String token = sessionManager.createToken(username);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", token);
        response.put("username", user.getUsername());
        response.put("mustChangePassword", user.getMustChangePassword());
        log.info("User logged in: {}", username);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(@RequestBody Map<String, String> body,
                                                              HttpServletRequest request) {
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");

        String token = extractToken(request);
        if (token == null || !sessionManager.validate(token)) {
            return errorResponse("未登录或登录已过期", HttpStatus.UNAUTHORIZED);
        }

        String username = sessionManager.getUsername(token);
        Optional<UserEntity> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return errorResponse("用户不存在", HttpStatus.UNAUTHORIZED);
        }

        UserEntity user = userOpt.get();

        if (oldPassword == null || !passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            return errorResponse("原密码错误", HttpStatus.UNAUTHORIZED);
        }

        PasswordValidator.ValidationResult validation = passwordValidator.validate(
                newPassword, username, user.getPasswordHash(), passwordEncoder);
        if (!validation.isValid()) {
            return errorResponse(validation.getMessage(), HttpStatus.BAD_REQUEST);
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);

        // Invalidate all tokens except current
        sessionManager.invalidateAll(username);
        String newToken = sessionManager.createToken(username);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "密码修改成功");
        response.put("token", newToken);
        log.info("Password changed for user: {}", username);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null) {
            sessionManager.invalidate(token);
        }
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "已退出登录");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> profile(HttpServletRequest request) {
        String token = extractToken(request);
        if (token == null || !sessionManager.validate(token)) {
            return errorResponse("未登录或登录已过期", HttpStatus.UNAUTHORIZED);
        }
        String username = sessionManager.getUsername(token);
        Optional<UserEntity> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return errorResponse("用户不存在", HttpStatus.UNAUTHORIZED);
        }
        UserEntity user = userOpt.get();
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("username", user.getUsername());
        response.put("mustChangePassword", user.getMustChangePassword());
        return ResponseEntity.ok(response);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private ResponseEntity<Map<String, Object>> errorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }
}
