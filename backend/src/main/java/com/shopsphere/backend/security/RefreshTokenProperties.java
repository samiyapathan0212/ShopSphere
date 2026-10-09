package com.shopsphere.backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Refresh-token configuration bound from {@code app.refresh-token.*} (backed
 * by the environment variables {@code REFRESH_TOKEN_EXPIRATION_MS} and
 * {@code REFRESH_TOKEN_COOKIE_SECURE}). No raw tokens or secrets are ever
 * stored in configuration.
 */
@Component
@ConfigurationProperties(prefix = "app.refresh-token")
public class RefreshTokenProperties {

    /** Refresh-token lifetime in milliseconds. */
    private long expirationMs;

    /**
     * Whether the refresh cookie is marked {@code Secure}. Defaults to true,
     * which is correct for any HTTPS deployment; browsers also accept Secure
     * cookies on {@code http://localhost}, so local Docker development is
     * unaffected. Set to false only for plain-HTTP development on a
     * non-localhost host.
     */
    private boolean secure = true;

    public long getExpirationMs() {
        return expirationMs;
    }

    public void setExpirationMs(long expirationMs) {
        this.expirationMs = expirationMs;
    }

    public boolean isSecure() {
        return secure;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }
}