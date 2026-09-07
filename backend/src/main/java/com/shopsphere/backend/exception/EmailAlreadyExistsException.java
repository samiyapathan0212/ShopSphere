package com.shopsphere.backend.exception;

/**
 * Thrown when registering with an email that already exists.
 * Mapped to HTTP 409 Conflict by {@link GlobalExceptionHandler}.
 */
public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String email) {
        super("Email is already registered: " + email);
    }
}