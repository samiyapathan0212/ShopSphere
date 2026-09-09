package com.shopsphere.backend.dto.response;

import java.math.BigDecimal;

/**
 * Response DTO for an order line item (Phase 5). Serves the immutable
 * purchase-time snapshots, not live catalog values.
 */
public record OrderItemResponse(
        Long id,
        Long productId,
        String productName,
        String sku,
        BigDecimal priceAtPurchase,
        int quantity,
        BigDecimal lineTotal) {
}
