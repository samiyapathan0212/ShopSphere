package com.shopsphere.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import com.shopsphere.backend.dto.request.LoginRequest;
import com.shopsphere.backend.dto.request.RegisterRequest;
import com.shopsphere.backend.dto.response.LoginResponse;
import com.shopsphere.backend.dto.response.RefreshTokenResponse;
import com.shopsphere.backend.dto.response.UserResponse;
import com.shopsphere.backend.mapper.UserMapper;
import com.shopsphere.backend.security.RefreshTokenCookieService;
import com.shopsphere.backend.security.UserPrincipal;
import com.shopsphere.backend.service.AuthService;

/**
 * Authentication endpoints (Phase 2C): registration, login, refresh and
 * logout. Refresh tokens are sent only via the Secure/HttpOnly refresh_token
 * cookie and are never included in JSON bodies.
 * <p>
 * Phase 2D: {@code /me} is available to any authenticated user with the
 * CUSTOMER or ADMIN role (enforced via method security).
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Registration, login, refresh and logout")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieService cookieService;

    public AuthController(AuthService authService, RefreshTokenCookieService cookieService) {
        this.authService = authService;
        this.cookieService = cookieService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new customer account",
            description = "Creates a user with a BCrypt-hashed password. Duplicate emails return 409.")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in with email and password",
            description = "Authenticates credentials, returns a short-lived JWT access token and sets the refresh_token HttpOnly cookie.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               jakarta.servlet.http.HttpServletResponse httpResponse) {
        AuthService.AuthResult result = authService.login(request);
        cookieService.writeCookie(httpResponse, cookieService.buildCookie(result.refreshToken()));
        LoginResponse body = LoginResponse.of(result.accessToken(), result.expiresInMs(), result.user());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh the access token",
            description = "Rotates the refresh_token cookie and returns a fresh access JWT. The new refresh token is set in the cookie.")
    public ResponseEntity<RefreshTokenResponse> refresh(jakarta.servlet.http.HttpServletRequest httpRequest,
                                                        jakarta.servlet.http.HttpServletResponse httpResponse) {
        String raw = extractRefreshToken(httpRequest);
        AuthService.RefreshAuthResult result = authService.refresh(raw);
        cookieService.writeCookie(httpResponse, cookieService.buildCookie(result.refreshToken()));
        RefreshTokenResponse body =
                RefreshTokenResponse.of(result.accessToken(), result.expiresInMs(), result.user());
        return ResponseEntity.ok(body);
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out",
            description = "Revokes the refresh_token cookie (idempotent) and clears it.")
    public ResponseEntity<Void> logout(jakarta.servlet.http.HttpServletRequest httpRequest,
                                       jakarta.servlet.http.HttpServletResponse httpResponse) {
        String raw = extractRefreshToken(httpRequest);
        authService.logout(raw);
        cookieService.writeCookie(httpResponse, cookieService.buildClearingCookie());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Current authenticated user",
            description = "Returns the profile of the authenticated user. Requires a valid Bearer token (CUSTOMER or ADMIN).")
    public ResponseEntity<UserResponse> me(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return ResponseEntity.ok(UserMapper.toResponse(principal.user()));
    }

    private String extractRefreshToken(jakarta.servlet.http.HttpServletRequest request) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if (RefreshTokenCookieService.COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}