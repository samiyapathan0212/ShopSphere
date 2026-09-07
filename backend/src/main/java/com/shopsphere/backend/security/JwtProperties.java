package com.shopsphere.backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT configuration bound from {@code app.jwt.*} (backed by environment
 * variables {@code JWT_SECRET} / {@code JWT_EXPIRATION_MS}). The secret is
 * never hardcoded in code and must be provided via configuration.
 */
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

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
}