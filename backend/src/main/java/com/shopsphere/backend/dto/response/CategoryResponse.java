package com.shopsphere.backend.dto.response;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

/**
 * Response DTO for a catalog category. Exposes API fields only — internal
 * entities are never returned directly.
 */
public record CategoryResponse(
        Long id,
        String name,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}