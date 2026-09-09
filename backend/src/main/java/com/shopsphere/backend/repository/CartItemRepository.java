package com.shopsphere.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shopsphere.backend.domain.CartItem;

/**
 * Cart item repository (Phase 4A).
 */
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    java.util.List<CartItem> findByCartId(Long cartId);

    void deleteByCartId(Long cartId);

    boolean existsByCartIdAndProductId(Long cartId, Long productId);
}
