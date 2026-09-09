package com.shopsphere.backend.exception;

public class WishlistNotFoundException extends RuntimeException {

    public WishlistNotFoundException(Long userId) {
        super("Wishlist not found for user: " + userId);
    }
}
