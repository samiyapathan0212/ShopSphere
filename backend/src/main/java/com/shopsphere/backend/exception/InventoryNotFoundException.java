package com.shopsphere.backend.exception;

/**
 * Thrown when inventory is requested for a product that has no inventory row
 * and the row cannot be created. Mapped to HTTP 404 Not Found by
 * {@link GlobalExceptionHandler}.
 */
public class InventoryNotFoundException extends RuntimeException {

    public InventoryNotFoundException(Long productId) {
        super("Inventory not found for product id: " + productId);
    }
}
