package com.shopsphere.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopsphere.backend.config.CacheConfig;
import com.shopsphere.backend.domain.Category;
import com.shopsphere.backend.dto.request.CategoryRequest;
import com.shopsphere.backend.dto.response.CategoryResponse;
import com.shopsphere.backend.exception.CategoryInUseException;
import com.shopsphere.backend.exception.CategoryNotFoundException;
import com.shopsphere.backend.exception.DuplicateCategoryNameException;
import com.shopsphere.backend.mapper.CategoryMapper;
import com.shopsphere.backend.repository.CategoryRepository;
import com.shopsphere.backend.repository.ProductRepository;

/**
 * Catalog category operations. Reads are available to CUSTOMER and ADMIN and
 * expose active categories only; results are cached through Spring's Cache
 * abstraction ({@link CatalogCacheSupport}). ADMIN writes enforce name
 * uniqueness (409), refuse to delete categories that still have products (409)
 * and return DTOs, never entities. Every category mutation evicts the whole
 * catalog cache space because product DTOs embed category data and listings are
 * filterable by category. Names are trimmed before storage/uniqueness checks.
 */
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CatalogCacheSupport cacheSupport;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository,
                            CatalogCacheSupport cacheSupport) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.cacheSupport = cacheSupport;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listActiveCategories() {
        return cacheSupport.getCached(CacheConfig.CATEGORIES_CACHE, "all", () ->
                categoryRepository.findAllByActiveTrue().stream()
                        .map(CategoryMapper::toResponse)
                        .toList());
    }

    @Transactional(readOnly = true)
    public CategoryResponse getActiveCategory(Long id) {
        return cacheSupport.getCached(CacheConfig.CATEGORY_CACHE, "category:" + id, () -> {
            Category category = categoryRepository.findById(id)
                    .filter(Category::isActive)
                    .orElseThrow(() -> new CategoryNotFoundException(id));
            return CategoryMapper.toResponse(category);
        });
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByName(name)) {
            throw new DuplicateCategoryNameException(name);
        }
        Category saved = categoryRepository.save(CategoryMapper.toEntity(normalize(request, name)));
        evictCatalogCaches();
        return CategoryMapper.toResponse(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        String name = request.name().trim();
        if (categoryRepository.existsByNameAndIdNot(name, id)) {
            throw new DuplicateCategoryNameException(name);
        }
        category.update(name, request.description(), request.active() == null || request.active());
        // Flush so @UpdateTimestamp has applied before mapping the response.
        Category flushed = categoryRepository.saveAndFlush(category);
        evictCatalogCaches();
        return CategoryMapper.toResponse(flushed);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CategoryNotFoundException(id));
        if (productRepository.existsByCategoryId(id)) {
            // Pre-check gives a clean 409; the fk_products_category constraint
            // remains the concurrency backstop.
            throw new CategoryInUseException();
        }
        categoryRepository.delete(category);
        evictCatalogCaches();
    }

    /**
     * Category mutations change category DTOs (and the category data embedded in
     * every product DTO, plus listings filterable by category), so evict the
     * entire catalog cache space to avoid serving stale category/product data.
     */
    private void evictCatalogCaches() {
        cacheSupport.evict(CacheConfig.CATEGORIES_CACHE, CacheConfig.CATEGORY_CACHE,
                CacheConfig.PRODUCTS_CACHE, CacheConfig.PRODUCT_CACHE);
    }

    private CategoryRequest normalize(CategoryRequest request, String name) {
        return new CategoryRequest(name, request.description(), request.active());
    }
}
