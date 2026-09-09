package com.shopsphere.backend.dto.response;

import java.time.Instant;

/**
 * Admin inventory response (Phase 5). Reserved quantities and reorder
 * thresholds are admin-only details — customer-facing responses use
 * {@link InventoryAvailabilityResponse}, which omits every field here.
 */
public record InventoryResponse(
        Long id,
        Long productId,
        String sku,
        String productName,
        int quantity,
        int reservedQty,
        int availableQty,
        int reorderThreshold,
        boolean lowStock,
        Instant createdAt,
        Instant updatedAt) {
}
