package com.shopsphere.backend.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.shopsphere.backend.domain.OrderItem;

/**
 * Order item repository (Phase 5). {@code findByOrderIdIn} batches item
 * loading for paginated order lists, avoiding an N+1 query per order.
 */
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByOrderIdIn(Collection<Long> orderIds);
}
