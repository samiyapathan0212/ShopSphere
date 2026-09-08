package com.shopsphere.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import com.shopsphere.backend.dto.request.CategoryRequest;
import com.shopsphere.backend.dto.response.CategoryResponse;
import com.shopsphere.backend.service.CategoryService;

/**
 * Category catalog endpoints (Phase 3B). Reads are available to CUSTOMER and
 * ADMIN and expose active categories only; create/update/delete are
 * ADMIN-only via method security. Responses are DTOs, never entities.
 */
@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Product catalog categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "List active categories",
            description = "Returns all active categories. Requires CUSTOMER or ADMIN.")
    public List<CategoryResponse> list() {
        return categoryService.listActiveCategories();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Get an active category by id",
            description = "Returns one active category. Inactive or unknown ids return 404.")
    public CategoryResponse get(@PathVariable Long id) {
        return categoryService.getActiveCategory(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a category (admin only)",
            description = "Creates a category. Duplicate names return 409.")
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a category (admin only)",
            description = "Updates an existing category. Duplicate names return 409; unknown ids return 404.")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return categoryService.updateCategory(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a category (admin only)",
            description = "Deletes a category that has no products. Unknown ids return 404; categories still in use return 409.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}