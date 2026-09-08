package com.shopsphere.backend.service;

import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopsphere.backend.domain.Role;
import com.shopsphere.backend.domain.User;
import com.shopsphere.backend.dto.request.LoginRequest;
import com.shopsphere.backend.dto.request.RegisterRequest;
import com.shopsphere.backend.dto.response.LoginResponse;
import com.shopsphere.backend.dto.response.UserResponse;
import com.shopsphere.backend.exception.EmailAlreadyExistsException;
import com.shopsphere.backend.mapper.UserMapper;
import com.shopsphere.backend.repository.UserRepository;
import com.shopsphere.backend.security.JwtService;
import com.shopsphere.backend.security.UserPrincipal;

/**
 * Authentication (Phases 2A-2C).
 * <p>
 * Registration stores only BCrypt password hashes. Login authenticates
 * email + password through Spring Security's {@link AuthenticationManager},
 * issues a short-lived JWT access token and a secure refresh token (only its
 * SHA-256 hash is persisted). Refresh validates/rotates the refresh token and
 * issues a fresh access JWT. Logout revokes the refresh token. Passwords,
 * raw refresh tokens and secrets are never logged.
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager, JwtService jwtService,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User user = User.builder()
                .name(request.name().trim())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .build();

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // Backstop for a concurrent registration racing the pre-check above.
            throw new EmailAlreadyExistsException(email);
        }

        return UserMapper.toResponse(user);
    }

    /**
     * Authenticates credentials and issues an access JWT plus a refresh token.
     * The refresh token is returned only here for placement in an HttpOnly
     * cookie; it is never part of the JSON response.
     * <p>
     * Writable transaction: login persists the new refresh-token record.
     */
    @Transactional
    public AuthResult login(LoginRequest request) {
        String email = normalizeEmail(request.email());

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException ex) {
            // Generic error for both wrong password and unknown email.
            throw new BadCredentialsException("Invalid email or password");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        UserPrincipal principal = UserPrincipal.from(user);
        String accessToken = jwtService.generateToken(principal);
        String refreshToken = refreshTokenService.createRefreshToken(user);
        return AuthResult.of(accessToken, refreshToken,
                UserMapper.toResponse(user), jwtService.getExpirationMs());
    }

    /**
     * Validates and rotates the presented refresh token, then issues a new
     * access JWT. Returns a fresh refresh token for the response cookie.
     */
    @Transactional
    public RefreshAuthResult refresh(String rawRefreshToken) {
        // rotate() revokes the old token (single use) and issues a new raw token.
        String newRefreshToken = refreshTokenService.rotate(rawRefreshToken);
        User user = refreshTokenService.validateAndGetUser(newRefreshToken);

        UserPrincipal principal = UserPrincipal.from(user);
        String accessToken = jwtService.generateToken(principal);
        return new RefreshAuthResult(accessToken, newRefreshToken,
                UserMapper.toResponse(user), jwtService.getExpirationMs());
    }

    /** Revokes the presented refresh token (safe/idempotent). */
    @Transactional
    public void logout(String rawRefreshToken) {
        refreshTokenService.revoke(rawRefreshToken);
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    /** Result of a successful login. The refresh token goes to the cookie only. */
    public record AuthResult(String accessToken, String refreshToken, UserResponse user, long expiresInMs) {
        static AuthResult of(String accessToken, String refreshToken, UserResponse user, long expiresInMs) {
            return new AuthResult(accessToken, refreshToken, user, expiresInMs);
        }
    }

    /** Result of a successful refresh. */
    public record RefreshAuthResult(String accessToken, String refreshToken, UserResponse user, long expiresInMs) {
    }
}