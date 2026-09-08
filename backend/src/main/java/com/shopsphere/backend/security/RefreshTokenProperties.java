package com.shopsphere.backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Refresh-token configuration bound from {@code app.refresh-token.*} (backed
 * by the environment variable {@code REFRESH_TOKEN_EXPIRATION_MS}). No raw
 * tokens or secrets are ever stored in configuration.
 */
@Component
@ConfigurationProperties(prefix = "app.refresh-token")
public class RefreshTokenProperties {

    /** Refresh-token lifetime in milliseconds. */
    private long expirationMs;

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }
}