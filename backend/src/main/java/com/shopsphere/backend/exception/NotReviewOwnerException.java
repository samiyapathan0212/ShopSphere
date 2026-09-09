package com.shopsphere.backend.exception;

public class NotReviewOwnerException extends RuntimeException {

    public NotReviewOwnerException(Long reviewId) {
        super("Not authorized to modify review: " + reviewId);
    }
}
