package com.shopsphere.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shopsphere.backend.domain.Category;

/**
 * Spring Data repository for {@link Category}.
 * Lookups support the future catalog APIs: uniqueness checks, category
 * listings and active-only filtering.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    boolean existsByName(String name);

    /** Conflict check for future updates (a different row with the same name). */
    boolean existsByNameAndIdNot(String name, Long id);

    List<Category> findAllByActiveTrue();
}