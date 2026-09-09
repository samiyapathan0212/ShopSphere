package com.shopsphere.backend.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Response DTO for a cart (Phase 4A).
 */
public record CartResponse(
        Long id,
        Long userId,
        List<CartItemResponse> items,
        BigDecimal subtotal,
        int itemCount,
        int itemCountWithQuantity,
        Instant createdAt,
        Instant updatedAt) {
}
