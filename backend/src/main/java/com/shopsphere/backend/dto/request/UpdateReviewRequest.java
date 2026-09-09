package com.shopsphere.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating a product review (Phase 4C).
 */
public record UpdateReviewRequest(

        @NotNull(message = "rating is required")
        @Min(value = 1, message = "rating must be at least 1")
        @Max(value = 5, message = "rating must be at most 5")
        Integer rating,

        @Size(max = 255, message = "title must not exceed 255 characters")
        String title,

        @NotBlank(message = "text is required")
        @Size(max = 2000, message = "text must not exceed 2000 characters")
        String text) {
}
