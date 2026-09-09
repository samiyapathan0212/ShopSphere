package com.shopsphere.backend.mapper;

import com.shopsphere.backend.domain.Cart;
import com.shopsphere.backend.domain.CartItem;
import com.shopsphere.backend.dto.response.CartItemResponse;
import com.shopsphere.backend.dto.response.CartResponse;

/**
 * Maps cart and cart item entities to response DTOs.
 */
public final class CartMapper {

    private CartMapper() {
    }

    public static CartItemResponse toItemResponse(CartItem item) {
        return new CartItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getSku(),
                item.getProduct().getName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }

    public static CartResponse toResponse(Cart cart, java.util.List<CartItemResponse> items,
                                          java.math.BigDecimal subtotal, int itemCount,
                                          int itemCountWithQuantity) {
        return new CartResponse(cart.getId(), cart.getUser().getId(), items, subtotal,
                itemCount, itemCountWithQuantity, cart.getCreatedAt(), cart.getUpdatedAt());
    }
}
