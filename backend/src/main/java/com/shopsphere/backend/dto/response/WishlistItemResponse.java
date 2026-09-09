package com.shopsphere.backend.dto.response;

import java.time.Instant;

/**
 * Response DTO for a wishlist item (Phase 4B).
 */
public record WishlistItemResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        Instant createdAt,
        Instant updatedAt) {
}
