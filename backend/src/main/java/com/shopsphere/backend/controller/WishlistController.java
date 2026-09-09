package com.shopsphere.backend.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shopsphere.backend.dto.response.WishlistResponse;
import com.shopsphere.backend.service.WishlistService;

/**
 * Wishlist endpoints (Phase 4B). All mutations require CUSTOMER authentication;
 * customers can only access their own wishlist.
 */
@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<WishlistResponse> getWishlist(Principal principal) {
        Long userId = userIdFrom(principal);
        return ResponseEntity.ok(wishlistService.getWishlist(userId));
    }

    @PostMapping("/items/{productId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<WishlistResponse> addItem(Principal principal,
                                                                                                        @PathVariable Long productId) {
        Long userId = userIdFrom(principal);
        return ResponseEntity.ok(wishlistService.addItem(userId, productId));
    }

    @DeleteMapping("/items/{productId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<WishlistResponse> removeItem(Principal principal,
                                                                                                               @PathVariable Long productId) {
        Long userId = userIdFrom(principal);
        return ResponseEntity.ok(wishlistService.removeItem(userId, productId));
    }

    private Long userIdFrom(Principal principal) {
        return ((com.shopsphere.backend.security.UserPrincipal) principal).user().getId();
    }
}
