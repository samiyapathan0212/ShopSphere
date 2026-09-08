package com.shopsphere.backend.controller;

import java.math.BigDecimal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import com.shopsphere.backend.dto.request.ProductRequest;
import com.shopsphere.backend.dto.response.PageResponse;
import com.shopsphere.backend.dto.response.ProductResponse;
import com.shopsphere.backend.service.ProductService;

/**
 * Product catalog endpoints (Phase 3B + 3C/3D). Reads are available to
 * CUSTOMER and ADMIN; customers see active products only, admins may see
 * inactive products in the paginated listing too. Responses are DTOs, never
 * entities. The listing supports pagination, case-insensitive search (name/SKU),
 * category and price filters, and safe sorting on name/price/createdAt with
 * asc/desc direction. Writes are ADMIN-only via method security.
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product catalog")
public class ProductController {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    @Operation(summary = "List products with pagination, search, filters and sorting",
            description = "Customers see active products only; admins additionally see inactive products."
                    + " Supports case-insensitive search on name/SKU, categoryId and minPrice/maxPrice filters,"
                    + " and safe sorting by name/price/createdAt in asc/desc order. Returns a PageResponse.")
    public PageResponse<ProductResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "asc") String direction,
            Authentication authentication) {
        boolean includeInactive = isAdmin(authentication);
        return productService.listProducts(search, categoryId, minPrice, maxPrice, page, size, sort,
                direction, includeInactive);
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

    private static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_ADMIN.equals(authority.getAuthority()));
    }
}
