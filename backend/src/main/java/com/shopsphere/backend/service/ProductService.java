package com.shopsphere.backend.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopsphere.backend.config.CacheConfig;
import com.shopsphere.backend.config.CacheConfig.CacheProperties;
import com.shopsphere.backend.domain.Category;
import com.shopsphere.backend.domain.Product;
import com.shopsphere.backend.dto.request.ProductRequest;
import com.shopsphere.backend.dto.response.PageResponse;
import com.shopsphere.backend.dto.response.ProductResponse;
import com.shopsphere.backend.exception.CategoryNotFoundException;
import com.shopsphere.backend.exception.DuplicateSkuException;
import com.shopsphere.backend.exception.InvalidQueryParameterException;
import com.shopsphere.backend.exception.ProductNotFoundException;
import com.shopsphere.backend.mapper.ProductMapper;
import com.shopsphere.backend.repository.CategoryRepository;
import com.shopsphere.backend.repository.ProductRepository;
import com.shopsphere.backend.repository.ProductSpecification;

/**
 * Catalog product operations. Reads are available to CUSTOMER and ADMIN;
 * customers see active products only, admins may additionally see inactive
 * products in listings. Writes are ADMIN-only and enforced at the controller
 * via method security. Listing supports pagination, case-insensitive search
 * on name/SKU, category/price filters, and safe sorting on a field/direction
 * whitelist; requests outlive the configured maximum page size are clamped.
 * Read results are cached through Spring's Cache abstraction ({@link CatalogCacheSupport}) -
 * keys include every query parameter and every admin mutation evicts the affected
 * catalog caches. Cache failures (e.g. Redis unavailable) degrade to live DB reads.
 */
@Service
public class ProductService {


    /** Whitelisted sortable fields (mapped 1:1 to entity property names). */
    private static final List<String> ALLOWED_SORT_FIELDS = List.of("name", "price", "createdAt");

