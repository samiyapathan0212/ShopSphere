package com.shopsphere.backend.exception;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(Long productivityId) {
        super("Cart item not found: product " + productivityId);
    }
}
