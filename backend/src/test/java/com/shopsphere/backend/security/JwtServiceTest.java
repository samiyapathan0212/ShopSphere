package com.shopsphere.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.shopsphere.backend.domain.Role;
import com.shopsphere.backend.domain.User;

import io.jsonwebtoken.JwtException;

/**
 * Unit tests for {@link JwtService} — token claims, expiry and validity.
 * Uses a development-only secret (no real secrets in tests).
 */
public class JwtServiceTest {

    private static final String DEV_SECRET =
            "ZGV2LW9ubHktc2hvcHNwaGVyZS1qd3Qtc2VjcmV0LWtleS1wbGFjZWhvbGRlci0wMTIzNDU2Nzg5YWJjZGVm";

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(DEV_SECRET);
        properties.setExpirationMs(900_000L);
        jwtService = new JwtService(properties);
    }

    private UserPrincipal principal() {
        User user = User.builder()
                .id(42L)
                .name("Ada Lovelace")
                .email("ada@example.com")
                .passwordHash("$2a$10$devOnlyHashValue0000000000000000000000000000000")
                .role(Role.CUSTOMER)
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();
        return UserPrincipal.from(user);
    }

    @Test
    void tokenContainsExpectedClaims() {
        String token = jwtService.generateToken(principal());

        assertThat(token).isNotBlank();
        // Subject is the email; custom claims carry id, email and role.
        assertThat(jwtService.extractSubject(token)).isEqualTo("ada@example.com");
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtService.extractEmail(token)).isEqualTo("ada@example.com");
        assertThat(jwtService.extractRole(token)).isEqualTo(Role.CUSTOMER);
    }

    @Test
    void tokenExpirationMatchesConfiguration() {
        String token = jwtService.generateToken(principal());

        Instant expiration = jwtService.getExpiration(token);
        Instant now = Instant.now();
        // ~15 minutes after issuance (generated a moment before `now`).
        assertThat(expiration).isAfter(now.plusSeconds(13 * 60));
        assertThat(expiration).isBeforeOrEqualTo(now.plusSeconds(16 * 60));
    }

    @Test
    void tokenIsValidOnlyForItsOwner() {
        String token = jwtService.generateToken(principal());

        assertThat(jwtService.isTokenValid(token, principal())).isTrue();

        User other = User.builder()
                .id(99L)
                .name("Other User")
                .email("other@example.com")
                .passwordHash("$2a$10$devOnlyHashValue0000000000000000000000000000000")
                .role(Role.ADMIN)
                .createdAt(Instant.parse("2026-01-02T10:00:00Z"))
                .updatedAt(Instant.parse("2026-01-02T10:00:00Z"))
                .build();
        assertThat(jwtService.isTokenValid(token, UserPrincipal.from(other))).isFalse();
    }

    @Test
    void expiredTokenIsInvalid() {
        JwtProperties expiredProperties = new JwtProperties();
        expiredProperties.setSecret(DEV_SECRET);
        expiredProperties.setExpirationMs(-1_000L); // already expired
        JwtService shortLived = new JwtService(expiredProperties);

        String token = shortLived.generateToken(principal());

        // JJWT refuses to parse expired tokens (throws ExpiredJwtException, a
        // JwtException) — the filter catches this and leaves the request
        // unauthenticated, so rejection is guaranteed.
        assertThatThrownBy(() -> shortLived.isTokenValid(token, principal()))
                .isInstanceOf(JwtException.class);
    }
}