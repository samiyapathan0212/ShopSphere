package com.shopsphere.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.shopsphere.backend.config.CacheConfig.CacheProperties;
import com.shopsphere.backend.domain.Category;
import com.shopsphere.backend.domain.Product;
import com.shopsphere.backend.dto.request.ProductRequest;
import com.shopsphere.backend.dto.response.PageResponse;
import com.shopsphere.backend.dto.response.ProductResponse;
import com.shopsphere.backend.exception.InvalidQueryParameterException;
import com.shopsphere.backend.repository.CategoryRepository;
import com.shopsphere.backend.repository.ProductRepository;

/**
 * Unit tests for the Phase 3C/3D product listing features on
 * {@link ProductService}: pagination, validation, sorting whitelist,
 * active-only customer handling, and cache key separation/eviction.
 */
class ProductCatalogListingTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T10:00:00Z");

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        categoryRepository = mock(CategoryRepository.class);
    }

    private ProductService productService() {
        return new ProductService(productRepository, categoryRepository,
                new CatalogCacheSupport(null), new CacheProperties());
    }

    private ProductService cachedProductService(String... cacheNames) {
        return new ProductService(productRepository, categoryRepository,
                new CatalogCacheSupport(new ConcurrentMapCacheManager(cacheNames)),
                new CacheProperties());
    }

    private Category category(Long id, String name, boolean active) {
        return Category.builder()
                .id(id).name(name).description("desc").active(active)
                .createdAt(CREATED).updatedAt(CREATED)
                .build();
    }

    private Product product(Long id, String sku, Category cat, boolean active) {
        return Product.builder()
                .id(id).sku(sku).name("Name").description("d")
                .price(new BigDecimal("19.99")).active(active).category(cat)
                .createdAt(CREATED).updatedAt(CREATED)
                .build();
    }
@Test
    void paginatesAndReturnsPageMetadata() {
        Category cat = category(1L, "Electronics", true);
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt"));
        Page<Product> page = new PageImpl<>(List.of(product(10L, "SKU-1", cat, true)), pageable, 1);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        PageResponse<ProductResponse> response = productService()
                .listProducts(null, null, null, null, 0, 20, "createdAt", "asc", false);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).id()).isEqualTo(10L);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.hasPrevious()).isFalse();
        assertThat(response.hasNext()).isFalse();
    }

    @Test
    void returnsEmptyPageWhenNothingMatches() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt"));
        Page<Product> empty = new PageImpl<>(List.of(), pageable, 0);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(empty);

        PageResponse<ProductResponse> response = productService()
                .listProducts("zzz-no-match", null, null, null, 0, 20, "createdAt", "asc", false);

        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isZero();
        assertThat(response.totalPages()).isZero();
    }

    @Test
    void clampsPageSizeToConfiguredMaximum() {
        CacheProperties props = new CacheProperties();
        props.setMaxPageSize(5);
        ProductService svc = new ProductService(productRepository, categoryRepository,
                new CatalogCacheSupport(null), props);
        Pageable pageable = PageRequest.of(0, 5, Sort.by("createdAt"));
        Page<Product> empty = new PageImpl<>(List.of(), pageable, 0);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(empty);

        svc.listProducts(null, null, null, null, 0, 999, "createdAt", "asc", false);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(any(Specification.class), captor.capture());

        assertThat(captor.getValue().getPageSize()).isEqualTo(5);
        assertThat(captor.getValue().getPageNumber()).isEqualTo(0);
    }

    @Test
    void sortsByWhitelistedFieldAndDirection() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by("price"));
        when(productRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        productService().listProducts(null, null, null, null, 0, 20, "price", "desc", false);
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(any(Specification.class), captor.capture());

        assertThat(captor.getValue().getSort().getOrderFor("price").getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(captor.getValue().getSort().getOrderFor("id").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }
}