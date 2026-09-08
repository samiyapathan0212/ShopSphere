package com.shopsphere.backend.dto.response;

/**
 * Successful token-refresh response. Carries a fresh short-lived JWT access
 * token plus basic user information. The refresh token itself is delivered
 * only as a Secure/HttpOnly cookie and is intentionally never included in
 * JSON responses.
 */
public record RefreshTokenResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        UserResponse user) {

    public static RefreshTokenResponse of(String accessToken, long expiresInMs, UserResponse user) {
        return new RefreshTokenResponse(accessToken, "Bearer", expiresInMs, user);
    }
}