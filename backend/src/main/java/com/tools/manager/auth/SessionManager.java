package com.tools.manager.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SessionManager {

    private static final Logger log = LoggerFactory.getLogger(SessionManager.class);

    private static final int TOKEN_EXPIRY_HOURS = 12;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_DURATION_MINUTES = 15;

    private final ConcurrentHashMap<String, SessionInfo> tokens = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AttemptInfo> attempts = new ConcurrentHashMap<>();

    private static class SessionInfo {
        final String username;
        volatile LocalDateTime expiresAt;

        SessionInfo(String username, LocalDateTime expiresAt) {
            this.username = username;
            this.expiresAt = expiresAt;
        }
    }

    private static class AttemptInfo {
        int count;
        LocalDateTime lockedUntil;

        AttemptInfo(int count, LocalDateTime lockedUntil) {
            this.count = count;
            this.lockedUntil = lockedUntil;
        }
    }

    public String createToken(String username) {
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);
        tokens.put(token, new SessionInfo(username, expiresAt));
        log.info("Created token for user: {}", username);
        return token;
    }

    public boolean validate(String token) {
        if (token == null) {
            return false;
        }
        SessionInfo info = tokens.get(token);
        if (info == null) {
            return false;
        }
        if (info.expiresAt.isBefore(LocalDateTime.now())) {
            tokens.remove(token);
            log.info("Removed expired token for user: {}", info.username);
            return false;
        }
        // Extend expiry on valid access
        info.expiresAt = LocalDateTime.now().plusHours(TOKEN_EXPIRY_HOURS);
        return true;
    }

    public void invalidate(String token) {
        if (token == null) {
            return;
        }
        SessionInfo info = tokens.remove(token);
        if (info != null) {
            log.info("Invalidated token for user: {}", info.username);
        }
    }

    public void invalidateAll(String username) {
        Iterator<Map.Entry<String, SessionInfo>> it = tokens.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, SessionInfo> entry = it.next();
            if (entry.getValue().username.equals(username)) {
                it.remove();
            }
        }
        log.info("Invalidated all tokens for user: {}", username);
    }

    public String getUsername(String token) {
        if (token == null) {
            return null;
        }
        SessionInfo info = tokens.get(token);
        if (info == null) {
            return null;
        }
        if (info.expiresAt.isBefore(LocalDateTime.now())) {
            tokens.remove(token);
            return null;
        }
        return info.username;
    }

    public boolean isLocked(String username) {
        AttemptInfo info = attempts.get(username);
        if (info == null || info.lockedUntil == null) {
            return false;
        }
        if (info.lockedUntil.isBefore(LocalDateTime.now())) {
            attempts.remove(username);
            return false;
        }
        return true;
    }

    public void recordFailedAttempt(String username) {
        AttemptInfo info = attempts.computeIfAbsent(username, k -> new AttemptInfo(0, null));
        info.count++;
        if (info.count >= MAX_FAILED_ATTEMPTS) {
            info.lockedUntil = LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES);
            log.warn("Account locked for user: {} after {} failed attempts", username, info.count);
        } else {
            log.warn("Failed attempt {} for user: {}", info.count, username);
        }
    }

    public void resetAttempts(String username) {
        attempts.remove(username);
        log.info("Reset failed attempts for user: {}", username);
    }
}
