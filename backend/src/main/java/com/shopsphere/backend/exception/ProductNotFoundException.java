package com.shopsphere.backend.exception;

/**
 * Thrown when a catalog product id does not exist (or is inactive and thus
 * not exposed through read APIs).
 * Mapped to HTTP 404 Not Found by {@link GlobalExceptionHandler}.
 */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Product not found: " + id);
    }
}