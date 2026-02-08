package org.fintech.wallet.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:token:";
    private static final String REFRESH_TOKEN_PREFIX = "refresh:token:";
    private static final String USER_SESSIONS_PREFIX = "user:sessions:";
    /**
     * update at
     */
    private static final String USER_LOGOUT_AT_PREFIX = "user:lastLogoutAt:";


    /**
     * Blacklist an access token
     * @param token The JWT token to blacklist
     * @param expirationSeconds How long to keep it blacklisted (match token TTL)
     */
    public void blacklistToken(String token, long expirationSeconds) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "blacklisted", expirationSeconds, TimeUnit.SECONDS);
        log.info("Token blacklisted for {} seconds", expirationSeconds);
    }

    /**
     * Check if a token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        String key = BLACKLIST_PREFIX + token;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * Store refresh token in Redis
     * @param userId User ID
     * @param refreshToken The refresh token
     * @param expirationDays Expiration in days
     */
    public void storeRefreshToken(UUID userId,
                                  String sessionId,
                                  String refreshToken,
                                  long expirationDays) {

        String key = REFRESH_TOKEN_PREFIX + userId + ":" + sessionId;
        redisTemplate.opsForValue().set(
                key,
                refreshToken,
                Duration.ofDays(expirationDays)
        );
    }

    /**
     * Invalidate refresh token (for logout)
     */
    public void invalidateRefreshToken(UUID userId, String sessionId) {
        String key = REFRESH_TOKEN_PREFIX + userId + ":" + sessionId;
        redisTemplate.delete(key);
    }

    /**
     * Invalidate all refresh tokens for a user
     * @param userId
     */
    public void invalidateAllRefreshTokens(UUID userId) {
        Set<String> keys =
                redisTemplate.keys(REFRESH_TOKEN_PREFIX + userId + ":*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /**
     * Store user session (for tracking active sessions)
     */
    public void storeUserSession(UUID userId, String sessionId, String deviceInfo) {
        String key = USER_SESSIONS_PREFIX + userId;
        String sessionData = sessionId + ":" + deviceInfo;
        redisTemplate.opsForSet().add(key, sessionData);
        log.info("Session stored for user: {}", userId);
    }

    /**
     * Remove user session (on logout)
     */
    public void removeUserSession(UUID userId, String sessionId) {
        String key = USER_SESSIONS_PREFIX + userId;
        // Remove all sessions matching this sessionId
        redisTemplate.opsForSet().members(key).stream()
                .filter(session -> session.startsWith(sessionId))
                .forEach(session -> redisTemplate.opsForSet().remove(key, session));
        log.info("Session removed for user: {}", userId);
    }

    /**
     * Logout from all devices (invalidate all sessions)
     */
    public void logoutAllDevices(UUID userId) {
        String key = USER_SESSIONS_PREFIX + userId;

        // remove all sessions
        redisTemplate.delete(key);

        // invalidate ALL refresh tokens
        invalidateAllRefreshTokens(userId);

        log.info("All sessions cleared for user: {}", userId);
    }
    public void updateLastLogoutAt(UUID userId) {
        String key = USER_LOGOUT_AT_PREFIX + userId;
        redisTemplate.opsForValue().set(
                key,
                String.valueOf(System.currentTimeMillis())
        );
    }
    public Long getLastLogoutAt(UUID userId) {
        String value = redisTemplate.opsForValue()
                .get(USER_LOGOUT_AT_PREFIX + userId);

        return value != null ? Long.parseLong(value) : null;
    }

}