package com.shopsphere.backend.exception;

/**
 * Thrown when checking out a cart that has no items. Mapped to HTTP 400 Bad
 * Request by {@link GlobalExceptionHandler}.
 */
public class EmptyCartException extends RuntimeException {

    public EmptyCartException() {
        super("Cart is empty; add items before checking out");
    }
}
