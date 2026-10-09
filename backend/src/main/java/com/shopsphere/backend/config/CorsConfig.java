package com.shopsphere.backend.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Cross-origin configuration for the REST API (Phase 8 hardening).
 * <p>
 * The allowed origins come from {@code app.cors.allowed-origins} (environment
 * variable {@code CORS_ALLOWED_ORIGINS}) as a comma-separated list. When it is
 * empty — the default — no cross-origin rule is registered at all, which
 * preserves the single-origin setup where nginx proxies {@code /api} to this
 * service on the same host.
 * <p>
 * Credentials are allowed because the refresh token travels in a cookie, so a
 * wildcard origin is deliberately not supported: browsers reject
 * {@code Access-Control-Allow-Origin: *} together with credentials. Only the
 * methods, headers and origins the frontend actually uses are permitted, and no
 * response headers are exposed.
 */
@Configuration
public class CorsConfig {

    /** Methods used by the storefront and admin clients. */
    private static final List<String> ALLOWED_METHODS =
            List.of("GET", "POST", "PUT", "DELETE", "OPTIONS");

    /** Request headers the browser sends on API calls. */
    private static final List<String> ALLOWED_HEADERS =
            List.of("Authorization", "Content-Type", "Accept", "X-Requested-With");

    private static final long PREFLIGHT_MAX_AGE_SECONDS = 3600L;

    @Value("${app.cors.allowed-origins:}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        List<String> origins = parseOrigins(allowedOrigins);

        // Only register a rule when origins are configured. Registering a bare
        // CorsConfiguration instead would REJECT every request that carries an
        // Origin header — and browsers send Origin on every POST, even
        // same-origin ones, so an empty list must mean "no CORS rule at all".
        if (!origins.isEmpty()) {
            CorsConfiguration configuration = new CorsConfiguration();
            configuration.setAllowedOrigins(origins);
            // Required: the refresh token is sent as a cookie.
            configuration.setAllowCredentials(true);
            configuration.setAllowedMethods(ALLOWED_METHODS);
            configuration.setAllowedHeaders(ALLOWED_HEADERS);
            // The frontend reads only the response body.
            configuration.setExposedHeaders(List.of());
            configuration.setMaxAge(PREFLIGHT_MAX_AGE_SECONDS);
            source.registerCorsConfiguration("/api/**", configuration);
        }
        return source;
    }

    /** Parses the comma-separated origin list, ignoring blanks and whitespace. */
    private List<String> parseOrigins(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }
}