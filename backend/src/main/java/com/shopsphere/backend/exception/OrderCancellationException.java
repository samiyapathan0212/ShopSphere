package com.shopsphere.backend.exception;

import com.shopsphere.backend.domain.OrderStatus;

/**
 * Thrown when cancelling an order that can no longer be cancelled (already
 * shipped, delivered or cancelled). Mapped to HTTP 409 Conflict by
 * {@link GlobalExceptionHandler}.
 */
public class OrderCancellationException extends RuntimeException {

    public OrderCancellationException(Long orderId, OrderStatus status) {
        super("Order " + orderId + " cannot be cancelled from status " + status);
    }
}
