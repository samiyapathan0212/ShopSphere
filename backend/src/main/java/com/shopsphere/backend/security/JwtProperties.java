package com.shopsphere.backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.io.Decoders;
import jakarta.annotation.PostConstruct;

/**
 * JWT configuration bound from {@code app.jwt.*} (backed by environment
 * variables {@code JWT_SECRET} / {@code JWT_EXPIRATION_MS}). The secret is
 * never hardcoded in code and must be provided via configuration.
 * <p>
 * There is deliberately no built-in default secret. Startup fails fast with an
 * explicit message when the secret is absent, blank, not valid base64, or too
 * short for HMAC-SHA256, so a deployment can never silently fall back to a
 * well-known development key.
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    /** Minimum decoded key length required for an HS256 signing key. */
    private static final int MIN_SECRET_BYTES = 32;

    /** Base64-encoded HMAC-SHA256 signing secret. */
    private String secret;

    /** Access token lifetime in milliseconds. */
    private long expirationMs;

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }

    /**
     * Rejects an unusable signing secret at startup rather than at the first
     * token operation, so a misconfigured deployment cannot serve traffic.
     */
    @PostConstruct
    void validateSecret() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.secret (JWT_SECRET) is required. Set the JWT_SECRET environment "
                            + "variable to a base64-encoded secret of at least 32 bytes "
                            + "(for example: openssl rand -base64 64).");
        }
        byte[] decoded;
        try {
            decoded = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "app.jwt.secret (JWT_SECRET) must be a valid base64-encoded value.", ex);
        }
        if (decoded.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret (JWT_SECRET) must decode to at least " + MIN_SECRET_BYTES
                            + " bytes for HS256 signing, but decoded " + decoded.length + " bytes.");
        }
    }
}