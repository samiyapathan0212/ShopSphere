package com.shopsphere.backend.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * Request payload for adding a product to the wishlist (Phase 4B).
 * productId is taken from the path; no body is required.
 */
public record AddWishlistItemRequest(
        @NotNull(message = "productId is required")
        Long productId) {
}
