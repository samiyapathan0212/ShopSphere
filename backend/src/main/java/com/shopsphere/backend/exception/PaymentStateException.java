package com.shopsphere.backend.exception;

/**
 * Thrown when a payment operation conflicts with the payment's current state
 * (e.g. processing a payment that is already PAID, or an unsupported manual
 * status transition). Mapped to HTTP 409 Conflict by
 * {@link GlobalExceptionHandler}.
 */
public class PaymentStateException extends RuntimeException {

    public PaymentStateException(String message) {
        super(message);
    }
}
