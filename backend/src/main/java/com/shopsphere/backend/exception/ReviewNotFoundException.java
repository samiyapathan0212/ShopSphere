package com.shopsphere.backend.exception;

public class ReviewNotFoundException extends RuntimeException {

    public ReviewNotFoundException(Long reviewId) {
        super("Review not found: " + reviewId);
    }
}
