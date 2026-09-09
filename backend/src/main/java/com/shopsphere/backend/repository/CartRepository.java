package com.shopsphere.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.shopsphere.backend.domain.Cart;
import com.shopsphere.backend.domain.User;

/**
 * Cart repository (Phase 4A).
 * One cart per user (enforced by a unique FK on carts.user_id).
 */
public interface CartRepository extends JpaRepository<Cart, Long> {

    Optional<Cart> findByUserId(Long userId);

    boolean existsByUserId(Long userId);

    List<Cart> findAllByUserIn(List<User> users);
}
