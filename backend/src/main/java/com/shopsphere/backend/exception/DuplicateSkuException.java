package com.shopsphere.backend.exception;

/**
 * Thrown when creating/updating a product with a SKU that is already
 * registered. Mapped to HTTP 409 Conflict by {@link GlobalExceptionHandler}.
 */
public class DuplicateSkuException extends RuntimeException {

    public DuplicateSkuException(String sku) {
        super("SKU is already registered: " + sku);
    }
}