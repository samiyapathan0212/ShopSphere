package com.shopsphere.backend.mapper;

import com.shopsphere.backend.domain.Category;
import com.shopsphere.backend.dto.request.CategoryRequest;
import com.shopsphere.backend.dto.response.CategoryResponse;

/**
 * Maps between {@link Category} entities and catalog DTOs. Responses expose
 * API fields only — entities are never returned directly.
 */
public final class CategoryMapper {

    private CategoryMapper() {
    }

    /** Builds a new entity from a validated request; {@code active} defaults to true. */
    public static Category toEntity(CategoryRequest request) {
        return Category.builder()
                .name(request.name())
                .description(request.description())
                .active(request.active() == null || request.active())
                .build();
    }

    public static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}