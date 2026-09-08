package com.shopsphere.backend.exception;

/**
 * Thrown when a catalog category id does not exist (or is inactive and thus
 * not exposed through read APIs).
 * Mapped to HTTP 404 Not Found by {@link GlobalExceptionHandler}.
 */
public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(Long id) {
        super("Category not found: " + id);
    }
}