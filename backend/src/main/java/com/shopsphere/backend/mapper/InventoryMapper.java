package com.shopsphere.backend.mapper;

import com.shopsphere.backend.domain.Inventory;
import com.shopsphere.backend.domain.Product;
import com.shopsphere.backend.dto.response.InventoryAvailabilityResponse;
import com.shopsphere.backend.dto.response.InventoryResponse;

/**
 * Maps inventory entities to response DTOs (Phase 5). The admin view carries
 * every field; the customer view deliberately exposes only availability.
 */
public final class InventoryMapper {

    private InventoryMapper() {
    }

    public static InventoryResponse toResponse(Inventory inventory) {
        Product product = inventory.getProduct();
        return new InventoryResponse(
                inventory.getId(),
                product.getId(),
                product.getSku(),
                product.getName(),
                inventory.getQuantity(),
                inventory.getReservedQty(),
                inventory.getAvailableQty(),
                inventory.getReorderThreshold(),
                inventory.isLowStock(),
                inventory.getCreatedAt(),
                inventory.getUpdatedAt());
    }

    public static InventoryAvailabilityResponse toAvailabilityResponse(Inventory inventory) {
        Product product = inventory.getProduct();
        return new InventoryAvailabilityResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                inventory.getAvailableQty());
    }
}
