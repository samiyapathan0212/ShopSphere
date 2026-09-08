package com.shopsphere.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating/updating a catalog category (Phase 3A
 * foundation; catalog controllers/services arrive in a later phase).
 */
public record CategoryRequest(

        @NotBlank(message = "name is required")
        @Size(max = 100, message = "name must not exceed 100 characters")
        String name,

        @Size(max = 1000, message = "description must not exceed 1000 characters")
        String description,

        /** Optional; defaults to {@code true} when omitted. */
        Boolean active) {
}