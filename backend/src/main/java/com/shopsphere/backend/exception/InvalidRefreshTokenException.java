package com.shopsphere.backend.exception;

/**
 * Thrown when a refresh token is missing, invalid, expired or revoked.
 * Mapped to a generic HTTP 401 by {@link GlobalExceptionHandler} so the
 * response never reveals whether a token or user exists.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token");
    }
}