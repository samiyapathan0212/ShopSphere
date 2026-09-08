package com.shopsphere.backend.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Bean-validation tests for the Phase 3A catalog request DTOs: required
 * fields, positive prices, SKU/name/description size limits and the decimal
 * scale cap. Uniqueness (SKU, category name) is database-level and covered by
 * {@link com.shopsphere.backend.repository.CatalogPersistenceTest}.
 */
class CatalogRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    private static Set<String> messages(Object request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getMessage())
                .collect(Collectors.toSet());
    }

    // --- CategoryRequest ---

    @Test
    void validCategoryHasNoViolations() {
        assertThat(messages(new CategoryRequest("Electronics", "Gadgets and devices", true))).isEmpty();
        // Optional fields may be omitted entirely.
        assertThat(messages(new CategoryRequest("Electronics", null, null))).isEmpty();
    }

    @Test
    void categoryRequiresAName() {
        assertThat(messages(new CategoryRequest(null, null, null))).contains("name is required");
        assertThat(messages(new CategoryRequest("   ", null, null))).contains("name is required");
    }

    @Test
    void oversizedCategoryFieldsAreRejected() {
        assertThat(messages(new CategoryRequest("x".repeat(101), null, null)))
                .contains("name must not exceed 100 characters");
        assertThat(messages(new CategoryRequest("Ok", "y".repeat(1001), null)))
                .contains("description must not exceed 1000 characters");
    }

    @Test
    void categorySizeBoundsAreAccepted() {
        assertThat(messages(new CategoryRequest("x".repeat(100), "y".repeat(1000), false))).isEmpty();
    }

    // --- ProductRequest ---

    private ProductRequest validProduct() {
        return new ProductRequest("SKU-001", "Desk Lamp", "Warm LED light",
                new BigDecimal("24.99"), 7L, true);
    }

    @Test
    void validProductHasNoViolations() {
        assertThat(messages(validProduct())).isEmpty();
    }

    @Test
    void requiredProductFieldsAreEnforced() {
        Set<String> messages = messages(new ProductRequest("", "  ", null, null, null, null));

        assertThat(messages).contains(
                "sku is required",
                "name is required",
                "price is required",
                "category is required");
    }

    @Test
    void priceMustBePositive() {
        assertThat(messages(new ProductRequest("S", "N", null, BigDecimal.ZERO, 1L, null)))
                .contains("price must be positive");
        assertThat(messages(new ProductRequest("S", "N", null, new BigDecimal("-0.01"), 1L, null)))
                .contains("price must be positive");
    }

    @Test
    void priceSupportsAtMostTwoDecimals() {
        assertThat(messages(new ProductRequest("S", "N", null, new BigDecimal("9.999"), 1L, null)))
                .contains("price supports at most 10 integer digits and 2 decimals");
    }

    @Test
    void oversizedProductFieldsAreRejected() {
        assertThat(messages(new ProductRequest("x".repeat(65), "N", null, BigDecimal.ONE, 1L, null)))
                .contains("sku must not exceed 64 characters");
        assertThat(messages(new ProductRequest("S", "x".repeat(151), null, BigDecimal.ONE, 1L, null)))
                .contains("name must not exceed 150 characters");
        assertThat(messages(new ProductRequest("S", "N", "z".repeat(2001), BigDecimal.ONE, 1L, null)))
                .contains("description must not exceed 2000 characters");
    }

    @Test
    void productSizeBoundsAreAccepted() {
        assertThat(messages(new ProductRequest("x".repeat(64), "x".repeat(150), "d".repeat(2000),
                new BigDecimal("12345678.90"), 1L, false))).isEmpty();
    }
}