package com.shopsphere.backend.exception;

/**
 * Thrown when a catalog read endpoint receives an invalid query parameter
 * (negative/zero page size, minPrice greater than maxPrice, an unsupported
 * sort field or direction,. Mapped to HTTP 400 Bad Request by
 * {@link GlobalExceptionHandler} alongside the existing validation envelope.
 */
public class InvalidQueryParameterException extends RuntimeException {

    public InvalidQueryParameterException(String message) {
        super(message);
    }
}