package com.shopsphere.backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopsphere.backend.domain.RefreshToken;
import com.shopsphere.backend.domain.User;
import com.shopsphere.backend.exception.InvalidRefreshTokenException;
import com.shopsphere.backend.repository.RefreshTokenRepository;
import com.shopsphere.backend.security.RefreshTokenProperties;

/**
 * Issues, validates and rotates refresh tokens (Phase 2C).
 * <p>
 * Raw refresh tokens are generated with {@link SecureRandom} (256 bits,
 * base64url-encoded). Only a SHA-256 hash of the token is persisted, so a
 * database leak never exposes usable tokens. Each refresh both revokes the
 * presented token and issues a fresh one (rotation); expired or revoked
 * tokens are rejected with a generic error. Raw tokens are never logged.
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenProperties refreshTokenProperties;
    private final SecureRandom secureRandom;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               RefreshTokenProperties refreshTokenProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenProperties = refreshTokenProperties;
        this.secureRandom = new SecureRandom();
    }

    /**
     * Creates a new refresh token and persists only its SHA-256 hash.
     *
     * @return the raw token (delivered to the caller via the HttpOnly cookie;
     *         never stored or logged)
     */
    @Transactional
    public String createRefreshToken(User user) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        RefreshToken entity = RefreshToken.builder()
                .tokenHash(hash(rawToken))
                .user(user)
                .expiresAt(Instant.now().plusMillis(refreshTokenProperties.getExpirationMs()))
                .build();

        refreshTokenRepository.save(entity);
        return rawToken;
    }

    /**
     * Validates a raw token against its stored hash, expiry and revocation
     * status. On success the presented token is revoked (single-use) and a new
     * token is returned, so {@code rawToken} must not be reused.
     *
     * @return the new raw refresh token
     */
    @Transactional
    public String rotate(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            // A missing refresh_token cookie must yield a clean 401, not a 500.
            throw new InvalidRefreshTokenException();
        }
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (stored.isRevoked() || stored.isExpired()) {
            throw new InvalidRefreshTokenException();
        }

        stored.revoke();
        refreshTokenRepository.save(stored);

        return createRefreshToken(stored.getUser());
    }

    /** Revokes a refresh token, making it unusable. Never throws for unknown tokens. */
    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByTokenHash(hash(rawToken))
                .ifPresent(entity -> {
                    entity.revoke();
                    refreshTokenRepository.save(entity);
                });
    }

    /** Returns the user owning a currently-valid raw token, or throws. */
    @Transactional(readOnly = true)
    public User validateAndGetUser(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            throw new InvalidRefreshTokenException();
        }
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        if (stored.isRevoked() || stored.isExpired()) {
            throw new InvalidRefreshTokenException();
        }
        return stored.getUser();
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }
}