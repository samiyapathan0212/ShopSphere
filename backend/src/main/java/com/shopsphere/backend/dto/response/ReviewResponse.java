package com.shopsphere.backend.dto.response;

import java.time.Instant;
import java.util.Objects;

/**
 * Response DTO for a product review (Phase 4C).
 * Does not expose password/security fields.
 */
public record ReviewResponse(
        Long id,
        Long productId,
        Long userId,
        String customerName,
        int rating,
        String title,
        String text,
        Instant createdAt,
        Instant updatedAt) {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReviewResponse that)) return false;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
