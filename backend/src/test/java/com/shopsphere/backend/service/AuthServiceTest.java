package com.shopsphere.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.shopsphere.backend.domain.Role;
import com.shopsphere.backend.domain.User;
import com.shopsphere.backend.dto.request.LoginRequest;
import com.shopsphere.backend.dto.request.RegisterRequest;
import com.shopsphere.backend.dto.response.LoginResponse;
import com.shopsphere.backend.dto.response.UserResponse;
import com.shopsphere.backend.exception.EmailAlreadyExistsException;
import com.shopsphere.backend.repository.UserRepository;
import com.shopsphere.backend.security.JwtService;

/**
 * Unit tests for {@link AuthService} using a real BCrypt encoder and mocked
 * authentication manager / JWT and refresh-token services (no database).
 */
class AuthServiceTest {

    private static final String PLAIN_PASSWORD = "superSecret123!";

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthenticationManager authenticationManager;
    private JwtService jwtService;
    private RefreshTokenService refreshTokenService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        authenticationManager = mock(AuthenticationManager.class);
        jwtService = mock(JwtService.class);
        refreshTokenService = mock(RefreshTokenService.class);
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService,
                refreshTokenService);
    }

    @Test
    void registerStoresBcryptHashNotPlaintextPassword() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User incoming = invocation.getArgument(0);
            Instant now = Instant.now();
            return User.builder()
                    .id(1L)
                    .name(incoming.getName())
                    .email(incoming.getEmail())
                    .passwordHash(incoming.getPasswordHash())
                    .role(incoming.getRole())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        });

        authService.register(new RegisterRequest(" Ada Lovelace ", "Ada@Example.com ", PLAIN_PASSWORD));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getName()).isEqualTo("Ada Lovelace");
        // Email is normalized (trimmed + lowercased) before storage/lookup.
        assertThat(saved.getEmail()).isEqualTo("ada@example.com");
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
        // Never store the plain-text password.
        assertThat(saved.getPasswordHash()).isNotEqualTo(PLAIN_PASSWORD);
        // The stored value must be a valid BCrypt hash of the original password.
        assertThat(saved.getPasswordHash()).startsWith("$2a$");
        assertThat(passwordEncoder.matches(PLAIN_PASSWORD, saved.getPasswordHash())).isTrue();
    }

    @Test
    void registerDoesNotExposePasswordHashInResponse() {
        AtomicLong idSeq = new AtomicLong();
        when(userRepository.existsByEmail(any(String.class))).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User incoming = invocation.getArgument(0);
            Instant now = Instant.now();
            return User.builder()
                    .id(idSeq.incrementAndGet())
                    .name(incoming.getName())
                    .email(incoming.getEmail())
                    .passwordHash(incoming.getPasswordHash())
                    .role(incoming.getRole())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
        });

        UserResponse response =
                authService.register(new RegisterRequest("Ada Lovelace", "ada@example.com", PLAIN_PASSWORD));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Ada Lovelace");
        assertThat(response.email()).isEqualTo("ada@example.com");
        assertThat(response.role()).isEqualTo(Role.CUSTOMER);
        assertThat(response.createdAt()).isNotNull();
        assertThat(response.updatedAt()).isNotNull();
        // The response type has no password fields at all (guaranteed by its shape).
        assertThat(UserResponse.class.getRecordComponents())
                .noneMatch(c -> c.getName().toLowerCase().contains("password"));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Ada Lovelace", "Ada@Example.com", PLAIN_PASSWORD)))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("ada@example.com");

        // No attempt is made to persist a duplicate user.
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void loginSuccessReturnsTokenAndUser() {
        User user = User.builder()
                .id(1L)
                .name("Ada Lovelace")
                .email("ada@example.com")
                .passwordHash(passwordEncoder.encode(PLAIN_PASSWORD))
                .role(Role.CUSTOMER)
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(any())).thenReturn("sample.jwt.token");
        when(jwtService.getExpirationMs()).thenReturn(900_000L);
        when(refreshTokenService.createRefreshToken(user)).thenReturn("raw.refresh.token");

        AuthService.AuthResult result = authService.login(new LoginRequest("ada@example.com", PLAIN_PASSWORD));

        assertThat(result.accessToken()).isEqualTo("sample.jwt.token");
        assertThat(result.refreshToken()).isEqualTo("raw.refresh.token");
        assertThat(result.expiresInMs()).isEqualTo(900_000L);
        assertThat(result.user().email()).isEqualTo("ada@example.com");
        assertThat(result.user().id()).isEqualTo(1L);
        // The refresh token is carried for the HttpOnly cookie; the JSON-facing
        // LoginResponse never includes it.
        verify(refreshTokenService).createRefreshToken(user);
        assertThat(LoginResponse.class.getRecordComponents())
                .noneMatch(c -> c.getName().toLowerCase().contains("password")
                        || c.getName().toLowerCase().contains("refresh"));
        // Raw refresh tokens are never logged (no logging call here); assert the
        // response record has no refresh field.
    }

    @Test
    void refreshRotatesTokenAndIssuesNewAccessJwt() {
        User user = User.builder()
                .id(2L)
                .name("Ada Lovelace")
                .email("ada@example.com")
                .passwordHash(passwordEncoder.encode(PLAIN_PASSWORD))
                .role(Role.CUSTOMER)
                .createdAt(Instant.parse("2026-01-01T10:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T10:00:00Z"))
                .build();

        when(refreshTokenService.rotate("old.raw.refresh")).thenReturn("new.raw.refresh");
        when(refreshTokenService.validateAndGetUser("new.raw.refresh")).thenReturn(user);
        when(jwtService.generateToken(any())).thenReturn("fresh.access.jwt");
        when(jwtService.getExpirationMs()).thenReturn(900_000L);

        AuthService.RefreshAuthResult result = authService.refresh("old.raw.refresh");

        assertThat(result.accessToken()).isEqualTo("fresh.access.jwt");
        assertThat(result.refreshToken()).isEqualTo("new.raw.refresh");
        assertThat(result.user().email()).isEqualTo("ada@example.com");
        // Old token was rotated (single use) and a new one issued.
        verify(refreshTokenService).rotate("old.raw.refresh");
        verify(refreshTokenService).validateAndGetUser("new.raw.refresh");
    }

    @Test
    void logoutRevokesRefreshToken() {
        authService.logout("raw.refresh.token");
        verify(refreshTokenService).revoke("raw.refresh.token");
    }

    @Test
    void loginWrongPasswordReturnsGenericError() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("ada@example.com", "wrong-password")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");

        // The repository is never consulted for a failed authentication, so the
        // response cannot reveal whether the email exists.
        verify(userRepository, never()).findByEmail(anyString());
        verify(refreshTokenService, never()).createRefreshToken(any());
    }

    @Test
    void loginUnknownEmailReturnsGenericError() {
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown@example.com", "whatever123")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid email or password");
    }
}