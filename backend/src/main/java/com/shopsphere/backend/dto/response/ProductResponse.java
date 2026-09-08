package com.shopsphere.backend.dto.response;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * Response DTO for a catalog product. Exposes API fields only — internal
 * entities are never returned directly; the category is embedded as a
 * {@link CategoryResponse}.
 */
public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        BigDecimal price,
        boolean active,
        CategoryResponse category,
        Instant createdAt,
        Instant updatedAt) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}