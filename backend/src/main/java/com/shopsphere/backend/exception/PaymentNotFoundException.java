package com.shopsphere.backend.exception;

/**
 * Thrown when no payment record exists for an order. Mapped to HTTP 404 Not
 * Found by {@link GlobalExceptionHandler}.
 */
public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(Long orderId) {
        super("Payment not found for order id: " + orderId);
    }
}
