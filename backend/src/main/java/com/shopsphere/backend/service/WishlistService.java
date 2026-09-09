package com.shopsphere.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopsphere.backend.domain.Product;
import com.shopsphere.backend.domain.User;
import com.shopsphere.backend.domain.Wishlist;
import com.shopsphere.backend.dto.response.WishlistItemResponse;
import com.shopsphere.backend.dto.response.WishlistResponse;
import com.shopsphere.backend.exception.ProductNotFoundException;
import com.shopsphere.backend.exception.WishlistItemAlreadyExistsException;
import com.shopsphere.backend.exception.WishlistItemNotFoundException;
import com.shopsphere.backend.exception.WishlistNotFoundException;
import com.shopsphere.backend.mapper.CartMapper;
import com.shopsphere.backend.repository.ProductRepository;
import com.shopsphere.backend.repository.UserRepository;
import com.shopsphere.backend.repository.WishlistItemRepository;
import com.shopsphere.backend.repository.WishlistRepository;

/**
 * Wishlist operations (Phase 4B). One wishlist per user; customers can only
 * access their own wishlist. Inactive products are excluded from read results.
 */
@Service
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public WishlistService(WishlistRepository wishlistRepository,
                           WishlistItemRepository wishlistItemRepository,
                           ProductRepository productRepository, UserRepository userRepository) {
        this.wishlistRepository = wishlistRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
                this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public WishlistResponse getWishlist(Long userId) {
        Wishlist wishlist = requireWishlist(userId);
        List<WishlistItemResponse> items = wishlistItemRepository.findByWishlistId(wishlist.getId()).stream()
                .map(this::toItemResponse)
                .toList();
        return new WishlistResponse(wishlist.getId(), userId, items, items.size(),
                wishlist.getCreatedAt(), wishlist.getUpdatedAt());
    }

                    @Transactional
    public WishlistResponse addItem(Long userId, Long productId) {
        Wishlist wishlist = requireWishlist(userId);
        Product product = resolveActiveProduct(productId);

        if (wishlistItemRepository.existsByWishlistIdAndProductId(wishlist.getId(), productId)) {
            throw new WishlistItemAlreadyExistsException(productId);
        }

        com.shopsphere.backend.domain.WishlistItem item = com.shopsphere.backend.domain.WishlistItem.builder()
                .wishlist(wishlist)
                .product(product)
                .build();
        wishlistItemRepository.save(item);
        return getWishlist(userId);
    }

                @Transactional
    public WishlistResponse removeItem(Long userId, Long productId) {
        Wishlist wishlist = requireWishlist(userId);
        com.shopsphere.backend.domain.WishlistItem item = wishlistItemRepository
                .findByWishlistIdAndProductId(wishlist.getId(), productId)
                .orElseThrow(() -> new WishlistItemNotFoundException(productId));
        wishlistItemRepository.delete(item);
        return getWishlist(userId);
    }

    private Wishlist requireWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new WishlistNotFoundException(userId));
                    com.shopsphere.backend.domain.Wishlist wishlist = com.shopsphere.backend.domain.Wishlist.builder()
                            .user(user)
                            .build();
                    return wishlistRepository.save(wishlist);
                });
    }

                private Product resolveActiveProduct(Long productId) {
        return productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private WishlistItemResponse toItemResponse(com.shopsphere.backend.domain.WishlistItem item) {
        Product product = item.getProduct();
        // Ensure inactive products are never exposed in wishlist reads.
        if (!product.isActive()) {
            throw new ProductNotFoundException(product.getId());
        }
        return new WishlistItemResponse(
                item.getId(),
                product.getId(),
                product.getSku(),
                product.getName(),
                item.getCreatedAt(),
                item.getUpdatedAt());
    }
}
