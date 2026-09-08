package com.shopsphere.backend.mapper;

import com.shopsphere.backend.domain.Category;
import com.shopsphere.backend.domain.Product;
import com.shopsphere.backend.dto.request.ProductRequest;
import com.shopsphere.backend.dto.response.ProductResponse;

/**
 * Maps between {@link Product} entities and catalog DTOs. Responses expose
 * API fields only — entities are never returned directly; the category is
 * embedded as a {@link com.shopsphere.backend.dto.response.CategoryResponse}.
 */
public final class ProductMapper {

    private ProductMapper() {
    }

    /**
     * Builds a new entity from a validated request and its resolved category;
     * {@code active} defaults to true.
     */
    public static Product toEntity(ProductRequest request, Category category) {
        return Product.builder()
                .sku(request.sku())
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .active(request.active() == null || request.active())
                .category(category)
                .build();
    }

    /**
     * Maps a product (and its nested category) to the response DTO. The
     * category association is lazy, so call this inside a transaction.
     */
    public static ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.isActive(),
                CategoryMapper.toResponse(product.getCategory()),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}