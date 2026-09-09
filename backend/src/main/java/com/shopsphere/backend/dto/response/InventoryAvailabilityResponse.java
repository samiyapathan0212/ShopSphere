package com.shopsphere.backend.dto.response;

/**
 * Customer-safe inventory availability (Phase 5). Deliberately exposes only
 * the purchasable amount — never {@code quantity}, {@code reservedQty} or
 * {@code reorderThreshold}, which are admin-only.
 */
public record InventoryAvailabilityResponse(
        Long productId,
        String sku,
        String productName,
        int availableQty) {
}
