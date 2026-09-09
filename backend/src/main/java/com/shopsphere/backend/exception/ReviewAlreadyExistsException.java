package com.shopsphere.backend.exception;

public class ReviewAlreadyExistsException extends RuntimeException {

    public ReviewAlreadyExistsException(Long userId, Long productivityId) {
        super("You have already reviewed this product: " + productivityId);
    }
}
