package com.shopsphere.backend.exception;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(Long productivityId, int requested) {
        super("Insufficient stock for product " + productivityId + ": requested " + requested);
    }
}
