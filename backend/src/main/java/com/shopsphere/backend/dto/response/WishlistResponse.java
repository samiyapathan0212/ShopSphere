package com.shopsphere.backend.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Response DTO for a wishlist (Phase 4B).
 */
public record WishlistResponse(
        Long id,
        Long userId,
        List<WishlistItemResponse> items,
        int itemCount,
        Instant createdAt,
        Instant updatedAt) {
}
