package com.shopsphere.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.shopsphere.backend.domain.RefreshToken;
import com.shopsphere.backend.domain.Role;
import com.shopsphere.backend.domain.User;
import com.shopsphere.backend.exception.InvalidRefreshTokenException;
import com.shopsphere.backend.repository.RefreshTokenRepository;
import com.shopsphere.backend.security.RefreshTokenProperties;

/**
 * Unit tests for {@link RefreshTokenService}: only the SHA-256 hash is
 * persisted, rotation revokes the old token, and expired/revoked/unknown
 * tokens are rejected. No database required.
 */
class RefreshTokenServiceTest {

    private static final String PLAIN_USER_HASH = "$2a$10$devOnlyHashValue0000000000000000000000000000000";

    private RefreshTokenRepository refreshTokenRepository;
    private RefreshTokenProperties properties;
    private RefreshTokenService service;

    @BeforeEach
    void setUp() {
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        properties = new RefreshTokenProperties();
        properties.setExpirationMs(2_592_000_000L); // 30 days
        service = new RefreshTokenService(refreshTokenRepository, properties);
    }

    private User user() {
        return User.builder()
                .id(1L)
                .name("Ada Lovelace")
                .email("ada@example.com")
                .passwordHash(PLAIN_USER_HASH)
                .role(Role.CUSTOMER)
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();
    }

    private RefreshToken stored(String hash, User user, Instant expiresAt, Instant revokedAt) {
        return RefreshToken.builder()
                .id(10L)
                .tokenHash(hash)
                .user(user)
                .expiresAt(expiresAt)
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .revokedAt(revokedAt)
                .build();
    }

    private String hashOf(String raw) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash);
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Test
    void createPersistsOnlyHashAndReturnsRawToken() {
        User user = user();
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String raw = service.createRefreshToken(user);

        assertThat(raw).isNotBlank();
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        // The persisted value is a 64-char hex SHA-256 hash, never the raw token.
        assertThat(saved.getTokenHash()).hasSize(64);
        assertThat(saved.getTokenHash()).isNotEqualTo(raw);
        assertThat(raw).doesNotContain(saved.getTokenHash());
        // The hash is a valid SHA-256 of the raw token.
        assertThat(hashOf(raw)).isEqualTo(saved.getTokenHash());
        // The record is associated with the given user. User is a JPA entity
        // without value equality, so compare the instance.
        assertThat(saved.getUser()).isSameAs(user);
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void rotateRevokesOldTokenAndReturnsNewRawToken() {
        User user = user();
        when(refreshTokenRepository.save(any(RefreshToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        String oldRaw = service.createRefreshToken(user);
        String oldHash = hashOf(oldRaw);
        RefreshToken oldStored = stored(oldHash, user, Instant.now().plusSeconds(3600), null);

        when(refreshTokenRepository.findByTokenHash(oldHash)).thenReturn(Optional.of(oldStored));

        String newRaw = service.rotate(oldRaw);

        // Old token is revoked (single-use) and a new raw token is issued.
        assertThat(oldStored.isRevoked()).isTrue();
        assertThat(oldStored.getRevokedAt()).isNotNull();
        assertThat(newRaw).isNotEqualTo(oldRaw);
        assertThat(newRaw).isNotBlank();

        // Three saves: initial creation, revocation of the old record and the
        // newly created record for the rotated token.
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, times(3)).save(captor.capture());
        List<RefreshToken> savedRecords = captor.getAllValues();
        assertThat(savedRecords.get(0).getTokenHash()).isEqualTo(hashOf(oldRaw));
        assertThat(savedRecords.get(1)).isSameAs(oldStored);
        assertThat(savedRecords.get(1).isRevoked()).isTrue();
        // The new raw token is persisted as a different hash, unrevoked.
        assertThat(savedRecords.get(2).getTokenHash()).isEqualTo(hashOf(newRaw));
        assertThat(savedRecords.get(2).isRevoked()).isFalse();
        assertThat(savedRecords.get(2).getUser()).isSameAs(user);
    }

    @Test
    void rotateRejectsRevokedToken() {
        String raw = service.createRefreshToken(user());
        String hash = hashOf(raw);
        RefreshToken revoked = stored(hash, user(), Instant.now().plusSeconds(3600), Instant.now());
        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> service.rotate(raw))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rotateRejectsExpiredToken() {
        String raw = service.createRefreshToken(user());
        String hash = hashOf(raw);
        RefreshToken expired = stored(hash, user(), Instant.parse("2020-01-01T00:00:00Z"), null);
        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.rotate(raw))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void rotateRejectsUnknownToken() {
        when(refreshTokenRepository.findByTokenHash(hashOf("unknown-raw-token")))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rotate("unknown-raw-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void revokeIsIdempotentAndSafeForUnknownToken() {
        // Unknown token: no-op, no exception.
        when(refreshTokenRepository.findByTokenHash(hashOf("unknown-raw-token")))
                .thenReturn(Optional.empty());
        service.revoke("unknown-raw-token");

        String raw = service.createRefreshToken(user());
        String hash = hashOf(raw);
        RefreshToken stored = stored(hash, user(), Instant.now().plusSeconds(3600), null);
        when(refreshTokenRepository.findByTokenHash(hash)).thenReturn(Optional.of(stored));

        // First revoke works, second (already revoked) is also a no-op.
        service.revoke(raw);
        service.revoke(raw);
        assertThat(stored.isRevoked()).isTrue();
    }
}