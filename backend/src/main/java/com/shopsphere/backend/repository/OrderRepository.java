package com.shopsphere.backend.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.shopsphere.backend.domain.Order;
import com.shopsphere.backend.domain.OrderStatus;

/**
 * Order repository (Phase 5). Ownership-scoped lookups power the customer
 * endpoints ("customers may access ONLY their own orders"); full-list and
 * status-filtered pages back the admin endpoints.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByOrderStatus(OrderStatus orderStatus, Pageable pageable);

    boolean existsByOrderNumber(String orderNumber);
}
