package com.shopsphere.backend.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating/updating a catalog product (Phase 3A
 * foundation). SKU/category-name uniqueness is enforced by the database
 * ({@code uq_products_sku}, {@code uq_categories_name}) and will be checked
 * by the future service through the repository lookup methods.
 */
public record ProductRequest(

        @NotBlank(message = "sku is required")
        @Size(max = 64, message = "sku must not exceed 64 characters")
        String sku,

        @NotBlank(message = "name is required")
        @Size(max = 150, message = "name must not exceed 150 characters")
        String name,

        @Size(max = 2000, message = "description must not exceed 2000 characters")
        String description,

        @NotNull(message = "price is required")
        @Positive(message = "price must be positive")
        @Digits(integer = 10, fraction = 2, message = "price supports at most 10 integer digits and 2 decimals")
        BigDecimal price,

        /** Required: every product belongs to exactly one category. */
        @NotNull(message = "category is required")
        Long categoryId,

        /** Optional; defaults to {@code true} when omitted. */
        Boolean active) {
}