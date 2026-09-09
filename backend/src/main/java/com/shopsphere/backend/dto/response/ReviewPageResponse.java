package com.shopsphere.backend.dto.response;

/**
 * Paginated response wrapper for review listings (Phase 4C).
 */
public record ReviewPageResponse(
        java.util.List<ReviewResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious) {
}
