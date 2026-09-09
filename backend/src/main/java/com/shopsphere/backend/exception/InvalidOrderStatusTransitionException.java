package com.shopsphere.backend.exception;

import com.shopsphere.backend.domain.OrderStatus;

/**
 * Thrown when an admin attempts an order status change that the lifecycle
 * does not permit (e.g. DELIVERED to PROCESSING). Mapped to HTTP 409 Conflict
 * by {@link GlobalExceptionHandler}.
 */
public class InvalidOrderStatusTransitionException extends RuntimeException {

    public InvalidOrderStatusTransitionException(OrderStatus from, OrderStatus to) {
        super("Invalid order status transition: " + from + " -> " + to);
    }
}
