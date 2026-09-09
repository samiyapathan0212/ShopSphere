package com.shopsphere.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for the admin inventory update endpoint (Phase 5).
 * Negative values are rejected at validation time; the entity additionally
 * refuses a quantity below the currently reserved amount.
 */
public record UpdateInventoryRequest(

        @NotNull(message = "quantity is required")
        @Min(value = 0, message = "quantity must not be negative")
        Integer quantity,

        @NotNull(message = "reorderThreshold is required")
        @Min(value = 0, message = "reorderThreshold must not be negative")
        Integer reorderThreshold) {
}
