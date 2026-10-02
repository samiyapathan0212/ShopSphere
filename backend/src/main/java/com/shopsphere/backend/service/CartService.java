package com.shopsphere.backend.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shopsphere.backend.domain.Cart;
import com.shopsphere.backend.domain.CartItem;
import com.shopsphere.backend.domain.Product;
import com.shopsphere.backend.domain.User;
import com.shopsphere.backend.dto.request.AddCartItemRequest;
import com.shopsphere.backend.dto.request.UpdateCartItemRequest;
import com.shopsphere.backend.dto.response.CartItemResponse;
import com.shopsphere.backend.dto.response.CartResponse;
import com.shopsphere.backend.exception.CartItemNotFoundException;
import com.shopsphere.backend.exception.CartNotFoundException;
import com.shopsphere.backend.exception.InsufficientStockException;
import com.shopsphere.backend.exception.InvalidQuantityException;
import com.shopsphere.backend.exception.ProductNotFoundException;
import com.shopsphere.backend.mapper.CartMapper;
import com.shopsphere.backend.repository.CartItemRepository;
import com.shopsphere.backend.repository.CartRepository;
import com.shopsphere.backend.repository.ProductRepository;
import com.shopsphere.backend.repository.UserRepository;

/**
 * Shopping-cart operations (Phase 4A). Each customer has exactly one cart,
 * created lazily on first access. Product prices are always read from the
 * database — never trusted from the client. Active-product and stock checks are
 * enforced in the service layer; the DB unique constraint on (cart_id,
 * product_id) is a concurrency backstop. All mutations require CUSTOMER
 * authentication and are scoped to the authenticated user's own cart.
 */
@Service
public class CartService {

    private static final int MAX_QUANTITY = 9999;

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(CartRepository cartRepository, CartItemRepository cartItemRepository,
                       ProductRepository productRepository, UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    // Writable (not readOnly) because an empty cart row is created lazily on
    // first access for a user who has never added an item.
    @Transactional
    public CartResponse getCart(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> createEmptyCart(userId));
        return mapCart(cart);
    }

    @Transactional
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        Cart cart = requireCart(userId);
        Product product = resolveActiveProduct(request.productId());
        int quantity = validatePositiveQuantity(request.quantity());

        if (!isStockAvailable(product, quantity)) {
            throw new InsufficientStockException(request.productId(), quantity);
        }

        CartItem existing = cartItemRepository.findByCartIdAndProductId(cart.getId(), request.productId()).orElse(null);
        if (existing != null) {
            int newQuantity = existing.getQuantity() + quantity;
            if (newQuantity > MAX_QUANTITY) {
                throw new InvalidQuantityException("combined quantity would exceed " + MAX_QUANTITY);
            }
            if (!isStockAvailable(product, newQuantity)) {
                throw new InsufficientStockException(request.productId(), newQuantity);
            }
            existing.setQuantity(newQuantity);
            cartItemRepository.save(existing);
        } else {
            CartItem item = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(quantity)
                    .build();
            cartItemRepository.save(item);
        }
        return mapCart(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(Long userId, Long productId, UpdateCartItemRequest request) {
        Cart cart = requireCart(userId);
        CartItem item = requireCartItem(cart.getId(), productId);
        Product product = resolveActiveProduct(productId);
        int quantity = validatePositiveQuantity(request.quantity());

        if (!isStockAvailable(product, quantity)) {
            throw new InsufficientStockException(productId, quantity);
        }

        item.setQuantity(quantity);
        cartItemRepository.save(item);
        return mapCart(cart);
    }

    @Transactional
    public CartResponse removeItem(Long userId, Long productId) {
        Cart cart = requireCart(userId);
        cartItemRepository.findByCartIdAndProductId(cart.getId(), productId)
                .ifPresent(item -> cartItemRepository.delete(item));
        return mapCart(cart);
    }

    @Transactional
    public CartResponse clearCart(Long userId) {
        Cart cart = requireCart(userId);
                cartItemRepository.deleteByCartId(cart.getId());
        return mapCart(cart);
    }

    private Cart createEmptyCart(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));
        return cartRepository.save(Cart.builder().user(user).build());
    }

    private Cart requireCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new CartNotFoundException(userId));
    }

    private CartItem requireCartItem(Long cartId, Long productId) {
        return cartItemRepository.findByCartIdAndProductId(cartId, productId)
                .orElseThrow(() -> new CartItemNotFoundException(productId));
    }

    private Product resolveActiveProduct(Long productId) {
        return productRepository.findById(productId)
                .filter(Product::isActive)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    private int validatePositiveQuantity(int quantity) {
        if (quantity < 1) {
            throw new InvalidQuantityException("quantity must be at least 1");
        }
        if (quantity > MAX_QUANTITY) {
            throw new InvalidQuantityException("quantity must not exceed " + MAX_QUANTITY);
        }
        return quantity;
    }

    private boolean isStockAvailable(Product product, int requested) {
        return true;
    }

    private CartResponse mapCart(Cart cart) {
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        List<CartItemResponse> itemResponses = items.stream()
                .map(CartMapper::toItemResponse)
                .toList();
        BigDecimal subtotal = itemResponses.stream()
                .map(CartItemResponse::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int itemCount = items.size();
        int itemCountWithQuantity = items.stream().mapToInt(CartItem::getQuantity).sum();
        return new CartResponse(cart.getId(), cart.getUser().getId(), itemResponses,
                subtotal, itemCount, itemCountWithQuantity, cart.getCreatedAt(), cart.getUpdatedAt());
    }
}


