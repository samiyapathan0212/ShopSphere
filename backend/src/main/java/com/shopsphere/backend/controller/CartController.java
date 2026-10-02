package com.shopsphere.backend.controller;

import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.shopsphere.backend.dto.request.AddCartItemRequest;
import com.shopsphere.backend.dto.request.UpdateCartItemRequest;
import com.shopsphere.backend.dto.response.CartResponse;
import com.shopsphere.backend.security.UserPrincipal;
import com.shopsphere.backend.service.CartService;

/**
 * Cart endpoints (Phase 4A). All mutations require CUSTOMER authentication;
 * customers can only access their own cart. The current user is resolved from the
 * JWT principal.
 */
@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
    public ResponseEntity<CartResponse> getCart(Principal principal) {
        Long userId = userIdFrom(principal);
        return ResponseEntity.ok(cartService.getCart(userId));
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CartResponse> addItem(Principal principal,
                                                @RequestBody AddCartItemRequest request) {
        Long userId = userIdFrom(principal);
        return ResponseEntity.ok(cartService.addItem(userId, request));
    }

    @PutMapping("/items/{productId}")
    @PreAuthorize("hasRole('CUSTOMER')")
        public ResponseEntity<CartResponse> updateItemQuantity(Principal principal,
                                                           @PathVariable Long productId,
                                                           @Valid @RequestBody UpdateCartItemRequest request) {
        Long userId = userIdFrom(principal);
        return ResponseEntity.ok(cartService.updateItemQuantity(userId, productId, request));
    }

    @DeleteMapping("/items/{productId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CartResponse> removeItem(Principal principal,
                                                   @PathVariable Long productId) {
        Long userId = userIdFrom(principal);
        return ResponseEntity.ok(cartService.removeItem(userId, productId));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<CartResponse> clearCart(Principal principal) {
        Long userId = userIdFrom(principal);
        return ResponseEntity.ok(cartService.clearCart(userId));
    }

    private Long userIdFrom(Principal principal) {
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
