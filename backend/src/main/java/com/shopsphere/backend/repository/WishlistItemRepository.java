package com.shopsphere.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shopsphere.backend.domain.WishlistItem;

/**
 * Wishlist item repository (Phase 4B).
 */
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    Optional<WishlistItem> findByWishlistIdAndProductId(Long wishlistId, Long productId);

    java.util.List<WishlistItem> findByWishlistId(Long wishlistId);

    boolean existsByWishlistIdAndProductId(Long wishlistId, Long productId);
}
