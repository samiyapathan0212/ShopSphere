package com.shopsphere.backend.exception;

/**
 * Thrown when an order id does not exist — or when it exists but belongs to a
 * different customer (the response never reveals another customer's orders).
 * Mapped to HTTP 404 Not Found by {@link GlobalExceptionHandler}.
 */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(Long orderId) {
        super("Order not found for id: " + orderId);
    }
}
