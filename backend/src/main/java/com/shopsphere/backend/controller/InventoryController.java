package com.shopsphere.backend.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.shopsphere.backend.dto.request.UpdateInventoryRequest;
import com.shopsphere.backend.dto.response.InventoryAvailabilityResponse;
import com.shopsphere.backend.dto.response.InventoryResponse;
import com.shopsphere.backend.mapper.InventoryMapper;
import com.shopsphere.backend.repository.InventoryRepository;
import com.shopsphere.backend.service.InventoryService;

/**
 * Inventory endpoints (Phase 5). The stock overview and updates are
 * ADMIN-only; the per-product read is customer-facing and therefore returns
 * {@link InventoryAvailabilityResponse}, which deliberately exposes only the
 * purchasable amount — never quantity, reservedQty or reorder thresholds.
 */
@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;

    public InventoryController(InventoryService inventoryService,
                               InventoryRepository inventoryRepository) {
        this.inventoryService = inventoryService;
        this.inventoryRepository = inventoryRepository;
    }

    /** Admin overview of every product's stock levels (full admin view). */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true) // lazy product mapping; open-in-view is disabled
    public ResponseEntity<List<InventoryResponse>> listInventory() {
        List<InventoryResponse> inventory = inventoryRepository.findAll().stream()
                .map(InventoryMapper::toResponse)
                .toList();
        return ResponseEntity.ok(inventory);
    }

    /** Availability for one product — the only customer-facing stock info. */
    @GetMapping("/{productId}")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<InventoryAvailabilityResponse> getAvailability(
            @PathVariable Long productId) {
        return ResponseEntity.ok(inventoryService.getAvailability(productId));
    }

    /** Admin stock update; invalid/negative or under-reserved values are rejected (409). */
    @PutMapping("/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<InventoryResponse> updateInventory(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateInventoryRequest request) {
        return ResponseEntity.ok(inventoryService.updateInventoryForProduct(productId, request));
    }
}
