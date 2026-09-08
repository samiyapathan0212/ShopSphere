package com.shopsphere.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.shopsphere.backend.domain.Category;
import com.shopsphere.backend.domain.Product;
import com.shopsphere.backend.dto.request.ProductRequest;
import com.shopsphere.backend.dto.response.ProductResponse;
import com.shopsphere.backend.exception.CategoryNotFoundException;
import com.shopsphere.backend.exception.DuplicateSkuException;
import com.shopsphere.backend.exception.ProductNotFoundException;
import com.shopsphere.backend.repository.CategoryRepository;
import com.shopsphere.backend.repository.ProductRepository;

/**
 * Unit tests for {@link ProductService} (Phase 3B) with mocked repositories:
 * active-only reads, SKU uniqueness (409), category resolution (404), update
 * application and 404 handling.
 */
class ProductServiceTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T10:00:00Z");

    private ProductRepository productRepository;
    private CategoryRepository categoryRepository;
    private ProductService productService;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        productService = new ProductService(productRepository, categoryRepository);
    }

    private Category category(Long id, String name, boolean active) {
        return Category.builder()
                .id(id)
                .name(name)
                .description("desc")
                .active(active)
                .createdAt(CREATED)
                .updatedAt(CREATED)
                .build();
    }

    private Product product(Long id, String sku, Category cat, boolean active) {
        return Product.builder()
                .id(id)
                .sku(sku)
                .name("Name")
                .description("d")
                .price(new BigDecimal("19.99"))
                .active(active)
                .category(cat)
                .createdAt(CREATED)
                .updatedAt(CREATED)
                .build();
    }

    @Test
    void listActiveProductsReturnsMappedDtos() {
        Category cat = category(1L, "Electronics", true);
        when(productRepository.findAllByActiveTrue()).thenReturn(List.of(product(10L, "SKU-1", cat, true)));

        List<ProductResponse> responses = productService.listActiveProducts();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(10L);
        assertThat(responses.get(0).sku()).isEqualTo("SKU-1");
        assertThat(responses.get(0).price()).isEqualByComparingTo(new BigDecimal("19.99"));
        assertThat(responses.get(0).category().name()).isEqualTo("Electronics");
    }

    @Test
    void getActiveProductReturnsDtoWithCategory() {
        Category cat = category(1L, "Electronics", true);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product(10L, "SKU-1", cat, true)));

        ProductResponse response = productService.getActiveProduct(10L);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.category().id()).isEqualTo(1L);
    }

    @Test
    void getActiveProductThrowsNotFoundForUnknownId() {
        when(productRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getActiveProduct(404L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product not found: 404");
    }

    @Test
    void getActiveProductHidesInactiveProductsFromCustomers() {
        Category cat = category(1L, "Electronics", true);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product(10L, "SKU-1", cat, false)));

        assertThatThrownBy(() -> productService.getActiveProduct(10L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void createProductResolvesCategoryAndTrimsSku() {
        Category cat = category(1L, "Electronics", true);
        when(productRepository.existsBySku("SKU-9")).thenReturn(false);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(cat));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product incoming = invocation.getArgument(0);
            return Product.builder()
                    .id(10L).sku(incoming.getSku()).name(incoming.getName())
                    .description(incoming.getDescription()).price(incoming.getPrice())
                    .active(incoming.isActive()).category(incoming.getCategory())
                    .createdAt(CREATED).updatedAt(CREATED).build();
        });

        ProductResponse response = productService.createProduct(
                new ProductRequest(" SKU-9 ", "Keyboard", "Mechanical", new BigDecimal("79.90"), 1L, null));

        verify(productRepository).existsBySku("SKU-9");
        verify(categoryRepository).findById(1L);
        ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(captor.capture());
        assertThat(captor.getValue().getSku()).isEqualTo("SKU-9");
        assertThat(captor.getValue().getCategory()).isSameAs(cat);
        assertThat(response.sku()).isEqualTo("SKU-9");
    }

    @Test
    void createProductRejectsDuplicateSku() {
        when(productRepository.existsBySku("SKU-1")).thenReturn(true);

        assertThatThrownBy(() -> productService.createProduct(
                new ProductRequest("SKU-1", "Laptop", "Gaming", new BigDecimal("999.99"), 1L, null)))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessage("SKU is already registered: SKU-1");

        verify(productRepository, never()).save(any(Product.class));
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    void createProductRejectsUnknownCategory() {
        when(productRepository.existsBySku("SKU-2")).thenReturn(false);
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.createProduct(
                new ProductRequest("SKU-2", "Mouse", "Wireless", new BigDecimal("29.90"), 99L, null)))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category not found: 99");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void updateProductAppliesChanges() {
        Category oldCat = category(1L, "Electronics", true);
        Category newCat = category(2L, "Home", true);
        Product product = product(10L, "SKU-1", oldCat, true);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuAndIdNot("SKU-2", 10L)).thenReturn(false);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(newCat));
        when(productRepository.saveAndFlush(product)).thenReturn(product);

        ProductResponse response = productService.updateProduct(10L, new ProductRequest(
                "SKU-2", "Renamed", "New desc", new BigDecimal("29.99"), 2L, Boolean.FALSE));

        assertThat(response.sku()).isEqualTo("SKU-2");
        assertThat(response.name()).isEqualTo("Renamed");
        assertThat(response.price()).isEqualTo(new BigDecimal("29.99"));
        assertThat(response.active()).isFalse();
        assertThat(response.category().name()).isEqualTo("Home");
        assertThat(product.getSku()).isEqualTo("SKU-2");
        assertThat(product.getCategory()).isSameAs(newCat);
    }

    @Test
    void updateProductRejectsDuplicateSkuOnOtherProduct() {
        Product product = product(10L, "SKU-1", category(1L, "Electronics", true), true);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuAndIdNot("SKU-2", 10L)).thenReturn(true);

        assertThatThrownBy(() -> productService.updateProduct(10L, new ProductRequest(
                "SKU-2", "Renamed", "New desc", new BigDecimal("29.99"), 1L, true)))
                .isInstanceOf(DuplicateSkuException.class)
                .hasMessage("SKU is already registered: SKU-2");

        verify(productRepository, never()).saveAndFlush(any());
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    void updateProductRejectsUnknownCategory() {
        Product product = product(10L, "SKU-1", category(1L, "Electronics", true), true);
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.existsBySkuAndIdNot("SKU-1", 10L)).thenReturn(false);
        when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(10L, new ProductRequest(
                "SKU-1", "Name", "d", new BigDecimal("19.99"), 404L, true)))
                .isInstanceOf(CategoryNotFoundException.class);

        verify(productRepository, never()).saveAndFlush(any());
    }

    @Test
    void updateProductThrowsNotFoundForUnknownId() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.updateProduct(99L, new ProductRequest(
                "SKU-1", "Name", "d", new BigDecimal("19.99"), 1L, true)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void deleteProductDeletesExistingProduct() {
        Product existing = product(10L, "SKU-1", category(1L, "Electronics", true), true);
        when(productRepository.findById(10L)).thenReturn(Optional.of(existing));

        productService.deleteProduct(10L);

        verify(productRepository).delete(existing);
    }

    @Test
    void deleteProductThrowsNotFoundForUnknownId() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.deleteProduct(99L))
                .isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).delete(any(Product.class));
    }
}