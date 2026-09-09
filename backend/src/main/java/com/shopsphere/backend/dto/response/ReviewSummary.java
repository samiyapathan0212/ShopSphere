package com.shopsphere.backend.dto.response;

import java.math.BigDecimal;

/**
 * Summary metadata for a product's reviews (Phase 4C).
 */
public record ReviewSummary(
        Long productId,
        int reviewCount,
        BigDecimal averageRating) {
}
