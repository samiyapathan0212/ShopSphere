package com.shopsphere.backend.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.shopsphere.backend.exception.InsufficientStockException;
import com.shopsphere.backend.exception.InvalidQuantityException;

/**
 * Stock levels for a product (Phase 5). One row per product (unique FK).
 * Available stock = {@code quantity - reservedQty}; reservation moves stock
 * aside without selling it, and the checkout transaction either converts the
 * reservation into a sale (payment approved) or releases it (payment failed).
 * Every mutation guards against negative {@code quantity}/{@code reservedQty}.
 */
@Entity
@Table(name = "inventories")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "reserved_qty", nullable = false)
    private int reservedQty;

    @Column(name = "reorder_threshold", nullable = false)
    private int reorderThreshold;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Inventory() {
        // Required by JPA.
    }

    private Inventory(Long id, Product product, int quantity, int reservedQty, int reorderThreshold) {
        this.id = id;
        this.product = product;
        this.quantity = quantity;
        this.reservedQty = reservedQty;
        this.reorderThreshold = reorderThreshold;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Long getId() {
        return id;
    }

    public Product getProduct() {
        return product;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getReservedQty() {
        return reservedQty;
    }

    public int getReorderThreshold() {
        return reorderThreshold;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public int getAvailableQty() {
        return quantity - reservedQty;
    }

    /** @return whether available stock has fallen to the reorder threshold. */
    public boolean isLowStock() {
        return getAvailableQty() <= reorderThreshold;
    }

    /** Moves units aside: available stock must cover the request. */
    public void reserve(int qty) {
        requirePositive(qty);
        if (getAvailableQty() < qty) {
            throw new InsufficientStockException(product.getId(), qty);
        }
        reservedQty += qty;
    }

    /** Gives reserved units back without selling them. */
    public void releaseReservation(int qty) {
        requirePositive(qty);
        if (reservedQty < qty) {
            throw new InvalidQuantityException(
                    "cannot release " + qty + " units: only " + reservedQty + " reserved");
        }
        reservedQty -= qty;
    }

    /** Converts a reservation into a sale: both counters drop by qty. */
    public void convertReservationToSale(int qty) {
        requirePositive(qty);
        if (reservedQty < qty) {
            throw new InvalidQuantityException(
                    "cannot sell " + qty + " units: only " + reservedQty + " reserved");
        }
        quantity -= qty;
        reservedQty -= qty;
    }

    /** Returns sold units to stock (order cancellation after payment). */
    public void restock(int qty) {
        requirePositive(qty);
        quantity += qty;
    }

    /**
     * Applies an admin update. Quantity may never drop below what is already
     * reserved, and neither field may be negative.
     */
    public void update(int newQuantity, int newReorderThreshold) {
        if (newQuantity < 0 || newReorderThreshold < 0) {
            throw new InvalidQuantityException("quantity and reorderThreshold must not be negative");
        }
        if (newQuantity < reservedQty) {
            throw new InvalidQuantityException(
                    "quantity (" + newQuantity + ") cannot be below reservedQty (" + reservedQty + ")");
        }
        this.quantity = newQuantity;
        this.reorderThreshold = newReorderThreshold;
    }

    private void requirePositive(int qty) {
        if (qty <= 0) {
            throw new InvalidQuantityException("quantity must be positive");
        }
    }

    public static final class Builder {
        private Long id;
        private Product product;
        private int quantity;
        private int reservedQty;
        private int reorderThreshold;

        private Builder() {
        }

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder product(Product product) {
            this.product = product;
            return this;
        }

        public Builder quantity(int quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder reservedQty(int reservedQty) {
            this.reservedQty = reservedQty;
            return this;
        }

        public Builder reorderThreshold(int reorderThreshold) {
            this.reorderThreshold = reorderThreshold;
            return this;
        }

        public Inventory build() {
            return new Inventory(id, product, quantity, reservedQty, reorderThreshold);
        }
    }
}
