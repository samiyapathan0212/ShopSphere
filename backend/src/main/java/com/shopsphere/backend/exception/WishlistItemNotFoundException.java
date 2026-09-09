package com.shopsphere.backend.exception;

public class WishlistItemNotFoundException extends RuntimeException {

    public WishlistItemNotFoundException(Long productivityId) {
        super("Wishlist item not found: product " + productivityId);
    }
}
