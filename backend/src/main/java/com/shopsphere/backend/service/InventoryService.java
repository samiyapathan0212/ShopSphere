package com.shopsphere.backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopsphere.backend.domain.Inventory;
import com.shopsphere.backend.domain.Product;
import com.shopsphere.backend.dto.request.UpdateInventoryRequest;
import com.shopsphere.backend.dto.response.InventoryAvailabilityResponse;
import com.shopsphere.backend.dto.response.InventoryResponse;
import com.shopsphere.backend.exception.InventoryNotFoundException;
import com.shopsphere.backend.exception.ProductNotFoundException;
import com.shopsphere.backend.mapper.InventoryMapper;
import com.shopsphere.backend.repository.InventoryRepository;
import com.shopsphere.backend.repository.ProductRepository;

/**
 * Inventory operations (Phase 5). All stock mutations go through the entity's
 * own guards ({@code reserve}/{@code releaseReservation}/{@code
 * convertReservationToSale}/{@code restock}/{@code update}), which reject
 * insufficient or invalid quantities. Batch operations take pessimistic write
 * locks via the {@code InventoryRepository...ForUpdate} queries and iterate
 * product ids in ascending order so concurrent checkouts acquire rows in a
 * deterministic order (deadlock prevention). Methods join the caller's
 * transaction ({@code REQUIRED}), so a checkout failure rolls every stock
 * change back.
 */
@Service
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public InventoryService(InventoryRepository inventoryRepository,
                            ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    /** Customer-safe availability for a product (creates the row if missing). */
    @Transactional
    public InventoryAvailabilityResponse getAvailability(Long productId) {
        return InventoryMapper.toAvailabilityResponse(loadOrCreate(productId, false));
    }

    /** Admin read by product. */
    @Transactional(readOnly = true)
    public InventoryResponse getInventoryByProduct(Long productId) {
        return InventoryMapper.toResponse(requireInventoryByProduct(productId));
    }

    /**
     * Returns the inventory row for a product, creating a zero-stock row when
     * the product exists but has none yet (products start without stock).
     */
    @Transactional
    public InventoryResponse getOrCreateInventoryForProduct(Long productId) {
        return InventoryMapper.toResponse(loadOrCreate(productId, false));
    }

    /** Admin stock update; the entity rejects negative or under-reserved values. */
    @Transactional
    public InventoryResponse updateInventoryForProduct(Long productId, UpdateInventoryRequest request) {
        Inventory inventory = loadOrCreate(productId, true);
        inventory.update(request.quantity(), request.reorderThreshold());
        return InventoryMapper.toResponse(inventory);
    }

    /**
     * Reserves stock for several products atomically. Rows are locked in
     * ascending product-id order; throws {@code InsufficientStockException}
     * when any product cannot cover its demand (which also rolls back the
     * reservations made for earlier products in the same transaction).
     */
    @Transactional
    public void reserveStockForProducts(Map<Long, Integer> quantitiesByProduct) {
        applyToEachProduct(quantitiesByProduct, Inventory::reserve);
    }

    /** Gives reserved units back without selling them (checkout decline). */
    @Transactional
    public void releaseReservationForProducts(Map<Long, Integer> quantitiesByProduct) {
        applyToEachProduct(quantitiesByProduct, Inventory::releaseReservation);
    }

    /** Converts reservations into sales (payment approved). */
    @Transactional
    public void convertReservationToSaleForProducts(Map<Long, Integer> quantitiesByProduct) {
        applyToEachProduct(quantitiesByProduct, Inventory::convertReservationToSale);
    }

    /** Returns sold units to stock (order cancellation after payment). */
    @Transactional
    public void restockForProducts(Map<Long, Integer> quantitiesByProduct) {
        applyToEachProduct(quantitiesByProduct, Inventory::restock);
    }

    private Inventory requireInventoryByProduct(Long productId) {
        return inventoryRepository.findByProductId(productId)
                .orElseThrow(() -> new InventoryNotFoundException(productId));
    }

    private Inventory loadOrCreate(Long productId, boolean forUpdate) {
        Optional<Inventory> existing = forUpdate
                ? inventoryRepository.findByProductIdForUpdate(productId)
                : inventoryRepository.findByProductId(productId);
        return existing.orElseGet(() -> createForProduct(productId));
    }

    private Inventory createForProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
        return inventoryRepository.save(Inventory.builder()
                .product(product)
                .quantity(0)
                .reservedQty(0)
                .reorderThreshold(0)
                .build());
    }

    /**
     * Applies a stock mutation to every product in the map, locking all rows
     * up front (SELECT ... FOR UPDATE) in ascending product-id order so
     * concurrent checkouts serialize in the same deterministic order.
     */
    private void applyToEachProduct(Map<Long, Integer> quantitiesByProduct,
                                    BiConsumer<Inventory, Integer> action) {
        List<Long> sortedProductIds = quantitiesByProduct.keySet().stream().sorted().toList();
        if (sortedProductIds.isEmpty()) {
            return;
        }
        Map<Long, Inventory> lockedByProductId = new HashMap<>();
        for (Inventory inventory : inventoryRepository.findAllByProductIdInForUpdate(sortedProductIds)) {
            lockedByProductId.put(inventory.getProduct().getId(), inventory);
        }
        for (Long productId : sortedProductIds) {
            Inventory inventory = lockedByProductId.get(productId);
            if (inventory == null) {
                inventory = createForProduct(productId);
            }
            action.accept(inventory, quantitiesByProduct.get(productId));
        }
    }
}
