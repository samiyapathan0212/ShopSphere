package com.shopsphere.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopsphere.backend.domain.Category;
import com.shopsphere.backend.domain.Product;
import com.shopsphere.backend.dto.request.ProductRequest;
import com.shopsphere.backend.dto.response.ProductResponse;
import com.shopsphere.backend.exception.CategoryNotFoundException;
import com.shopsphere.backend.exception.DuplicateSkuException;
import com.shopsphere.backend.exception.ProductNotFoundException;
import com.shopsphere.backend.mapper.ProductMapper;
import com.shopsphere.backend.repository.CategoryRepository;
import com.shopsphere.backend.repository.ProductRepository;

/**
 * Catalog product operations (Phase 3B). Reads are for CUSTOMER/ADMIN and
 * expose active products only. ADMIN writes enforce SKU uniqueness (409),
 * require an existing category for every product (404 otherwise) and return
 * DTOs, never entities. SKU values are trimmed before storage/uniqueness
 * checks. Mapping runs inside transactions because the category association
 * is lazy.
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> listActiveProducts() {
        return productRepository.findAllByActiveTrue().stream()
                .map(ProductMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getActiveProduct(Long id) {
        Product product = productRepository.findById(id)
                .filter(Product::isActive)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return ProductMapper.toResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        String sku = request.sku().trim();
        if (productRepository.existsBySku(sku)) {
            throw new DuplicateSkuException(sku);
        }
        Category category = resolveCategory(request.categoryId());
        Product saved = productRepository.save(ProductMapper.toEntity(normalize(request, sku), category));
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
        return ProductMapper.toResponse(flushed);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        productRepository.delete(product);
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
}