package com.shopsphere.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request payload for updating a cart item quantity (Phase 4A).
 */
public record UpdateCartItemRequest(

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        @Max(value = 9999, message = "quantity must not exceed 9999")
        Integer quantity) {
}
