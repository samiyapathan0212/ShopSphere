package com.shopsphere.backend.exception;

/**
 * Thrown when creating/updating a category with a name that is already in use.
 * Mapped to HTTP 409 Conflict by {@link GlobalExceptionHandler}.
 */
public class DuplicateCategoryNameException extends RuntimeException {

    public DuplicateCategoryNameException(String name) {
        super("Category name is already in use: " + name);
    }
}