package com.shopsphere.backend.exception;

/**
 * Thrown when attempting to delete a category that still has products
 * referencing it. Mapped to HTTP 409 Conflict by
 * {@link GlobalExceptionHandler} (the {@code fk_products_category} database
 * constraint remains the backstop).
 */
public class CategoryInUseException extends RuntimeException {

    public CategoryInUseException() {
        super("Category has products and cannot be deleted");
    }
}