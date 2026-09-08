package com.shopsphere.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.shopsphere.backend.domain.Category;
import com.shopsphere.backend.dto.request.CategoryRequest;
import com.shopsphere.backend.dto.response.CategoryResponse;
import com.shopsphere.backend.exception.CategoryInUseException;
import com.shopsphere.backend.exception.CategoryNotFoundException;
import com.shopsphere.backend.exception.DuplicateCategoryNameException;
import com.shopsphere.backend.repository.CategoryRepository;
import com.shopsphere.backend.repository.ProductRepository;

/**
 * Unit tests for {@link CategoryService} (Phase 3B) with mocked repositories:
 * active-only reads, name uniqueness (409), delete-in-use protection (409),
 * update application and 404 handling.
 */
class CategoryServiceTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T10:00:00Z");

    private CategoryRepository categoryRepository;
    private ProductRepository productRepository;
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        categoryRepository = mock(CategoryRepository.class);
        productRepository = mock(ProductRepository.class);
        categoryService = new CategoryService(categoryRepository, productRepository);
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

    @Test
    void listActiveCategoriesReturnsMappedDtos() {
        when(categoryRepository.findAllByActiveTrue())
                .thenReturn(List.of(category(1L, "Electronics", true)));

        List<CategoryResponse> responses = categoryService.listActiveCategories();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(0).name()).isEqualTo("Electronics");
        assertThat(responses.get(0).active()).isTrue();
    }

    @Test
    void getActiveCategoryReturnsDto() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Electronics", true)));

        CategoryResponse response = categoryService.getActiveCategory(1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Electronics");
    }

    @Test
    void getActiveCategoryThrowsNotFoundForUnknownId() {
        when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getActiveCategory(404L))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category not found: 404");
    }

    @Test
    void getActiveCategoryHidesInactiveCategoriesFromCustomers() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(category(2L, "Archived", false)));

        assertThatThrownBy(() -> categoryService.getActiveCategory(2L))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void createCategoryTrimsNameChecksUniquenessAndReturnsDto() {
        when(categoryRepository.existsByName("Electronics")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category incoming = invocation.getArgument(0);
            return Category.builder()
                    .id(1L).name(incoming.getName()).description(incoming.getDescription())
                    .active(incoming.isActive()).createdAt(CREATED).updatedAt(CREATED).build();
        });

        CategoryResponse response = categoryService.createCategory(
                new CategoryRequest("  Electronics  ", "Devices", null));

        verify(categoryRepository).existsByName("Electronics");
        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Electronics");
        assertThat(captor.getValue().isActive()).isTrue();
        assertThat(response.name()).isEqualTo("Electronics");
    }

    @Test
    void createCategoryRejectsDuplicateName() {
        when(categoryRepository.existsByName("Electronics")).thenReturn(true);

        assertThatThrownBy(() -> categoryService.createCategory(new CategoryRequest("Electronics", null, null)))
                .isInstanceOf(DuplicateCategoryNameException.class)
                .hasMessage("Category name is already in use: Electronics");

        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void createCategoryRejectsDuplicateNameOnUpdate() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Electronics", true)));
        when(categoryRepository.existsByNameAndIdNot("Gaming", 1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.updateCategory(1L,
                new CategoryRequest("Gaming", "Consoles", true)))
                .isInstanceOf(DuplicateCategoryNameException.class)
                .hasMessage("Category name is already in use: Gaming");

        verify(categoryRepository, never()).saveAndFlush(any(Category.class));
    }

    @Test
    void updateCategoryAppliesChangesAndReturnsDto() {
        Category managed = category(1L, "Electronics", true);
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(managed));
        when(categoryRepository.existsByNameAndIdNot("Gaming", 1L)).thenReturn(false);
        when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = categoryService.updateCategory(1L,
                new CategoryRequest("Gaming", "Consoles and games", false));

        // The managed entity was mutated by the domain update method.
        assertThat(managed.getName()).isEqualTo("Gaming");
        assertThat(managed.getDescription()).isEqualTo("Consoles and games");
        assertThat(managed.isActive()).isFalse();
        assertThat(response.name()).isEqualTo("Gaming");
        assertThat(response.active()).isFalse();
    }

    @Test
    void updateCategoryRejectsUnknownId() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.updateCategory(99L,
                new CategoryRequest("Gaming", null, null)))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category not found: 99");
    }

    @Test
    void deleteCategoryRemovesWhenNoProductsReferenceIt() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Electronics", true)));
        when(productRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).delete(any(Category.class));
    }

    @Test
    void deleteCategoryRejectsCategoryWithProducts() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category(1L, "Electronics", true)));
        when(productRepository.existsByCategoryId(1L)).thenReturn(true);

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(CategoryInUseException.class)
                .hasMessage("Category has products and cannot be deleted");

        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void deleteCategoryThrowsNotFoundForUnknownId() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.deleteCategory(99L))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category not found: 99");

        verify(categoryRepository, never()).delete(any(Category.class));
        verify(productRepository, never()).existsByCategoryId(any());
    }
}