    /** Whitelisted sort directions (case-insensitive). */
    private static final List<String> ALLOWED_DIRECTIONS = List.of("asc", "desc");


    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CatalogCacheSupport cacheSupport;
    private final CacheProperties cacheProperties;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
                            CatalogCacheSupport cacheSupport, CacheProperties cacheProperties) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.cacheSupport = cacheSupport;
        this.cacheProperties = cacheProperties;
    }

    /**
     * Paginated catalog listing with optional search/filters/sort. For customers
     * ({@code includeInactive=false}) only active products are returned; admins may
     * request inactive products too. Min/max prices are inclusive; searching matches
     * name OR SKU case-insensitively (substring). The page size is validated and
     * clamped to {@link CacheProperties#getMaxPageSize()}; sort fields and
     * directions are whitelisted through {@link #ALLOWED_SORT_FIELDS} and
     * {@link #ALLOWED_DIRECTIONS}.
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> listProducts(String search, Long categoryId, BigDecimal minPrice,
                                                       BigDecimal maxPrice, int page, int size, String sort,
                                                       String direction, boolean includeInactive) {
        validateQuery(page, size, minPrice, maxPrice, sort, direction);
        int effectiveSize = Math.min(size, cacheProperties.getMaxPageSize());
        Sort springSort = buildSort(sort, direction);
        Pageable pageable = PageRequest.of(page, effectiveSize, springSort);
        String key = productListKey(search, categoryId, minPrice, maxPrice, page, effectiveSize, sort,
                direction, includeInactive);
        return cacheSupport.getCached(CacheConfig.PRODUCTS_CACHE, key, () -> {
            Specification<Product> spec = ProductSpecification.withFilters(search, categoryId, minPrice,
                    maxPrice, includeInactive ? null : Boolean.TRUE);
            Page<Product> result = productRepository.findAll(spec, pageable);
            return PageResponse.from(result.map(ProductMapper::toResponse));
        });
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listActiveProducts() {
        return cacheSupport.getCached(CacheConfig.PRODUCTS_CACHE, "list-active", () ->
                productRepository.findAllByActiveTrue().stream()
                        .map(ProductMapper::toResponse)
                        .toList());
    }

    @Transactional(readOnly = true)
    public ProductResponse getActiveProduct(Long id) {
        return cacheSupport.getCached(CacheConfig.PRODUCT_CACHE, "product:" + id, () -> {
            Product product = productRepository.findById(id)
                    .filter(Product::isActive)
                    .orElseThrow(() -> new ProductNotFoundException(id));
            return ProductMapper.toResponse(product);
        });
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        String sku = request.sku().trim();
        if (productRepository.existsBySku(sku)) {
            throw new DuplicateSkuException(sku);
        }
        Category category = resolveCategory(request.categoryId());
        Product saved = productRepository.save(ProductMapper.toEntity(normalize(request, sku), category));
        evictProductCaches();
        return ProductMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        String sku = request.sku().trim();
        if (productRepository.existsBySkuAndIdNot(sku, id)) {
            throw new DuplicateSkuException(sku);
        }
        Category category = resolveCategory(request.categoryId());
        product.update(sku, request.name(), request.description(), request.price(), category,
                request.active() == null || request.active());
        // Flush so @UpdateTimestamp has applied before mapping the response.
        Product flushed = productRepository.saveAndFlush(product);
        evictProductCaches();
        return ProductMapper.toResponse(flushed);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        productRepository.delete(product);
        evictProductCaches();
    }

    /**
     * Rejects invalid query parameters before any cache/database work. Page size
     * above the configured maximum is clamped, everything else invalid is rejected.
     */
    private void validateQuery(int page, int size, BigDecimal minPrice, BigDecimal maxPrice,
                               String sort, String direction) {
        if (page < 0) {
            throw new InvalidQueryParameterException("page must not be negative");
        }
        if (size < 1) {
            throw new InvalidQueryParameterException("size must be at least 1");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidQueryParameterException("minPrice must not be greater than maxPrice");
        }
        if (sort != null && !sort.isBlank() && ALLOWED_SORT_FIELDS.stream()
                .noneMatch(allowed -> allowed.equalsIgnoreCase(sort))) {
            throw new InvalidQueryParameterException("unsupported sort field: " + sort);
        }
        if (direction != null && !direction.isBlank() && ALLOWED_DIRECTIONS.stream()
                .noneMatch(allowed -> allowed.equalsIgnoreCase(direction))) {
            throw new InvalidQueryParameterException("unsupported sort direction: " + direction);
        }
    }

    /** Builds a Spring Data sort from whitelisted fields/directions. */
    private Sort buildSort(String sort, String direction) {
        // Map the (case-insensitive) request value back to the exact entity
        // property name: sort paths are resolved case-sensitively by Spring Data,
        // so the camelCase property must never be lowercased.
        String field = (sort == null || sort.isBlank()) ? "createdAt"
                : ALLOWED_SORT_FIELDS.stream()
                      .filter(allowed -> allowed.equalsIgnoreCase(sort))
                      .findFirst()
                      .orElse("createdAt");

        boolean ascending = direction == null || direction.isBlank()
                || "asc".equalsIgnoreCase(direction);

        return Sort.by(ascending ? Direction.ASC : Direction.DESC, field)
                .and(Sort.by(Direction.ASC, "id"));
    }

    /** Cache key covering every input that affects listing results. */
    private String productListKey(String search, Long categoryId, BigDecimal minPrice, BigDecimal maxPrice,
                                  int page, int size, String sort, String direction, boolean includeInactive) {
        return "list:" + page + ":" + size + ":" + (search == null ? "" : search.trim().toLowerCase()) + ":"
                + (categoryId == null ? "" : categoryId) + ":" + (minPrice == null ? "" : minPrice) + ":"
                + (maxPrice == null ? "" : maxPrice) + ":" + (sort == null ? "" : sort.trim().toLowerCase()) + ":"
                + (direction == null ? "" : direction.trim().toLowerCase()) + ":" + includeInactive;
    }

    /** Every product must reference an existing category; otherwise 404. */
    private Category resolveCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    private ProductRequest normalize(ProductRequest request, String sku) {
        return new ProductRequest(sku, request.name(), request.description(), request.price(),
                request.categoryId(), request.active());
    }

    /** Admin product mutations affect listings and product details only. */
    private void evictProductCaches() {
        cacheSupport.evict(CacheConfig.PRODUCTS_CACHE, CacheConfig.PRODUCT_CACHE);
    }
}
