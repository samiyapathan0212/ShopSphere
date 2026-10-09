package com.shopsphere.backend.security;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Builds the {@code refresh_token} cookie.
 * <p>
 * Attribute choices:
 * <ul>
 *   <li>{@code HttpOnly} — prevents JavaScript access.</li>
 *   <li>{@code Secure} — only sent over HTTPS. Configurable through
 *       {@code app.refresh-token.secure} ({@code REFRESH_TOKEN_COOKIE_SECURE},
 *       default true). Browsers treat {@code http://localhost} as a secure
 *       context, so local Docker development is unaffected; set it to false
 *       only for plain-HTTP development on a non-localhost host.</li>
 *   <li>{@code SameSite=Lax} — sent on same-site navigations, blocks cross-site
 *       sending in modern browsers, which is appropriate for the current
 *       development setup (separate frontend/backend on the same site).</li>
 *   <li>{@code Path=/api/auth} — scoped so the cookie is only sent to the
 *       auth endpoints that use it.</li>
 * </ul>
 * The raw token value is delivered only inside this cookie.
 */
@Service
public class RefreshTokenCookieService {

    public static final String COOKIE_NAME = "refresh_token";

    private final RefreshTokenProperties properties;

    public RefreshTokenCookieService(RefreshTokenProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie buildCookie(String rawToken) {
        return ResponseCookie.from(COOKIE_NAME, rawToken)
                .path("/api/auth")
                .httpOnly(true)
                .secure(properties.isSecure())
                .sameSite("Lax")
                .maxAge(Duration.ofMillis(properties.getExpirationMs()))
                .build();
    }

    public ResponseCookie buildClearingCookie() {
        return ResponseCookie.from(COOKIE_NAME, "")
                .path("/api/auth")
                .httpOnly(true)
                .secure(properties.isSecure())
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
    }

    /** Writes a cookie into the servlet response. */
    public void writeCookie(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader("Set-Cookie", cookie.toString());
    }
}