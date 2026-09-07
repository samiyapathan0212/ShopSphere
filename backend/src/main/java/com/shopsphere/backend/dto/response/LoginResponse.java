package com.shopsphere.backend.dto.response;

/**
 * Successful login response. Carries the short-lived JWT access token plus
 * basic user information. Never contains credentials or password hashes.
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresInMs,
        UserResponse user) {

    public static LoginResponse of(String accessToken, long expiresInMs, UserResponse user) {
        return new LoginResponse(accessToken, "Bearer", expiresInMs, user);
    }
}