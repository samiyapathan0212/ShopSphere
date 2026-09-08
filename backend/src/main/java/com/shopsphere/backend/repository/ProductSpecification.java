package com.shopsphere.backend.repository;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;

import com.shopsphere.backend.domain.Product;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

/**
 * Specifications for dynamic {@link Product} queries. Combines search,
 * category, active-status and price-range filters into a single
 * {@link Specification} for efficient PostgreSQL execution.
 */
public final class ProductSpecification {

    private ProductSpecification() {
        // utility class
    }

    /**
     * Builds a specification combining all provided filters. All filters are
     * AND-combined. Null/blank parameters are ignored (no filtering).
     *
     * @param search     case-insensitive match on name or SKU (contains)
     * @param categoryId filter by category id
     * @param minPrice   minimum price (inclusive)
     * @param maxPrice   maximum price (inclusive)
     * @param active     filter by active status
     * @return combined specification
     */
    public static Specification<Product> withFilters(String search, Long categoryId,
            BigDecimal minPrice, BigDecimal maxPrice, Boolean active) {
        return (Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb) -> {
            Predicate predicate = cb.conjunction();

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate skuMatch = cb.like(cb.lower(root.get("sku")), pattern);
                predicate = cb.and(predicate, cb.or(nameMatch, skuMatch));
            }

            if (categoryId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("category").get("id"), categoryId));
            }

            if (active != null) {
                predicate = cb.and(predicate, cb.equal(root.get("active"), active));
            }

            if (minPrice != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("price"), maxPrice));
            }

            return predicate;
        };
    }
}