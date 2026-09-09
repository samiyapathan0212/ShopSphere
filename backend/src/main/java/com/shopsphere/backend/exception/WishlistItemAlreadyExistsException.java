package com.shopsphere.backend.exception;

public class WishlistItemAlreadyExistsException extends RuntimeException {

    public WishlistItemAlreadyExistsException(Long productivityId) {
        super("Product is already in wishlist: " + productivityId);
    }
}
