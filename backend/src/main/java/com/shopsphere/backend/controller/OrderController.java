package com.shopsphere.backend.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.shopsphere.backend.dto.response.OrderResponse;
import com.shopsphere.backend.dto.response.OrderTrackingResponse;
import com.shopsphere.backend.dto.response.PageResponse;
import com.shopsphere.backend.security.UserPrincipal;
import com.shopsphere.backend.service.OrderService;

/**
 * Customer order endpoints (Phase 5). Every route requires CUSTOMER
 * authentication; ownership (customers may access ONLY their own orders) is
 * enforced inside {@link OrderService} — a foreign order yields 404.
 */
@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    /** Paginated order history for the authenticated customer (newest first). */
    @GetMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<PageResponse<OrderResponse>> orderHistory(
            Principal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(orderService.getOrderHistory(currentUserId(principal), page, size));
    }

    /** Full order detail incl. immutable item snapshots. */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> getOrder(Principal principal, @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderForCustomer(id, currentUserId(principal)));
    }

    /** Lightweight lifecycle/tracking view incl. cancellability. */
    @GetMapping("/{id}/tracking")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderTrackingResponse> trackOrder(Principal principal, @PathVariable Long id) {
        return ResponseEntity.ok(orderService.trackOrder(id, currentUserId(principal)));
    }

    /** Customer cancellation; invalid state (shipped/delivered/...) yields 409. */
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<OrderResponse> cancelOrder(Principal principal, @PathVariable Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(id, currentUserId(principal)));
    }

    private Long currentUserId(Principal principal) {
        // Spring Security hands controllers the Authentication token, not the
        // UserDetails it wraps, so the principal has to be unwrapped first.
        // UserDetailsServiceImpl returns a UserPrincipal, which carries the id.
        if (principal instanceof UserPrincipal userPrincipal) {
            return userPrincipal.user().getId();
        }
        if (principal instanceof Authentication authentication
                && authentication.getPrincipal() instanceof UserPrincipal userPrincipal) {
            return userPrincipal.user().getId();
        }
        throw new AuthenticationCredentialsNotFoundException(
                "Authenticated principal is not a UserPrincipal");
    }
}
