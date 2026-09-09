package com.shopsphere.backend.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.shopsphere.backend.dto.request.CheckoutRequest;
import com.shopsphere.backend.dto.response.CheckoutResponse;
import com.shopsphere.backend.security.UserPrincipal;
import com.shopsphere.backend.service.CheckoutService;

/**
 * Checkout endpoint (Phase 5). Authenticated CUSTOMER only; the entire
 * checkout (stock reservation, order creation, payment charge, cart clearing)
 * runs inside {@link CheckoutService#checkout} in one transaction — no
 * business logic lives here.
 */
@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {

    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    /** Checks out the authenticated customer's cart; 201 with the created order + payment. */
    @PostMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CheckoutResponse> checkout(Principal principal,
                                                     @Valid @RequestBody CheckoutRequest request) {
        CheckoutResponse response = checkoutService.checkout(currentUserId(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    private Long currentUserId(Principal principal) {
        return ((UserPrincipal) principal).user().getId();
    }
}
