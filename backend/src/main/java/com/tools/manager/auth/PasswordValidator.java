package com.tools.manager.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 32;

    public ValidationResult validate(String password, String username, String oldPasswordHash, BCryptPasswordEncoder encoder) {
        if (password == null || password.isEmpty()) {
            return new ValidationResult(false, "密码不能为空");
        }
        if (password.length() < MIN_LENGTH) {
            return new ValidationResult(false, "密码长度不能少于 " + MIN_LENGTH + " 个字符");
        }
        if (password.length() > MAX_LENGTH) {
            return new ValidationResult(false, "密码长度不能超过 " + MAX_LENGTH + " 个字符");
        }
        if (!containsUppercase(password)) {
            return new ValidationResult(false, "密码必须包含至少一个大写字母");
        }
        if (!containsLowercase(password)) {
            return new ValidationResult(false, "密码必须包含至少一个小写字母");
        }
        if (!containsDigit(password)) {
            return new ValidationResult(false, "密码必须包含至少一个数字");
        }
        if (username != null && !username.isEmpty() && password.equals(username)) {
            return new ValidationResult(false, "密码不能与用户名相同");
        }
        if (oldPasswordHash != null && !oldPasswordHash.isEmpty() && encoder != null) {
            if (encoder.matches(password, oldPasswordHash)) {
                return new ValidationResult(false, "新密码不能与旧密码相同");
            }
        }
        return new ValidationResult(true, "密码符合要求");
    }

    private boolean containsUppercase(String password) {
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsLowercase(String password) {
        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsDigit(String password) {
        for (char c : password.toCharArray()) {
            if (Character.isDigit(c)) {
                return true;
            }
        }
        return false;
    }

    public static class ValidationResult {
        private final boolean valid;
        private final String message;

        public ValidationResult(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() {
            return valid;
        }

        public String getMessage() {
            return message;
        }
    }
}
