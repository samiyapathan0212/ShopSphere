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

import com.shopsphere.backend.dto.request.ProductRequest;
import com.shopsphere.backend.dto.response.ProductResponse;
import com.shopsphere.backend.service.ProductService;

/**
 * Product catalog endpoints (Phase 3B). Reads are available to CUSTOMER and
 * ADMIN and expose active products only; create/update/delete are ADMIN-only
 * via method security. Responses are DTOs, never entities. No pagination,
 * search, filter or sort yet (later phase).
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product catalog")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "List active products",
            description = "Returns all active products. Requires CUSTOMER or ADMIN.")
    public List<ProductResponse> list() {
        return productService.listActiveProducts();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "Get an active product by id",
            description = "Returns one active product with its category. Inactive or unknown ids return 404.")
    public ProductResponse get(@PathVariable Long id) {
        return productService.getActiveProduct(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a product (admin only)",
            description = "Creates a product in an existing category. Duplicate SKUs return 409; unknown categories return 404.")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a product (admin only)",
            description = "Updates an existing product. Duplicate SKUs return 409; unknown ids/categories return 404.")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return productService.updateProduct(id, request);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a product (admin only)",
            description = "Deletes a product. Unknown ids return 404.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }
}