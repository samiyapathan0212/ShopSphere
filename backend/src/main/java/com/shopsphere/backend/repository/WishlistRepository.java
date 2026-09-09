package com.shopsphere.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shopsphere.backend.domain.Wishlist;

/**
 * Wishlist repository (Phase 4B).
 * One wishlist per user.
 */
public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
