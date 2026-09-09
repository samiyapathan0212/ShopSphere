package com.shopsphere.backend.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for a cart item (Phase 4A).
 * Prices come from the database product, never from the client.
 */
public record CartItemResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal subtotal,
        Instant createdAt,
        Instant updatedAt) {
}
