package com.shopsphere.backend.controller;

import java.util.Locale;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.shopsphere.backend.domain.OrderStatus;
import com.shopsphere.backend.dto.request.UpdateOrderStatusRequest;
import com.shopsphere.backend.dto.response.OrderResponse;
import com.shopsphere.backend.dto.response.PageResponse;
import com.shopsphere.backend.exception.InvalidQueryParameterException;
import com.shopsphere.backend.service.OrderService;

/**
 * Admin order endpoints (Phase 5). ADMIN only: unrestricted listing/details
 * and lifecycle status updates validated against the order state machine
 * (invalid transitions yield 409 from the service).
 */
@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** All orders, newest first, optionally filtered by {@code status}. */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<OrderResponse>> listOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {
        return ResponseEntity.ok(orderService.listAllOrders(page, size, parseStatus(status)));
    }

    /** Any order's detail incl. item snapshots. */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderForAdmin(id));
    }

    /** Admin status update; invalid lifecycle transitions yield 409. */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request.orderStatus()));
    }

    /** Lenient parse so an unknown status is a 400, not a 500. */
    private OrderStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return OrderStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new InvalidQueryParameterException("Unknown order status: " + status);
        }
    }
}
