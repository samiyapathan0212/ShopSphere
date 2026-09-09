package com.shopsphere.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for adding/updating a cart item (Phase 4A).
 * Quantity must be positive; client-supplied prices are ignored.
 */
public record AddCartItemRequest(

        @NotNull(message = "productId is required")
        Long productId,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        @Max(value = 9999, message = "quantity must not exceed 9999")
        Integer quantity) {
}
