package com.shopsphere.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.shopsphere.backend.domain.Product;

/**
 * Spring Data repository for {@link Product}.
 * Lookups support the future catalog APIs: SKU uniqueness checks, product
 * listings and per-category/active filtering. Extends
 * {@link JpaSpecificationExecutor} for dynamic search/filter/sort queries.
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    /** Conflict check for future updates (a different row with the same SKU). */
    boolean existsBySkuAndIdNot(String sku, Long id);

    List<Product> findAllByActiveTrue();

    List<Product> findAllByCategoryId(Long categoryId);

    List<Product> findAllByCategoryIdAndActiveTrue(Long categoryId);

    /** Used to block deleting a category that still has products (service pre-check). */
    boolean existsByCategoryId(Long categoryId);
}