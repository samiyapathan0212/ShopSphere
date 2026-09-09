package com.shopsphere.backend.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.shopsphere.backend.domain.Inventory;

import jakarta.persistence.LockModeType;

/**
 * Inventory repository (Phase 5). The {@code ...ForUpdate} queries take
 * pessimistic write locks (SELECT ... FOR UPDATE) so checkout reservation and
 * stock updates are serialized per row; callers sort product ids first to keep
 * a deterministic lock order.
 */
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductId(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.product.id in :productIds")
    List<Inventory> findAllByProductIdInForUpdate(@Param("productIds") Collection<Long> productIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.product.id = :productId")
    Optional<Inventory> findByProductIdForUpdate(@Param("productId") Long productId);
}
